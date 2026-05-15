package ch.sponsorplatz.benutzer;

import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

/**
 * Port für das Profilbild-Upload — kapselt die Medien-Service-Details aus
 * der Sicht von {@code benutzer}. Die Implementation lebt im {@code projekt}-
 * Paket (wo der Medien-Stack liegt), damit {@code benutzer} nicht in höhere
 * Schichten greift (ARCH-06).
 */
public interface ProfilbildSpeicherung {

    /** Speichert ein neues Profilbild für den User und liefert die Asset-ID zurück. */
    UUID speichereProfilbild(MultipartFile datei, UUID userId);
}
