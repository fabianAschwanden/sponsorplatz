package ch.sponsorplatz.kontakt;

import ch.sponsorplatz.benutzer.AppUser;
import ch.sponsorplatz.benutzer.AppUserRepository;
import ch.sponsorplatz.benutzer.PlatformRolle;
import ch.sponsorplatz.shared.mail.MailService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Verarbeitet anonyme Plattform-Kontakt-Anfragen: protokolliert sie und
 * benachrichtigt alle PLATFORM_ADMINs per Mail. Bewusst ohne DB-Persistenz —
 * der Audit-Trail liegt im Mail-Posteingang + Log.
 */
@Service
public class KontaktService {

    private static final Logger log = LoggerFactory.getLogger(KontaktService.class);

    private final AppUserRepository appUserRepository;
    private final MailService mailService;

    public KontaktService(AppUserRepository appUserRepository, MailService mailService) {
        this.appUserRepository = appUserRepository;
        this.mailService = mailService;
    }

    /**
     * Nimmt eine Kontakt-Anfrage entgegen und benachrichtigt Admins.
     * Mail-Versand schlägt einzeln still fehl (WARN-Log), damit ein SMTP-
     * Ausfall den Submit-Erfolg nicht blockiert.
     */
    public void verarbeite(KontaktFormDto form) {
        log.info("Plattform-Kontakt von {} <{}>: {}",
                form.getName(), form.getEmail(), form.getBetreff());

        List<AppUser> admins = appUserRepository.findByPlatformRolle(PlatformRolle.PLATFORM_ADMIN);
        if (admins.isEmpty()) {
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

        for (AppUser admin : admins) {
            sendeStillSicher(admin.getEmail(), subject, body);
        }
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
