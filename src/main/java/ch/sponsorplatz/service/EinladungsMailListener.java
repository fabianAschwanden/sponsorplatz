package ch.sponsorplatz.service;

import ch.sponsorplatz.event.EinladungErstelltEvent;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Sendet die Einladungs-Mail erst nach dem erfolgreichen DB-Commit (H4-Fix).
 *
 * <p>Mail-Versand-Fehler werden geloggt aber nicht propagiert — die Tx ist zu
 * diesem Zeitpunkt schon commited; ein erneutes Throw würde nichts mehr rollback'n,
 * sondern nur einen Server-Error im (vom User bereits empfangenen) Response
 * verursachen. Bei Mail-Failure existiert die Einladung in der DB; ein späterer
 * manueller Re-Send ist möglich.</p>
 */
@Component
public class EinladungsMailListener {

    private static final Logger log = LoggerFactory.getLogger(EinladungsMailListener.class);

    private final JavaMailSender mailSender;
    private final String basisUrl;

    public EinladungsMailListener(JavaMailSender mailSender,
                                  @Value("${sponsorplatz.basis-url:http://localhost:8080}") String basisUrl) {
        this.mailSender = mailSender;
        this.basisUrl = basisUrl;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void aufEinladungErstellt(EinladungErstelltEvent event) {
        try {
            sendeMail(event);
        } catch (MessagingException | RuntimeException ex) {
            // Tx ist schon committed — wir können den State nicht mehr rollback'n.
            // Loggen reicht; Re-Send-Mechanismus ist Backlog (M2).
            log.warn("Einladungs-Mail an {} konnte nicht gesendet werden: {}",
                    event.empfaengerEmail(), ex.getMessage());
        }
    }

    private void sendeMail(EinladungErstelltEvent event) throws MessagingException {
        String link = basisUrl + "/einladung/annehmen?token=" + event.token();
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
        helper.setTo(event.empfaengerEmail());
        helper.setSubject("Sponsorplatz — Einladung zu " + event.orgName());
        helper.setText(
                "<h2>Sie wurden eingeladen!</h2>" +
                "<p>" + event.eingeladenVonName() + " hat Sie eingeladen, Mitglied von <strong>"
                + event.orgName() + "</strong> zu werden.</p>" +
                "<p><a href=\"" + link + "\">Einladung annehmen</a></p>" +
                "<p>Der Link ist 7 Tage gültig.</p>",
                true
        );
        helper.setFrom("noreply@sponsorplatz.ch");
        mailSender.send(message);
    }
}
