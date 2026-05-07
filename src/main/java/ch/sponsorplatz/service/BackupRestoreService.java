package ch.sponsorplatz.service;

import ch.sponsorplatz.model.AuditAktion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.Statement;

/**
 * Restore eines DB-Dumps — DESTRUKTIVE Operation.
 *
 * <p>H2 (dev): {@code RUNSCRIPT FROM '<file>'} — H2 droppt nichts automatisch,
 * deshalb funktioniert Restore nur, wenn die Ziel-DB leer ist oder der Dump
 * passende {@code DROP IF EXISTS}-Statements enthält. Tests + lokale Dev nutzen
 * das primär zum Bootstrappen aus einem Production-Snapshot.
 *
 * <p>PostgreSQL (prod): {@code psql --file=<file>} via {@link ProcessBuilder}.
 * pg_dump wird seit V15.1 mit {@code --clean --if-exists} aufgerufen — der
 * Dump enthält DROP-Statements, ein Restore in die laufende DB ist sicher.
 *
 * <p>Aufrufer-Verantwortung: User explizit zur Bestätigung "RESTORE" zwingen
 * (Controller-Layer).
 */
@Service
public class BackupRestoreService {

    private static final Logger log = LoggerFactory.getLogger(BackupRestoreService.class);

    private final DataSource dataSource;
    private final AuditService auditService;

    @Value("${spring.datasource.url:}")
    private String datasourceUrl;

    @Value("${spring.datasource.username:}")
    private String datasourceUser;

    public BackupRestoreService(DataSource dataSource, AuditService auditService) {
        this.dataSource = dataSource;
        this.auditService = auditService;
    }

    /**
     * Spielt den übergebenen SQL-Dump in die aktuelle Datenbank zurück.
     *
     * @param sqlBytes UTF-8-kodierter SQL-Dump (pg_dump-Output oder H2 SCRIPT)
     * @param ausgefuehrtVon Login-Name für Audit
     * @throws IOException bei IO-Fehlern beim Temp-File-Handling
     * @throws RuntimeException bei psql/Statement-Fehlern (Inhalt im Message)
     */
    public void restore(byte[] sqlBytes, String ausgefuehrtVon) throws IOException {
        if (sqlBytes == null || sqlBytes.length == 0) {
            throw new IllegalArgumentException("Restore-Datei ist leer");
        }

        Path tmp = Files.createTempFile("sponsorplatz_restore_", ".sql");
        try {
            Files.write(tmp, sqlBytes);
            if (istH2()) {
                fuehreH2Restore(tmp);
            } else {
                fuehrePostgresRestore(tmp);
            }
            auditService.protokolliere(AuditAktion.BACKUP_ERSTELLT, ausgefuehrtVon, null,
                    "RESTORE", "Bytes=" + sqlBytes.length);
            log.warn("RESTORE durchgeführt von {} — {} Bytes wiederhergestellt", ausgefuehrtVon, sqlBytes.length);
        } finally {
            try { Files.deleteIfExists(tmp); }
            catch (IOException e) { log.warn("Temp-Restore-Datei konnte nicht gelöscht werden: {}", tmp, e); }
        }
    }

    private boolean istH2() {
        return datasourceUrl != null && datasourceUrl.contains("h2");
    }

    private void fuehreH2Restore(Path sqlDatei) {
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute("RUNSCRIPT FROM '" + sqlDatei.toAbsolutePath() + "'");
        } catch (Exception e) {
            throw new RuntimeException("H2-Restore fehlgeschlagen: " + e.getMessage(), e);
        }
    }

    private void fuehrePostgresRestore(Path sqlDatei) {
        try {
            ProcessBuilder pb = new ProcessBuilder(
                    "psql",
                    "--single-transaction",
                    "--set", "ON_ERROR_STOP=on",
                    "--file=" + sqlDatei.toAbsolutePath(),
                    extractDbNameFromUrl()
            );
            pb.environment().put("PGPASSWORD", System.getenv("DB_PASSWORD"));
            pb.environment().put("PGUSER", datasourceUser);

            String dbHost = extractHostFromUrl();
            if (dbHost != null) {
                pb.environment().put("PGHOST", dbHost);
            }

            pb.redirectErrorStream(true);
            Process process = pb.start();
            String output = new String(process.getInputStream().readAllBytes());
            int exitCode = process.waitFor();

            if (exitCode != 0) {
                throw new RuntimeException("psql Exit-Code " + exitCode + ":\n" + output);
            }
            log.info("Postgres-Restore output:\n{}", output);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("psql unterbrochen", e);
        } catch (IOException e) {
            throw new RuntimeException("psql fehlgeschlagen: " + e.getMessage(), e);
        }
    }

    private String extractDbNameFromUrl() {
        if (datasourceUrl == null) return "sponsorplatz";
        int lastSlash = datasourceUrl.lastIndexOf('/');
        if (lastSlash < 0) return "sponsorplatz";
        String rest = datasourceUrl.substring(lastSlash + 1);
        int qMark = rest.indexOf('?');
        return qMark > 0 ? rest.substring(0, qMark) : rest;
    }

    private String extractHostFromUrl() {
        if (datasourceUrl == null || !datasourceUrl.contains("://")) return null;
        String afterProtocol = datasourceUrl.substring(datasourceUrl.indexOf("://") + 3);
        int colon = afterProtocol.indexOf(':');
        int slash = afterProtocol.indexOf('/');
        if (colon > 0) return afterProtocol.substring(0, colon);
        if (slash > 0) return afterProtocol.substring(0, slash);
        return afterProtocol;
    }
}
