package ch.sponsorplatz.anfrage;

import ch.sponsorplatz.organisation.Branche;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Öffentliche Ansicht eines aktiven Engagements (aus angenommener Anfrage).
 * Zeigt, welcher Sponsor welchen Verein/welches Projekt unterstützt.
 */
public record EngagementView(
        UUID id,
        String sponsorName,
        String sponsorSlug,
        String vereinName,
        String vereinSlug,
        Branche vereinBranche,
        String projektName,
        String projektSlug,
        String paketName,
        String region,
        Instant angenommenAm
) {
    public static EngagementView von(SponsoringAnfrage anfrage) {
        var sponsor = anfrage.getAnfragenderOrg();
        var verein = anfrage.getEmpfaengerOrg();
        var paket = anfrage.getPaket();
        var projekt = paket.getProjekt();
        return new EngagementView(
                anfrage.getId(),
                sponsor.getName(),
                sponsor.getSlug(),
                verein.getName(),
                verein.getSlug(),
                verein.getBranche(),
                projekt.getName(),
                projekt.getSlug(),
                paket.getName(),
                projekt.getOrt(),
                anfrage.getBeantwortetAm()
        );
    }

    public static List<EngagementView> von(List<SponsoringAnfrage> anfragen) {
        return anfragen.stream().map(EngagementView::von).toList();
    }
}

