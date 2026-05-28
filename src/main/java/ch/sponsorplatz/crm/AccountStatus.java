package ch.sponsorplatz.crm;

/**
 * Lebenszyklus eines {@link SponsorAccount} aus Sicht des Sponsor-Teams.
 * Alleinige Source of Truth (kein DB-CHECK-Constraint, Pattern V44/V45/V46).
 */
public enum AccountStatus {
    /** Beobachtet / qualifiziert, noch kein Engagement. */
    LEAD,
    /** Laufendes Sponsoring-Engagement. */
    AKTIV,
    /** Vertrag läuft aus, Verlängerung in Arbeit. */
    IN_RENEWAL,
    /** Engagement beendet / nicht verlängert. */
    VERLOREN,
    /** Bewusst nicht engagieren (Reputations-/Compliance-Gründe). */
    DO_NOT_ENGAGE
}
