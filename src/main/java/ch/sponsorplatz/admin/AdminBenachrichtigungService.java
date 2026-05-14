package ch.sponsorplatz.admin;

import ch.sponsorplatz.benachrichtigung.BenachrichtigungTyp;
import ch.sponsorplatz.benachrichtigung.NotificationService;
import ch.sponsorplatz.benutzer.AppUser;
import ch.sponsorplatz.benutzer.AppUserRepository;
import ch.sponsorplatz.benutzer.PlatformRolle;
import ch.sponsorplatz.organisation.OrgTyp;
import ch.sponsorplatz.organisation.Organisation;
import ch.sponsorplatz.shared.mail.MailService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Pusht Admin-relevante Events an alle {@link PlatformRolle#PLATFORM_ADMIN}-User
 * — gleichzeitig als In-App-Glocke (NotificationService) und als E-Mail
 * (MailService). Aktuell ein Event-Typ: neue Org-Registrierungen, damit die
 * Verifizierungs-Queue nicht aktiv überwacht werden muss.
 *
 * <p>Mail-Versand schlägt einzeln still fehl (WARN-Log), damit ein SMTP-Ausfall
 * weder die Registrierung blockiert noch andere Admins um die In-App-Notification
 * bringt.
 */
@Service
@Transactional
public class AdminBenachrichtigungService {

    private static final Logger log = LoggerFactory.getLogger(AdminBenachrichtigungService.class);

    private final AppUserRepository appUserRepository;
    private final NotificationService notificationService;
    private final MailService mailService;

    public AdminBenachrichtigungService(AppUserRepository appUserRepository,
                                         NotificationService notificationService,
                                         MailService mailService) {
        this.appUserRepository = appUserRepository;
        this.notificationService = notificationService;
        this.mailService = mailService;
    }

    /**
     * Benachrichtigt alle PLATFORM_ADMINs über eine frisch registrierte
     * (PENDING-)Organisation — In-App-Notification + E-Mail.
     */
    public void benachrichtigeUeberNeueOrgRegistrierung(Organisation org) {
        List<AppUser> admins = appUserRepository.findByPlatformRolle(PlatformRolle.PLATFORM_ADMIN);
        if (admins.isEmpty()) {
            log.warn("Keine PLATFORM_ADMIN-User vorhanden — Org-Registrierung '{}' bleibt unbenachrichtigt",
                    org.getName());
            return;
        }

        String titel = titelFuer(org);
        String text = textFuer(org);
        String link = "/admin/verifizierungen";

        for (AppUser admin : admins) {
            notificationService.benachrichtige(admin.getId(),
                    BenachrichtigungTyp.NEUE_ORG_REGISTRIERT, titel, text, link);
            sendeMailStillSicher(admin, titel, text);
        }
    }

    private void sendeMailStillSicher(AppUser admin, String subject, String body) {
        try {
            mailService.sendePlain(admin.getEmail(), subject, body);
        } catch (RuntimeException ex) {
            log.warn("Admin-Mail an {} fehlgeschlagen ({}): {}",
                    admin.getEmail(), ex.getClass().getSimpleName(), ex.getMessage());
        }
    }

    private String titelFuer(Organisation org) {
        String typLabel = org.getTyp() == OrgTyp.VEREIN ? "Verein" : "Sponsor-Organisation";
        return "Neue " + typLabel + ": " + org.getName();
    }

    private String textFuer(Organisation org) {
        String typLabel = org.getTyp() == OrgTyp.VEREIN ? "Ein Verein" : "Eine Sponsor-Organisation";
        return typLabel + " hat sich registriert und wartet auf Verifizierung: "
                + org.getName() + ".";
    }
}
