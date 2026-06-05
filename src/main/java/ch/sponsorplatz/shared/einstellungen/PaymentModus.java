package ch.sponsorplatz.shared.einstellungen;

/**
 * Aktiver Payment-Modus der Plattform — steuerbar via Admin-UI.
 *
 * <ul>
 *   <li>{@link #QR_RECHNUNG} — Swiss QR-Bill per E-Mail (Default, kostenlos)</li>
 *   <li>{@link #DATATRANS} — Online-Zahlung via Datatrans Hosted Payment Page</li>
 * </ul>
 */
public enum PaymentModus {
    /** Swiss QR-Bill wird per E-Mail an den Sponsor gesendet. */
    QR_RECHNUNG,
    /** Online-Zahlung via Datatrans (TWINT, Kreditkarte, PostFinance). */
    DATATRANS
}

