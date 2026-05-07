package ch.sponsorplatz.benutzer;
import ch.sponsorplatz.shared.mail.MailService;
import ch.sponsorplatz.shared.util.TokenGenerator;

import jakarta.mail.MessagingException;
import org.springframework.beans.factory.annotation.Value;
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
    private final MailService mailService;
    private final String basisUrl;

    public VerifikationsService(AppUserRepository repository,
                                MailService mailService,
                                @Value("${sponsorplatz.basis-url:http://localhost:8080}") String basisUrl) {
        this.repository = repository;
        this.mailService = mailService;
        this.basisUrl = basisUrl;
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
        mailService.sendeHtml(empfaenger, "Sponsorplatz — E-Mail bestätigen", helper -> {
            try {
                helper.setText(
                        "<h2>Willkommen bei Sponsorplatz, " + name + "!</h2>" +
                                "<p>Bitte bestätigen Sie Ihre E-Mail-Adresse:</p>" +
                                "<p><a href=\"" + link + "\">E-Mail bestätigen</a></p>" +
                                "<p>Der Link ist 24 Stunden gültig.</p>" +
                                "<p>Falls Sie sich nicht registriert haben, ignorieren Sie diese Mail.</p>",
                        true
                );
            } catch (MessagingException e) {
                throw new RuntimeException("Verifikations-Mail konnte nicht aufgebaut werden", e);
            }
        });
    }
}

