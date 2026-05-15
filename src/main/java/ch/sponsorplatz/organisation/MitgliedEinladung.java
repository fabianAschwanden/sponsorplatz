package ch.sponsorplatz.organisation;

import java.util.UUID;

/**
 * Port für das Erstellen einer Org-Einladung — wird vom Mitglieder-UI in
 * {@code organisation} konsumiert. Die Implementation lebt im
 * {@code einladung}-Paket (Adapter), damit {@code organisation} nicht in eine
 * höhere Schicht greifen muss (ARCH-06).
 */
public interface MitgliedEinladung {

    /**
     * Legt eine Einladung an und triggert den Mailversand asynchron.
     *
     * @param orgId            Org, in die eingeladen wird
     * @param email            Empfänger-Mail (bereits normalisiert)
     * @param rolle            zukünftige Rolle in der Org
     * @param eingeladenVonId  einladender User
     * @throws IllegalArgumentException falls die Mail bereits eingeladen ist
     */
    void erstelleEinladung(UUID orgId, String email, Rolle rolle, UUID eingeladenVonId);
}
