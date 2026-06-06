package ch.sponsorplatz.shared.mail;

/**
 * Port für den E-Mail-Versand (Hexagonal / Ports-&-Adapter).
 *
 * <p>Die Domäne/Anwendungsdienste hängen ausschliesslich an diesem Interface,
 * nie an einer konkreten Transport-Technologie. Der einzige Adapter ist heute
 * {@link SmtpMailVersand} (SMTP via Spring/Jakarta-Mail); ein zweiter Adapter
 * (z.B. API-basierter Provider) liesse sich ohne Änderung an den Aufrufern
 * ergänzen.
 *
 * <p>Bewusst <strong>framework-frei</strong>: kein {@code MimeMessageHelper},
 * kein {@code JavaMailSender} in der Signatur. Anhänge laufen über das
 * Value-Object {@link MailAnhang}.
 */
public interface MailVersand {

    /** Sendet eine einfache Plain-Text-Mail. */
    void sendePlain(String to, String subject, String body);

    /** Sendet eine HTML-Mail (ohne Anhang). */
    void sendeHtml(String to, String subject, String htmlBody);

    /** Sendet eine HTML-Mail mit genau einem Anhang. */
    void sendeHtmlMitAnhang(String to, String subject, String htmlBody, MailAnhang anhang);

    // ── Konfigurations-Introspektion (für Admin-UI / Ops) ──────────────────

    /** True, wenn ein SMTP-Host effektiv konfiguriert ist (DB oder ENV). */
    boolean istKonfiguriert();

    /** True, wenn Mails an echte Empfänger gehen (MAIL_LIVE=true). */
    boolean istLiveMode();

    /** Effektiver SMTP-Host (DB &gt; ENV). */
    String effektiverHost();

    /** Effektiver Test-Empfänger (DB &gt; ENV); leer = keiner. */
    String effektiverTestEmpfaenger();

    /** Effektive Absender-Adresse (DB &gt; ENV). */
    String effektiverAbsender();

    /**
     * Sendet eine Test-Mail an den konfigurierten Test-Empfänger — ohne
     * Live-Mode-Routing.
     *
     * @throws IllegalStateException wenn kein Test-Empfänger konfiguriert ist
     */
    void sendeTestMail();
}
