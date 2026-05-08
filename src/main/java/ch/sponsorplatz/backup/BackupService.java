package ch.sponsorplatz.backup;
import ch.sponsorplatz.audit.AuditService;

import ch.sponsorplatz.audit.AuditAktion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

/**
 * Backup-Service für die Datenbank.
 *
 * <ul>
 *   <li><b>H2 (dev):</b> SCRIPT TO — erzeugt SQL-Dump in backup-Verzeichnis</li>
 *   <li><b>PostgreSQL (prod):</b> pg_dump via ProcessBuilder (Pfad konfigurierbar)</li>
 * </ul>
 *
 * Konfiguration:
 * <pre>
 * sponsorplatz.backup.verzeichnis=./backups    (lokal)
 * sponsorplatz.backup.cron=0 0 2 * * *         (täglich um 02:00)
 * sponsorplatz.backup.aufbewahrung-tage=30
 * </pre>
 */
@Service
public class BackupService {

    private static final Logger log = LoggerFactory.getLogger(BackupService.class);
    private static final DateTimeFormatter TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");

    private final DataSource dataSource;
    private final AuditService auditService;
    private final Optional<BackupCloudUploader> cloudUploader;

    @Value("${sponsorplatz.backup.verzeichnis:./backups}")
    private String backupVerzeichnis;

    @Value("${sponsorplatz.backup.aufbewahrung-tage:30}")
    private int aufbewahrungTage;

    @Value("${spring.datasource.url:}")
    private String datasourceUrl;

    @Value("${spring.datasource.username:}")
    private String datasourceUser;

    public BackupService(DataSource dataSource, AuditService auditService,
                         Optional<BackupCloudUploader> cloudUploader) {
        this.dataSource = dataSource;
        this.auditService = auditService;
        this.cloudUploader = cloudUploader;
    }

    /**
     * Erstellt ein Backup manuell (z.B. via Admin-UI).
     *
     * @return Pfad der erstellten Backup-Datei
     * @throws IOException bei Dateisystem-Fehler
     */
    public Path erstelleBackup() throws IOException {
        Path verzeichnis = Paths.get(backupVerzeichnis);
        Files.createDirectories(verzeichnis);

        String timestamp = LocalDateTime.now().format(TIMESTAMP_FORMAT);
        String dateiname = "sponsorplatz_backup_" + timestamp + ".sql";
        Path backupPfad = verzeichnis.resolve(dateiname);

        if (istH2()) {
            erstelleH2Backup(backupPfad);
        } else {
            erstellePostgresBackup(backupPfad);
        }

        auditService.protokolliere(AuditAktion.BACKUP_ERSTELLT, "SYSTEM", null, "BACKUP", backupPfad.toString());
        bereinigAlteBackups(verzeichnis);

        ladeInCloudHoch(backupPfad);

        log.info("Backup erstellt: {}", backupPfad);
        return backupPfad;
    }

    /**
     * Lädt das Backup zusätzlich in einen Cloud-Bucket, sofern ein
     * {@link BackupCloudUploader} im Context registriert ist. Fehler werden
     * geloggt, aber nicht eskaliert — das lokale Backup bleibt gültig.
     */
    private void ladeInCloudHoch(Path backupPfad) {
        if (cloudUploader.isEmpty()) {
            return;
        }
        try {
            String key = cloudUploader.get().lade(backupPfad);
            log.info("Backup in Cloud gesichert: {}", key);
        } catch (RuntimeException e) {
            log.warn("Cloud-Backup-Upload fehlgeschlagen — lokales Backup bleibt: {}", e.getMessage());
        }
    }

    /**
     * Geplantes automatisches Backup (Default: täglich 02:00).
     */
    @Scheduled(cron = "${sponsorplatz.backup.cron:0 0 2 * * *}")
    public void geplanterBackup() {
        try {
            erstelleBackup();
        } catch (Exception e) {
            log.error("Geplanter Backup fehlgeschlagen: {}", e.getMessage(), e);
        }
    }

    /**
     * Liest eine Backup-Datei als Bytes — für Download via Admin-UI.
     *
     * @throws IllegalArgumentException wenn die Datei nicht existiert oder
     *                                  der Name ungültig ist (Path-Traversal)
     */
    public byte[] leseBackup(String dateiname) throws IOException {
        Path pfad = pruefeUndAufloeseBackup(dateiname);
        return Files.readAllBytes(pfad);
    }

    /**
     * Löscht eine Backup-Datei.
     *
     * @throws IllegalArgumentException bei ungültigem Namen / fehlender Datei
     */
    public void loescheBackup(String dateiname) throws IOException {
        Path pfad = pruefeUndAufloeseBackup(dateiname);
        Files.delete(pfad);
        log.info("Backup gelöscht: {}", dateiname);
    }

    /**
     * Validiert den Dateinamen (kein Path-Traversal, muss mit Backup-Prefix
     * starten) und löst ihn relativ zum Backup-Verzeichnis auf.
     */
    private Path pruefeUndAufloeseBackup(String dateiname) throws IOException {
        if (dateiname == null
                || !dateiname.matches("sponsorplatz_backup_[\\w-]+\\.sql")) {
            throw new IllegalArgumentException("Ungültiger Backup-Dateiname: " + dateiname);
        }
        Path verzeichnis = Paths.get(backupVerzeichnis).toAbsolutePath().normalize();
        Path pfad = verzeichnis.resolve(dateiname).normalize();
        if (!pfad.startsWith(verzeichnis) || !Files.exists(pfad)) {
            throw new IllegalArgumentException("Backup nicht gefunden: " + dateiname);
        }
        return pfad;
    }

    /**
     * Gibt alle vorhandenen Backup-Dateien zurück (neueste zuerst).
     */
    public java.util.List<Path> listeBackups() throws IOException {
        Path verzeichnis = Paths.get(backupVerzeichnis);
        if (!Files.exists(verzeichnis)) {
            return java.util.Collections.emptyList();
        }
        try (var stream = Files.list(verzeichnis)) {
            return stream
                    .filter(p -> p.getFileName().toString().startsWith("sponsorplatz_backup_"))
                    .sorted(java.util.Comparator.reverseOrder())
                    .toList();
        }
    }

    private boolean istH2() {
        return datasourceUrl != null && datasourceUrl.contains("h2");
    }

    private void erstelleH2Backup(Path backupPfad) {
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute("SCRIPT TO '" + backupPfad.toAbsolutePath() + "'");
        } catch (Exception e) {
            throw new RuntimeException("H2-Backup fehlgeschlagen: " + e.getMessage(), e);
        }
    }

    private void erstellePostgresBackup(Path backupPfad) {
        try {
            // pg_dump Aufruf — Credentials kommen aus Umgebung (PGPASSWORD etc.)
            // --clean + --if-exists ermöglichen Restore in eine bestehende DB:
            // psql DROPped Tabellen vor CREATE statt mit "already exists" zu failen.
            ProcessBuilder pb = new ProcessBuilder(
                    "pg_dump",
                    "--format=plain",
                    "--file=" + backupPfad.toAbsolutePath(),
                    "--clean",
                    "--if-exists",
                    "--no-owner",
                    "--no-privileges",
                    extractDbNameFromUrl()
            );
            pb.environment().put("PGPASSWORD", System.getenv("DB_PASSWORD"));
            // PGUSER explizit aus spring.datasource.username — sonst fällt
            // pg_dump auf den OS-User des Prozesses zurück (im Container 'sponsor',
            // was nicht der Postgres-User ist).
            if (datasourceUser != null && !datasourceUser.isBlank()) {
                pb.environment().put("PGUSER", datasourceUser);
            }

            String dbHost = extractHostFromUrl();
            if (dbHost != null) {
                pb.environment().put("PGHOST", dbHost);
            }

            pb.redirectErrorStream(true);
            Process process = pb.start();
            int exitCode = process.waitFor();

            if (exitCode != 0) {
                String output = new String(process.getInputStream().readAllBytes());
                throw new RuntimeException("pg_dump Exit-Code " + exitCode + ": " + output);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("pg_dump unterbrochen", e);
        } catch (IOException e) {
            throw new RuntimeException("pg_dump fehlgeschlagen: " + e.getMessage(), e);
        }
    }

    private void bereinigAlteBackups(Path verzeichnis) {
        try (var stream = Files.list(verzeichnis)) {
            var grenze = java.time.Instant.now().minus(java.time.Duration.ofDays(aufbewahrungTage));
            stream.filter(p -> p.getFileName().toString().startsWith("sponsorplatz_backup_"))
                    .filter(p -> {
                        try { return Files.getLastModifiedTime(p).toInstant().isBefore(grenze); }
                        catch (IOException e) { return false; }
                    })
                    .forEach(p -> {
                        try {
                            Files.delete(p);
                            log.info("Altes Backup gelöscht: {}", p.getFileName());
                        } catch (IOException e) {
                            log.warn("Konnte Backup nicht löschen: {}", p, e);
                        }
                    });
        } catch (IOException e) {
            log.warn("Backup-Bereinigung fehlgeschlagen: {}", e.getMessage());
        }
    }

    private String extractDbNameFromUrl() {
        // jdbc:postgresql://host:port/dbname → dbname
        if (datasourceUrl == null) return "sponsorplatz";
        int lastSlash = datasourceUrl.lastIndexOf('/');
        if (lastSlash < 0) return "sponsorplatz";
        String rest = datasourceUrl.substring(lastSlash + 1);
        int qMark = rest.indexOf('?');
        return qMark > 0 ? rest.substring(0, qMark) : rest;
    }

    private String extractHostFromUrl() {
        // jdbc:postgresql://host:port/dbname → host
        if (datasourceUrl == null || !datasourceUrl.contains("://")) return null;
        String afterProtocol = datasourceUrl.substring(datasourceUrl.indexOf("://") + 3);
        int colon = afterProtocol.indexOf(':');
        int slash = afterProtocol.indexOf('/');
        if (colon > 0) return afterProtocol.substring(0, colon);
        if (slash > 0) return afterProtocol.substring(0, slash);
        return afterProtocol;
    }
}

