package ch.sponsorplatz.model;

/**
 * Status einer Sponsoring-Rechnung.
 *
 * <ul>
 *   <li>{@link #OFFEN} — Rechnung erstellt, Sponsor zahlt per QR-Bill</li>
 *   <li>{@link #BEZAHLT} — Verein hat manuell als bezahlt markiert</li>
 *   <li>{@link #STORNIERT} — vor Bezahlung zurückgezogen</li>
 * </ul>
 */
public enum RechnungsStatus {
    OFFEN,
    BEZAHLT,
    STORNIERT
}
