package ch.sponsorplatz.organisation;


import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Immutable View-DTO für Mitgliedschaft — flacht user.anzeigename / user.email ein,
 * damit Templates nicht über Lazy-Relationen navigieren müssen und kein
 * passwortHash o.ä. ins View kommen kann.
 */
public record MitgliedView(
        UUID id,
        Rolle rolle,
        Instant beigetretenAm,
        String userAnzeigename,
        String userEmail,
        String userProfilbildUrl
) {

    public static MitgliedView von(Mitgliedschaft m) {
        var user = m.getUser();
        String bildUrl = user.getProfilbildId() != null
                ? "/medien/" + user.getProfilbildId()
                : null;
        return new MitgliedView(
                m.getId(),
                m.getRolle(),
                m.getBeigetretenAm(),
                user.getAnzeigename(),
                user.getEmail(),
                bildUrl
        );
    }

    public static List<MitgliedView> von(List<Mitgliedschaft> mitglieder) {
        return mitglieder.stream().map(MitgliedView::von).toList();
    }
}
