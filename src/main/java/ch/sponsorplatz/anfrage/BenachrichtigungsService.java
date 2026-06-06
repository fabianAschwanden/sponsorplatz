package ch.sponsorplatz.anfrage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import org.springframework.mail.MailException;

import ch.sponsorplatz.shared.mail.MailVersand;

/**
 * E-Mail-Benachrichtigungen für Sponsoring-Anfragen.
 *
 * <p>
 * Sendet via {@link MailVersand} — die Live-/Test-Modus-Logik und
 * SMTP-Settings (DB &gt; ENV) liegen dort zentral.
 */
@Service
public class BenachrichtigungsService {

    private static final Logger log = LoggerFactory.getLogger(BenachrichtigungsService.class);

    private final MailVersand mailService;

    public BenachrichtigungsService(MailVersand mailService) {
        this.mailService = mailService;
    }

    @Async
    public void benachrichtigeUeberNeueAnfrage(SponsoringAnfrage anfrage, String empfaengerEmail) {
        if (empfaengerEmail == null || empfaengerEmail.isBlank()) {
            log.debug("Keine Empfänger-E-Mail — überspringe Benachrichtigung");
            return;
        }

        String body = String.format(
                "Hallo,%n%nSie haben eine neue Sponsoring-Anfrage erhalten.%n%n" +
                        "Von: %s%nNachricht: %s%n%n" +
                        "Loggen Sie sich auf Sponsorplatz ein, um die Anfrage zu beantworten.%n%n" +
                        "Freundliche Grüsse%nSponsorplatz",
                anfrage.getKontaktName() != null ? anfrage.getKontaktName() : "Unbekannt",
                anfrage.getNachricht());

        try {
            mailService.sendePlain(empfaengerEmail, "Neue Sponsoring-Anfrage auf Sponsorplatz", body);
            log.info("Benachrichtigung gesendet an {}", empfaengerEmail);
        } catch (MailException ex) {
            log.error("Fehler beim Senden der Benachrichtigung an {}: {}", empfaengerEmail, ex.getMessage());
        }
    }

    @Async
    public void benachrichtigeUeberAntwort(SponsoringAnfrage anfrage, String empfaengerEmail) {
        if (empfaengerEmail == null || empfaengerEmail.isBlank()) {
            return;
        }

        String body = String.format(
                "Hallo,%n%nIhre Sponsoring-Anfrage wurde beantwortet.%n%n" +
                        "Status: %s%nAntwort: %s%n%n" +
                        "Freundliche Grüsse%nSponsorplatz",
                anfrage.getStatus().name(),
                anfrage.getAntwort() != null ? anfrage.getAntwort() : "—");

        try {
            mailService.sendePlain(empfaengerEmail, "Antwort auf Ihre Sponsoring-Anfrage — Sponsorplatz", body);
        } catch (MailException ex) {
            log.error("Fehler beim Senden der Antwort-Benachrichtigung: {}", ex.getMessage());
        }
    }
}
