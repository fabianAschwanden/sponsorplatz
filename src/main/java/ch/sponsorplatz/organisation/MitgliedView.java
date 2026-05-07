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
        String userEmail
) {

    public static MitgliedView von(Mitgliedschaft m) {
        return new MitgliedView(
                m.getId(),
                m.getRolle(),
                m.getBeigetretenAm(),
                m.getUser().getAnzeigename(),
                m.getUser().getEmail()
        );
    }

    public static List<MitgliedView> von(List<Mitgliedschaft> mitglieder) {
        return mitglieder.stream().map(MitgliedView::von).toList();
    }
}
