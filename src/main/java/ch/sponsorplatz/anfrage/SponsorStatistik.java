package ch.sponsorplatz.anfrage;

import ch.sponsorplatz.organisation.Branche;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * Read-only View-DTO für das Sponsor-Statistik-Dashboard (Phase 5.C).
 * Aggregiert über alle UNTERNEHMEN-Orgs eines Users und zeigt:
 *
 * <ul>
 *   <li><b>Engagements</b> — Anzahl Verträge je Status (ENTWURF/UNTERZEICHNET/GEKUENDIGT)
 *       + Gesamt-Sponsoring-Volumen (Summe Preis der unterzeichneten Verträge)</li>
 *   <li><b>Anfragen-Pipeline</b> — Conversion-Sicht der vom Sponsor ausgehenden
 *       Anfragen (NEU/ANGENOMMEN/ABGELEHNT)</li>
 *   <li><b>Rechnungs-Status</b> — Liquiditäts-Sicht (OFFEN/BEZAHLT/STORNIERT)</li>
 *   <li><b>Branchen-Verteilung</b> — wie viele Verträge in welche Health-Branche
 *       fließen (Marketing-Reporting "wo engagieren wir uns")</li>
 * </ul>
 *
 * <p>Leeres DTO via {@link #leer()} — wird verwendet, wenn der User kein
 * Mitglied einer UNTERNEHMEN-Org ist.
 */
public record SponsorStatistik(
        // Vertrags-Kennzahlen
        long anzahlVertraegeEntwurf,
        long anzahlVertraegeUnterzeichnet,
        long anzahlVertraegeGekuendigt,
        BigDecimal volumenChfUnterzeichnet,

        // Anfragen-Pipeline (Sponsor-Sicht: anfragenderOrg = Sponsor-Org)
        long anzahlAnfragenNeu,
        long anzahlAnfragenAngenommen,
        long anzahlAnfragenAbgelehnt,

        // Rechnungs-Status (über vertrag.sponsorOrg verknüpft)
        long anzahlRechnungenOffen,
        long anzahlRechnungenBezahlt,
        long anzahlRechnungenStorniert,

        // Branchen-Verteilung (nur UNTERZEICHNETe Verträge)
        Map<Branche, Long> vertraegeProBranche,

        // Liste der Sponsor-Org-Namen (für Header „Statistik über X, Y")
        List<String> sponsorOrgNamen
) {

    /** Leeres DTO für User ohne UNTERNEHMEN-Org-Mitgliedschaft. */
    public static SponsorStatistik leer() {
        return new SponsorStatistik(
                0, 0, 0, BigDecimal.ZERO,
                0, 0, 0,
                0, 0, 0,
                Map.of(), List.of()
        );
    }

    /** Conversion-Rate „angenommen / total beantwortet" in Prozent (0-100). */
    public int conversionRateProzent() {
        long beantwortet = anzahlAnfragenAngenommen + anzahlAnfragenAbgelehnt;
        if (beantwortet == 0) return 0;
        return (int) Math.round(100.0 * anzahlAnfragenAngenommen / beantwortet);
    }

    /** True, wenn der User wenigstens eine Sponsor-Org hat — sonst zeigt das Template einen Empty-Hint. */
    public boolean hatSponsorOrgs() {
        return !sponsorOrgNamen.isEmpty();
    }
}
