package ch.sponsorplatz.backup;

import ch.sponsorplatz.audit.AuditAktion;
import ch.sponsorplatz.audit.AuditService;
import ch.sponsorplatz.projekt.MedienAsset;
import ch.sponsorplatz.projekt.MedienAssetRepository;
import ch.sponsorplatz.shared.storage.StorageObjectNotFoundException;
import ch.sponsorplatz.shared.storage.StorageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Erstellt + verwaltet Backups der Medien-Dateien aus {@link StorageService}.
 *
 * <p>Format: ZIP-Archiv unter {@code <backup-verzeichnis>/sponsorplatz_uploads_<ts>.zip}.
 * Jede {@link MedienAsset}-Zeile wird durchgegangen, die referenzierte Datei via
 * {@code storageService.ladeAlsResource(asset.storagePfad)} gezogen und unter
 * exakt diesem Pfad als ZIP-Entry abgelegt. Orphaned Assets
 * (Storage-Objekt fehlt) werden geloggt und übersprungen — der Backup wird
 * nicht abgebrochen.
 *
 * <p>Der Restore liegt im Paralleldienst {@link DateiBackupRestoreService}.
 */
@Service
public class DateiBackupService {

    private static final Logger log = LoggerFactory.getLogger(DateiBackupService.class);
    private static final DateTimeFormatter TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");
    private static final String DATEI_PRAEFIX = "sponsorplatz_uploads_";
    private static final String SUFFIX = ".zip";

    private final MedienAssetRepository medienAssetRepository;
    private final StorageService storageService;
    private final AuditService auditService;

    @Value("${sponsorplatz.backup.verzeichnis:./backups}")
    private String backupVerzeichnis;

    public DateiBackupService(MedienAssetRepository medienAssetRepository,
                              StorageService storageService,
                              AuditService auditService) {
        this.medienAssetRepository = medienAssetRepository;
        this.storageService = storageService;
        this.auditService = auditService;
    }

    /**
     * Erstellt ein ZIP-Backup aller im DB-Index referenzierten Storage-Objekte.
     * Bricht nicht ab, wenn einzelne Objekte fehlen — die werden im Audit-Log
     * dokumentiert.
     *
     * @return Pfad der erstellten ZIP-Datei
     */
    public Path erstelleDateiBackup() throws IOException {
        Path verzeichnis = Paths.get(backupVerzeichnis);
        Files.createDirectories(verzeichnis);

        String timestamp = LocalDateTime.now().format(TIMESTAMP_FORMAT);
        Path zipPfad = verzeichnis.resolve(DATEI_PRAEFIX + timestamp + SUFFIX);

        List<MedienAsset> assets = medienAssetRepository.findAll();
        int included = 0;
        int skipped = 0;

        try (OutputStream fileOut = Files.newOutputStream(zipPfad);
             ZipOutputStream zip = new ZipOutputStream(fileOut)) {
            for (MedienAsset asset : assets) {
                try {
                    Resource resource = storageService.ladeAlsResource(asset.getStoragePfad());
                    ZipEntry entry = new ZipEntry(asset.getStoragePfad());
                    if (asset.getContentType() != null) {
                        entry.setComment(asset.getContentType());
                    }
                    zip.putNextEntry(entry);
                    try (InputStream in = resource.getInputStream()) {
                        in.transferTo(zip);
                    }
                    zip.closeEntry();
                    included++;
                } catch (StorageObjectNotFoundException e) {
                    log.warn("Datei-Backup-Skip — Asset {} fehlt im Storage", asset.getStoragePfad());
                    skipped++;
                }
            }
        }

        String detail = zipPfad + " (" + included + " inkl., " + skipped + " skipped)";
        auditService.protokolliere(AuditAktion.DATEI_BACKUP_ERSTELLT, "SYSTEM", null, "DATEI_BACKUP", detail);
        log.info("Datei-Backup erstellt: {} ({} Dateien, {} skipped)", zipPfad, included, skipped);
        return zipPfad;
    }

    /**
     * Listet vorhandene ZIP-Backups, neueste zuerst.
     */
    public List<Path> listeDateiBackups() throws IOException {
        Path verzeichnis = Paths.get(backupVerzeichnis);
        if (!Files.exists(verzeichnis)) {
            return List.of();
        }
        try (Stream<Path> stream = Files.list(verzeichnis)) {
            List<Path> liste = new ArrayList<>();
            stream.filter(p -> p.getFileName().toString().startsWith(DATEI_PRAEFIX)
                            && p.getFileName().toString().endsWith(SUFFIX))
                    .sorted(Comparator.reverseOrder())
                    .forEach(liste::add);
            return liste;
        }
    }

    /**
     * Liest den Inhalt einer Backup-Datei für den HTTP-Download.
     */
    public byte[] leseDateiBackup(String dateiname) throws IOException {
        validiereDateiname(dateiname);
        Path pfad = Paths.get(backupVerzeichnis).resolve(dateiname);
        return Files.readAllBytes(pfad);
    }

    /**
     * Löscht eine Backup-Datei.
     */
    public void loescheDateiBackup(String dateiname) throws IOException {
        validiereDateiname(dateiname);
        Path pfad = Paths.get(backupVerzeichnis).resolve(dateiname);
        Files.deleteIfExists(pfad);
    }

    private void validiereDateiname(String dateiname) {
        if (dateiname == null || dateiname.isBlank()) {
            throw new IllegalArgumentException("Dateiname darf nicht leer sein");
        }
        if (dateiname.contains("..") || dateiname.contains("/") || dateiname.contains("\\")) {
            throw new IllegalArgumentException("Ungültiger Dateiname: " + dateiname);
        }
        if (!dateiname.startsWith(DATEI_PRAEFIX) || !dateiname.endsWith(SUFFIX)) {
            throw new IllegalArgumentException(
                    "Erwartet '" + DATEI_PRAEFIX + "<ts>" + SUFFIX + "', war: " + dateiname);
        }
    }
}
