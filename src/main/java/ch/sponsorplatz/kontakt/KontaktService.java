package ch.sponsorplatz.kontakt;

import ch.sponsorplatz.benutzer.AppUserService;
import ch.sponsorplatz.shared.mail.MailService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;

/**
 * Verarbeitet anonyme Plattform-Kontakt-Anfragen: protokolliert sie und
 * benachrichtigt alle PLATFORM_ADMINs per Mail. Bewusst ohne DB-Persistenz —
 * der Audit-Trail liegt im Mail-Posteingang + Log.
 *
 * <p>Mail-Inhalt ist immer auf Deutsch (Admin-Sprache der Plattform), die
 * Bestätigungsmeldung an den Absender ist i18n-isiert via
 * {@link MessageSource}.
 */
@Service
public class KontaktService {

    private static final Logger log = LoggerFactory.getLogger(KontaktService.class);

    private final AppUserService appUserService;
    private final MailService mailService;
    private final MessageSource messageSource;

    public KontaktService(AppUserService appUserService,
                          MailService mailService,
                          MessageSource messageSource) {
        this.appUserService = appUserService;
        this.mailService = mailService;
        this.messageSource = messageSource;
    }

    /**
     * Nimmt eine Kontakt-Anfrage entgegen und benachrichtigt Admins.
     * Mail-Versand schlägt einzeln still fehl (WARN-Log), damit ein SMTP-
     * Ausfall den Submit-Erfolg nicht blockiert.
     */
    public void verarbeite(KontaktFormDto form) {
        log.info("Plattform-Kontakt von {} <{}>: {}",
                form.getName(), form.getEmail(), form.getBetreff());

        List<String> adminEmails = appUserService.findeAdminEmails();
        if (adminEmails.isEmpty()) {
            log.warn("Keine PLATFORM_ADMIN-User vorhanden — Kontakt-Anfrage '{}' bleibt nur im Log",
                    form.getBetreff());
            return;
        }

        String subject = "[Sponsorplatz] Kontakt: " + form.getBetreff();
        String body = "Neue Kontakt-Anfrage über die öffentliche /kontakt-Seite:\n\n"
                + "Name:    " + form.getName() + "\n"
                + "E-Mail:  " + form.getEmail() + "\n"
                + "Betreff: " + form.getBetreff() + "\n\n"
                + "Nachricht:\n"
                + form.getNachricht() + "\n";

        for (String adminEmail : adminEmails) {
            sendeStillSicher(adminEmail, subject, body);
        }
    }

    /**
     * Lokalisierte Bestätigungsmeldung für den Submitter — die Sprache kommt
     * aus dem aktuellen Request-Locale, das der {@code LocaleChangeInterceptor}
     * gesetzt hat.
     */
    public String erfolgsMeldung(Locale locale) {
        return messageSource.getMessage("kontakt.bestaetigung", null, locale);
    }

    private void sendeStillSicher(String to, String subject, String body) {
        try {
            mailService.sendePlain(to, subject, body);
        } catch (RuntimeException ex) {
            log.warn("Kontakt-Mail an {} fehlgeschlagen ({}): {}",
                    to, ex.getClass().getSimpleName(), ex.getMessage());
        }
    }
}
