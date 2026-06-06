package ch.sponsorplatz.shared.mail;

/**
 * Framework-freier E-Mail-Anhang. Teil des {@link MailVersand}-Ports, damit
 * Aufrufer keine Spring-/Jakarta-Mail-Typen (MimeMessageHelper, DataSource)
 * berühren müssen.
 *
 * @param dateiname  Anzeigename des Anhangs (z.B. "rechnung.pdf")
 * @param inhalt     Roh-Bytes des Anhangs
 * @param contentTyp MIME-Typ (z.B. "application/pdf")
 */
public record MailAnhang(String dateiname, byte[] inhalt, String contentTyp) {
}
