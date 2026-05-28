package ch.sponsorplatz.crm;

/**
 * Funktionale Rolle einer {@link KontaktPerson} am Account. {@code HAUPTANSPRECHPARTNER}
 * entspricht dem Dynamics-„Primary Contact" — pro Account höchstens einer
 * (Business-Regel im Service). Alleinige Source of Truth (kein DB-CHECK).
 */
public enum KontaktRolle {
    /** Primärer Ansprechpartner (Dynamics primarycontactid). */
    HAUPTANSPRECHPARTNER,
    /** Stellvertretung. */
    STELLVERTRETER,
    /** Buchhaltung / Finanzen. */
    BUCHHALTUNG,
    /** Presse / Kommunikation. */
    PRESSE,
    /** Sonstige Funktion. */
    SONSTIGE
}
