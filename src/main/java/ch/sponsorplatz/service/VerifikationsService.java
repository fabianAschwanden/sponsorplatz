package ch.sponsorplatz.service;

import ch.sponsorplatz.model.AppUser;
import ch.sponsorplatz.repository.AppUserRepository;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

/**
 * Service für E-Mail-Verifizierung nach Registrierung.
 */
@Service
@Transactional
public class VerifikationsService {

    private static final long TOKEN_GUELTIG_STUNDEN = 24;

    private final AppUserRepository repository;
    private final JavaMailSender mailSender;
    private final String basisUrl;
    private final String absender;

    public VerifikationsService(AppUserRepository repository,
                                JavaMailSender mailSender,
                                @Value("${sponsorplatz.basis-url:http://localhost:8080}") String basisUrl,
                                @Value("${sponsorplatz.mail.absender:noreply@sponsorplatz.ch}") String absender) {
        this.repository = repository;
        this.mailSender = mailSender;
        this.basisUrl = basisUrl;
        this.absender = absender;
    }

    /**
     * Generiert einen Verifikations-Token, setzt ihn auf dem User und sendet die Mail.
     */
    public void sendeVerifikationsMail(AppUser user) {
        String token = TokenGenerator.generiere();
        user.setVerifikationsToken(token);
        user.setTokenGueltigBis(Instant.now().plus(TOKEN_GUELTIG_STUNDEN, ChronoUnit.HOURS));
        repository.save(user);

        String link = basisUrl + "/verifizieren?token=" + token;
        sendeMail(user.getEmail(), user.getAnzeigename(), link);
    }

    /**
     * Verifiziert einen Token und aktiviert die E-Mail-Adresse.
     *
     * @throws IllegalArgumentException wenn Token unbekannt/ungültig
     * @throws IllegalStateException wenn Token abgelaufen
     */
    public void verifiziere(String token) {
        AppUser user = repository.findByVerifikationsToken(token)
                .orElseThrow(() -> new IllegalArgumentException("Verifikations-Token ist ungültig"));

        if (user.getTokenGueltigBis() == null || Instant.now().isAfter(user.getTokenGueltigBis())) {
            throw new IllegalStateException("Verifikations-Token ist abgelaufen. Bitte erneut anfordern.");
        }

        user.setEmailVerifiziert(true);
        user.setVerifikationsToken(null);
        user.setTokenGueltigBis(null);
        repository.save(user);
    }

    private void sendeMail(String empfaenger, String name, String link) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setTo(empfaenger);
            helper.setSubject("Sponsorplatz — E-Mail bestätigen");
            helper.setText(
                    "<h2>Willkommen bei Sponsorplatz, " + name + "!</h2>" +
                    "<p>Bitte bestätigen Sie Ihre E-Mail-Adresse:</p>" +
                    "<p><a href=\"" + link + "\">E-Mail bestätigen</a></p>" +
                    "<p>Der Link ist 24 Stunden gültig.</p>" +
                    "<p>Falls Sie sich nicht registriert haben, ignorieren Sie diese Mail.</p>",
                    true
            );
            helper.setFrom(absender);
            mailSender.send(message);
        } catch (MessagingException e) {
            throw new RuntimeException("Verifikations-Mail konnte nicht gesendet werden", e);
        }
    }
}

