package ch.sponsorplatz.anfrage;

import ch.sponsorplatz.organisation.Branche;
import ch.sponsorplatz.organisation.Kanton;
import ch.sponsorplatz.organisation.OrgTyp;
import ch.sponsorplatz.organisation.Organisation;
import ch.sponsorplatz.organisation.PlzVerzeichnis;
import ch.sponsorplatz.projekt.Projekt;
import ch.sponsorplatz.projekt.SponsoringPaket;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Öffentliche Ansicht eines aktiven Engagements (aus angenommener Anfrage).
 * Zeigt, welche Marke welchen Verein (und ggf. welches Projekt) unterstützt.
 *
 * <p>Projekt-/Paket-/Region-Felder sind {@code null}, wenn das Engagement aus
 * einer Kontakt-Anfrage stammt (kein Marktplatz-Paket). Die Verein-/Marken-Rolle
 * wird über den {@link OrgTyp} bestimmt, weil bei Kontakt-Anfragen der Verein der
 * Anfragende sein kann (Verein → Marke), bei Paket-Anfragen der Empfänger.
 */
public record EngagementView(
        UUID id,
        String sponsorName,
        String sponsorSlug,
        String vereinName,
        String vereinSlug,
        Branche vereinBranche,
        String vereinLogoUrl,
        String projektName,
        String projektSlug,
        String paketName,
        String region,
        Kanton kanton,
        Instant angenommenAm
) {
    /** Der Verein der Anfrage — über den Org-Typ aufgelöst (siehe Klassen-Doc). */
    static Organisation vereinVon(SponsoringAnfrage anfrage) {
        Organisation empfaenger = anfrage.getEmpfaengerOrg();
        return empfaenger.getTyp() == OrgTyp.VEREIN ? empfaenger : anfrage.getAnfragenderOrg();
    }

    public static EngagementView von(SponsoringAnfrage anfrage) {
        return von(anfrage, null);
    }

    /** Wie {@link #von(SponsoringAnfrage)}, zusätzlich mit Verein-Logo-URL (oder {@code null}). */
    public static EngagementView von(SponsoringAnfrage anfrage, String vereinLogoUrl) {
        Organisation empfaenger = anfrage.getEmpfaengerOrg();
        Organisation anfragender = anfrage.getAnfragenderOrg();
        boolean empfaengerIstVerein = empfaenger.getTyp() == OrgTyp.VEREIN;
        Organisation verein = empfaengerIstVerein ? empfaenger : anfragender;
        Organisation sponsor = empfaengerIstVerein ? anfragender : empfaenger;

        SponsoringPaket paket = anfrage.getPaket();
        Projekt projekt = (paket != null) ? paket.getProjekt() : null;
        String projektOrt = projekt != null ? projekt.getOrt() : null;

        // Kanton bestmöglich bestimmen: Verein-PLZ → Verein-Ort → Projekt-Ort.
        Kanton kanton = PlzVerzeichnis.kantonVon(verein.getPostleitzahl())
                .or(() -> PlzVerzeichnis.kantonVonOrt(verein.getOrt()))
                .or(() -> PlzVerzeichnis.kantonVonOrt(projektOrt))
                .orElse(null);

        return new EngagementView(
                anfrage.getId(),
                sponsor.getName(),
                sponsor.getSlug(),
                verein.getName(),
                verein.getSlug(),
                verein.getBranche(),
                vereinLogoUrl,
                projekt != null ? projekt.getName() : null,
                projekt != null ? projekt.getSlug() : null,
                paket != null ? paket.getName() : null,
                projektOrt,
                kanton,
                anfrage.getBeantwortetAm()
        );
    }

    public static List<EngagementView> von(List<SponsoringAnfrage> anfragen) {
        return anfragen.stream().map(EngagementView::von).toList();
    }
}

