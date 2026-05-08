package ch.sponsorplatz.backup;

import java.nio.file.Path;

/**
 * Optionaler Hook für {@link BackupService}: lädt eine erstellte Backup-Datei
 * in einen externen, ausfallsicheren Storage (z.B. OCI Object Storage).
 *
 * <p>Nicht-existenz im Spring-Context ist explizit erlaubt — {@link BackupService}
 * arbeitet dann rein lokal. Aktive Implementierungen werden über
 * {@code @ConditionalOnProperty(...="oci")} aktiviert.
 *
 * <p>Implementierungen sollen Fehler werfen, damit der Aufrufer protokollieren kann;
 * der lokale Backup-Pfad bleibt davon unberührt.
 */
public interface BackupCloudUploader {

    /**
     * Lädt {@code datei} in den konfigurierten Cloud-Bucket. Der Object-Key ist
     * implementation-detail (z.B. {@code backups/<dateiname>}).
     *
     * @return externer Storage-Pfad/-Key, unter dem die Datei abgelegt wurde
     */
    String lade(Path datei);
}
