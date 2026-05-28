package ch.sponsorplatz.crm;

/**
 * Strategische Einstufung eines {@link SponsorAccount} im Portfolio.
 * Optional (null = nicht eingestuft). Alleinige Source of Truth.
 */
public enum AccountTier {
    /** Strategisches Leuchtturm-Engagement. */
    STRATEGIC,
    /** Kern-Portfolio. */
    CORE,
    /** Long-Tail / opportunistisch. */
    LONG_TAIL
}
