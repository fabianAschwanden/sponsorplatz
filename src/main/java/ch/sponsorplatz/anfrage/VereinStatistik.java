package ch.sponsorplatz.anfrage;

import java.math.BigDecimal;
import java.util.List;

/**
 * Read-only View-DTO für das Vereins-Statistik-Dashboard (Phase 5.C, Verein-Seite).
 * Aggregiert über alle VEREIN-Orgs eines Users.
 *
 * <p>Fachliche Perspektive (anders als {@link SponsorStatistik}):
 * <ul>
 *   <li><b>Projekte/Pakete</b> — was hat der Verein veröffentlicht</li>
 *   <li><b>Eingehende Anfragen</b> — Sponsoren, die auf den Verein zugehen</li>
 *   <li><b>Ausgehende Kontakt-Anfragen</b> — Sponsoren, die der Verein proaktiv anfragt</li>
 *   <li><b>Aktive Sponsoring-Verträge</b> — Anzahl + Einnahmen-Summe</li>
 *   <li><b>Rechnungs-Status</b> — Liquiditäts-Sicht aus Vereins-Sicht</li>
 * </ul>
 */
public record VereinStatistik(
        // Eigene Inhalte
        long anzahlProjekteVeroeffentlicht,
        long anzahlPakete,

        // Eingehende Anfragen (Sponsor → Verein, klassische Paket-Anfragen)
        long anzahlAnfragenEingehendNeu,
        long anzahlAnfragenEingehendAngenommen,
        long anzahlAnfragenEingehendAbgelehnt,

        // Ausgehende Anfragen (Verein → Sponsor, Kontakt-Anfragen)
        long anzahlAnfragenAusgehendNeu,
        long anzahlAnfragenAusgehendAngenommen,
        long anzahlAnfragenAusgehendAbgelehnt,

        // Aktive Verträge + Einnahmen
        long anzahlVertraegeEntwurf,
        long anzahlVertraegeUnterzeichnet,
        long anzahlVertraegeGekuendigt,
        BigDecimal einnahmenChfUnterzeichnet,

        // Rechnungs-Status (Verein als rechnungsstellende Org)
        long anzahlRechnungenOffen,
        long anzahlRechnungenBezahlt,
        long anzahlRechnungenStorniert,

        // Verein-Namen für Header
        List<String> vereinOrgNamen
) {

    /** Leeres DTO für User ohne VEREIN-Org-Mitgliedschaft. */
    public static VereinStatistik leer() {
        return new VereinStatistik(
                0, 0,
                0, 0, 0,
                0, 0, 0,
                0, 0, 0, BigDecimal.ZERO,
                0, 0, 0,
                List.of()
        );
    }

    /** Conversion-Rate „angenommen / beantwortet" über alle eingehenden Anfragen (0–100). */
    public int conversionRateEingehendProzent() {
        long beantwortet = anzahlAnfragenEingehendAngenommen + anzahlAnfragenEingehendAbgelehnt;
        if (beantwortet == 0) return 0;
        return (int) Math.round(100.0 * anzahlAnfragenEingehendAngenommen / beantwortet);
    }

    /** Conversion-Rate für ausgehende Kontakt-Anfragen — wie viele Sponsoren sagen Ja. */
    public int conversionRateAusgehendProzent() {
        long beantwortet = anzahlAnfragenAusgehendAngenommen + anzahlAnfragenAusgehendAbgelehnt;
        if (beantwortet == 0) return 0;
        return (int) Math.round(100.0 * anzahlAnfragenAusgehendAngenommen / beantwortet);
    }

    /** True, wenn der User Mitglied mind. einer VEREIN-Org ist. */
    public boolean hatVereinOrgs() {
        return !vereinOrgNamen.isEmpty();
    }
}
