package ch.sponsorplatz.crm;

/**
 * Art einer {@link Aktivitaet} (Dynamics „activitytypecode"). Alleinige Source
 * of Truth (kein DB-CHECK).
 */
public enum AktivitaetTyp {
    /** Telefonat. */
    ANRUF,
    /** E-Mail-Kontakt. */
    EMAIL,
    /** Persönliches Treffen / Termin. */
    MEETING,
    /** Besuch eines Vereins-Events. */
    EVENT_BESUCH,
    /** Allgemeine Notiz ohne Interaktions-Kanal. */
    NOTIZ
}
