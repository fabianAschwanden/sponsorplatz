package ch.sponsorplatz.einladung;

import ch.sponsorplatz.organisation.Rolle;

import java.time.Instant;

/**
 * Read-only View für die Einladungs-Vorschau-Page.
 * Wird auf dem GET-Endpunkt {@code /einladung/annehmen} gerendert,
 * bevor der User per POST die Annahme bestätigt (Outlook-/Slack-Crawler-Schutz).
 */
public record EinladungVorschauView(
        String token,
        String orgName,
        String eingeladenVonName,
        Rolle rolle,
        String email,
        Instant gueltigBis
) {

    public static EinladungVorschauView von(Einladung e) {
        return new EinladungVorschauView(
                e.getToken(),
                e.getOrg().getName(),
                e.getEingeladenVon().getAnzeigename(),
                e.getRolle(),
                e.getEmail(),
                e.getGueltigBis()
        );
    }
}
