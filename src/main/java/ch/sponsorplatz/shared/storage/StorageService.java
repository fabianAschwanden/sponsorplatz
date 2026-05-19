package ch.sponsorplatz.shared.storage;

import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

/**
 * Abstrahiert die Datei-Speicherung.
 * Dev: lokales Dateisystem. Prod: S3 oder ähnlich.
 */
public interface StorageService {

    /**
     * Speichert eine Datei und gibt den Storage-Pfad zurück.
     */
    String speichere(MultipartFile datei, String zielpfad);

    /**
     * Speichert bereits geladene Bytes unter dem gegebenen Pfad — primär für
     * den ZIP-Restore-Pfad in {@code DateiBackupRestoreService}, der einzelne
     * ZIP-Entries als {@code byte[]} aus dem Archiv liest.
     */
    String speichereBytes(byte[] inhalt, String contentType, String zielpfad);

    /**
     * Löscht eine Datei anhand des Storage-Pfads.
     */
    void loesche(String storagePfad);

    /**
     * Lädt eine Datei als Resource für HTTP-Response.
     */
    Resource ladeAlsResource(String storagePfad);
}

