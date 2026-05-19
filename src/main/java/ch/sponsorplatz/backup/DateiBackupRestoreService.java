package ch.sponsorplatz.backup;

import ch.sponsorplatz.audit.AuditAktion;
import ch.sponsorplatz.audit.AuditService;
import ch.sponsorplatz.shared.storage.StorageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URLConnection;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Restoret ein ZIP-Backup (aus {@link DateiBackupService}) zurück in den
 * konfigurierten {@link StorageService}.
 *
 * <p>Pro ZIP-Entry wird der Entry-Name als Storage-Key übernommen und der
 * Inhalt via {@link StorageService#speichereBytes} hochgeladen. Vorhandene
 * Objekte werden überschrieben — bewusst destruktiv, weil das Restore-
 * Szenario typischerweise eine ganze Zone wiederherstellt.
 *
 * <p>Content-Type wird heuristisch aus dem Dateinamen abgeleitet
 * ({@link URLConnection#guessContentTypeFromName}), Fallback ist
 * {@code application/octet-stream}. Damit ist das ZIP auch ohne Manifest
 * cross-cloud transportabel.
 */
@Service
public class DateiBackupRestoreService {

    private static final Logger log = LoggerFactory.getLogger(DateiBackupRestoreService.class);
    private static final String DEFAULT_CONTENT_TYPE = "application/octet-stream";

    private final StorageService storageService;
    private final AuditService auditService;

    public DateiBackupRestoreService(StorageService storageService, AuditService auditService) {
        this.storageService = storageService;
        this.auditService = auditService;
    }

    /**
     * Restoret alle Einträge aus dem ZIP-Archiv.
     *
     * @param zipBytes ZIP-Datei-Inhalt (aus dem Admin-UI-Upload)
     * @param ausgefuehrtVon Audit-User (z.B. {@code admin@example.ch})
     * @return Anzahl erfolgreich wiederhergestellter Dateien
     */
    public RestoreReport restore(byte[] zipBytes, String ausgefuehrtVon) throws IOException {
        if (zipBytes == null || zipBytes.length == 0) {
            throw new IllegalArgumentException("Restore-Input darf nicht leer sein");
        }

        int restored = 0;
        int skipped = 0;

        try (ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(zipBytes))) {
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                if (entry.isDirectory()) {
                    zip.closeEntry();
                    continue;
                }
                String pfad = entry.getName();
                if (pfad == null || pfad.isBlank() || pfad.contains("..")) {
                    log.warn("Datei-Restore-Skip — verdächtiger Entry-Name: '{}'", pfad);
                    skipped++;
                    zip.closeEntry();
                    continue;
                }
                byte[] inhalt = zip.readAllBytes();
                String contentType = abgeleiteterContentType(pfad);
                storageService.speichereBytes(inhalt, contentType, pfad);
                restored++;
                zip.closeEntry();
            }
        }

        String detail = "restored=" + restored + ", skipped=" + skipped;
        auditService.protokolliere(AuditAktion.DATEI_BACKUP_RESTORED, ausgefuehrtVon, null,
                "DATEI_BACKUP", detail);
        log.info("Datei-Restore durch {}: {} Dateien wiederhergestellt, {} skipped",
                ausgefuehrtVon, restored, skipped);
        return new RestoreReport(restored, skipped);
    }

    private static String abgeleiteterContentType(String pfad) {
        String guess = URLConnection.guessContentTypeFromName(pfad);
        return guess != null ? guess : DEFAULT_CONTENT_TYPE;
    }

    /** Ergebnis-Report eines Restore-Laufs. */
    public record RestoreReport(int restored, int skipped) {}
}
