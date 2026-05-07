package ch.sponsorplatz.service;
import ch.sponsorplatz.shared.mail.MailService;
import ch.sponsorplatz.shared.util.TokenGenerator;

import ch.sponsorplatz.model.AppUser;
import ch.sponsorplatz.repository.AppUserRepository;
import jakarta.mail.MessagingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

/**
 * Service für Passwort-Reset per E-Mail-Token.
 *
 * Flow:
 * 1. User gibt E-Mail ein → Token wird generiert + Mail gesendet
 * 2. User klickt Link → Token wird validiert
 * 3. User gibt neues PW ein → PW wird gesetzt, Token gelöscht
 */
@Service
@Transactional
public class PasswortResetService {

    private static final Logger log = LoggerFactory.getLogger(PasswortResetService.class);
    private static final long TOKEN_GUELTIG_STUNDEN = 1;

    private final AppUserRepository repository;
    private final MailService mailService;
    private final PasswordEncoder passwordEncoder;
    private final String basisUrl;

    public PasswortResetService(AppUserRepository repository,
                                 MailService mailService,
                                 PasswordEncoder passwordEncoder,
                                 @Value("${sponsorplatz.basis-url:http://localhost:8080}") String basisUrl) {
        this.repository = repository;
        this.mailService = mailService;
        this.passwordEncoder = passwordEncoder;
        this.basisUrl = basisUrl;
    }

    /**
     * Sendet eine Passwort-Reset-Mail an die gegebene E-Mail-Adresse.
     * Gibt still zurück wenn die E-Mail nicht existiert (kein Information Leak).
     */
    public void sendeResetMail(String email) {
        repository.findByEmail(email.toLowerCase().trim()).ifPresent(user -> {
            String token = TokenGenerator.generiere();
            user.setResetToken(token);
            user.setResetTokenGueltigBis(Instant.now().plus(TOKEN_GUELTIG_STUNDEN, ChronoUnit.HOURS));
            repository.save(user);

            String link = basisUrl + "/passwort-reset?token=" + token;
            sendeMailIntern(user.getEmail(), user.getAnzeigename(), link);
            log.info("Passwort-Reset-Mail gesendet an {}", user.getEmail());
        });
    }

    /**
     * Validiert einen Reset-Token.
     *
     * @throws IllegalArgumentException wenn Token ungültig
     * @throws IllegalStateException wenn Token abgelaufen
     */
    public AppUser validiereToken(String token) {
        AppUser user = repository.findByResetToken(token)
                .orElseThrow(() -> new IllegalArgumentException("Reset-Link ist ungültig"));

        if (user.getResetTokenGueltigBis() == null || Instant.now().isAfter(user.getResetTokenGueltigBis())) {
            throw new IllegalStateException("Reset-Link ist abgelaufen. Bitte erneut anfordern.");
        }

        return user;
    }

    /**
     * Setzt ein neues Passwort für den User mit dem gegebenen Token.
     *
     * @throws IllegalArgumentException wenn Token ungültig oder PW zu kurz
     * @throws IllegalStateException wenn Token abgelaufen
     */
    public void setzeNeuesPasswort(String token, String neuesPasswort) {
        if (neuesPasswort == null || neuesPasswort.length() < 8) {
            throw new IllegalArgumentException("Passwort muss mindestens 8 Zeichen haben");
        }

        AppUser user = validiereToken(token);
        user.setPasswortHash(passwordEncoder.encode(neuesPasswort));
        user.setResetToken(null);
        user.setResetTokenGueltigBis(null);
        repository.save(user);

        log.info("Passwort zurückgesetzt für {}", user.getEmail());
    }

    private void sendeMailIntern(String empfaenger, String name, String link) {
        mailService.sendeHtml(empfaenger, "Sponsorplatz — Passwort zurücksetzen", helper -> {
            try {
                helper.setText(
                        "<h2>Passwort zurücksetzen</h2>" +
                                "<p>Hallo " + name + ",</p>" +
                                "<p>Sie haben ein neues Passwort angefordert. Klicken Sie auf den folgenden Link:</p>" +
                                "<p><a href=\"" + link + "\">Neues Passwort setzen</a></p>" +
                                "<p>Der Link ist <strong>1 Stunde</strong> gültig.</p>" +
                                "<p>Falls Sie kein neues Passwort angefordert haben, ignorieren Sie diese Mail.</p>" +
                                "<p>Freundliche Grüsse<br>Sponsorplatz</p>",
                        true
                );
            } catch (MessagingException e) {
                throw new RuntimeException("Reset-Mail konnte nicht aufgebaut werden", e);
            }
        });
    }
}

