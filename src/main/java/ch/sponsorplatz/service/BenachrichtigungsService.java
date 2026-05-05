package ch.sponsorplatz.service;

import ch.sponsorplatz.model.SponsoringAnfrage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * E-Mail-Benachrichtigungen für Sponsoring-Anfragen.
 */
@Service
public class BenachrichtigungsService {

    private static final Logger log = LoggerFactory.getLogger(BenachrichtigungsService.class);

    private final JavaMailSender mailSender;
    private final String absender;

    public BenachrichtigungsService(JavaMailSender mailSender,
                                    @Value("${sponsorplatz.mail.absender:noreply@sponsorplatz.ch}") String absender) {
        this.mailSender = mailSender;
        this.absender = absender;
    }

    @Async
    public void benachrichtigeUeberNeueAnfrage(SponsoringAnfrage anfrage, String empfaengerEmail) {
        if (empfaengerEmail == null || empfaengerEmail.isBlank()) {
            log.debug("Keine Empfänger-E-Mail — überspringe Benachrichtigung");
            return;
        }

        SimpleMailMessage mail = new SimpleMailMessage();
        mail.setFrom(absender);
        mail.setTo(empfaengerEmail);
        mail.setSubject("Neue Sponsoring-Anfrage auf Sponsorplatz");
        mail.setText(String.format(
                "Hallo,\n\nSie haben eine neue Sponsoring-Anfrage erhalten.\n\n" +
                "Von: %s\nNachricht: %s\n\n" +
                "Loggen Sie sich auf Sponsorplatz ein, um die Anfrage zu beantworten.\n\n" +
                "Freundliche Grüsse\nSponsorplatz",
                anfrage.getKontaktName() != null ? anfrage.getKontaktName() : "Unbekannt",
                anfrage.getNachricht()
        ));

        try {
            mailSender.send(mail);
            log.info("Benachrichtigung gesendet an {}", empfaengerEmail);
        } catch (Exception ex) {
            log.error("Fehler beim Senden der Benachrichtigung an {}: {}", empfaengerEmail, ex.getMessage());
        }
    }

    @Async
    public void benachrichtigeUeberAntwort(SponsoringAnfrage anfrage, String empfaengerEmail) {
        if (empfaengerEmail == null || empfaengerEmail.isBlank()) {
            return;
        }

        SimpleMailMessage mail = new SimpleMailMessage();
        mail.setFrom(absender);
        mail.setTo(empfaengerEmail);
        mail.setSubject("Antwort auf Ihre Sponsoring-Anfrage — Sponsorplatz");
        mail.setText(String.format(
                "Hallo,\n\nIhre Sponsoring-Anfrage wurde beantwortet.\n\n" +
                "Status: %s\nAntwort: %s\n\n" +
                "Freundliche Grüsse\nSponsorplatz",
                anfrage.getStatus().name(),
                anfrage.getAntwort() != null ? anfrage.getAntwort() : "—"
        ));

        try {
            mailSender.send(mail);
        } catch (Exception ex) {
            log.error("Fehler beim Senden der Antwort-Benachrichtigung: {}", ex.getMessage());
        }
    }
}

