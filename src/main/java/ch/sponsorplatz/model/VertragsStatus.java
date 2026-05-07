package ch.sponsorplatz.model;

/**
 * Status eines Sponsoring-Vertrags.
 *
 * <ul>
 *   <li>{@link #ENTWURF} — generiert aus angenommener Anfrage, Konditionen
 *       editierbar, noch nicht rechtsverbindlich</li>
 *   <li>{@link #UNTERZEICHNET} — beide Seiten haben unterschrieben (manuell
 *       in PDF oder via Markierung im UI), Konditionen final</li>
 *   <li>{@link #GEKUENDIGT} — vorzeitig beendet</li>
 * </ul>
 */
public enum VertragsStatus {
    ENTWURF,
    UNTERZEICHNET,
    GEKUENDIGT
}
