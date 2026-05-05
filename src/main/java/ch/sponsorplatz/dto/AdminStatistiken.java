package ch.sponsorplatz.dto;

/**
 * Statistiken für das Admin-Dashboard.
 */
public record AdminStatistiken(
        long anzahlBenutzer,
        long anzahlAktiveBenutzer,
        long anzahlOrganisationen,
        long anzahlPendingOrgs,
        long anzahlVerifizierteOrgs,
        long anzahlProjekte,
        long anzahlAnfragen,
        long anzahlNachrichten
) {
}

