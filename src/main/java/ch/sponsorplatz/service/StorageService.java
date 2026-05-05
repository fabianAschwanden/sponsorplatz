package ch.sponsorplatz.service;

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
     * Löscht eine Datei anhand des Storage-Pfads.
     */
    void loesche(String storagePfad);

    /**
     * Lädt eine Datei als Resource für HTTP-Response.
     */
    Resource ladeAlsResource(String storagePfad);
}

