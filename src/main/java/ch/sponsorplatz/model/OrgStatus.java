package ch.sponsorplatz.model;

/**
 * Status einer Organisation im Plattform-Lebenszyklus.
 *
 * Übergänge:
 *   PENDING ── verifiziert ──> VERIFIED ── aktiviert ──> ACTIVE
 *      \                                                  /
 *       \__ suspendiert ___________________________ SUSPENDED
 */
public enum OrgStatus {
    /** Frisch registriert, wartet auf Verifizierung. */
    PENDING,
    /** Plattform-Admin oder Auto-Verifizierung (Zefix) hat geprüft. */
    VERIFIED,
    /** Aktiv genutzt, im Marktplatz sichtbar. */
    ACTIVE,
    /** Temporär gesperrt durch Plattform-Admin / Moderator. */
    SUSPENDED
}
