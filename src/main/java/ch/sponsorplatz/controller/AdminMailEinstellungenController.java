package ch.sponsorplatz.controller;

import ch.sponsorplatz.dto.MailEinstellungenFormDto;
import ch.sponsorplatz.dto.MailEinstellungenView;
import ch.sponsorplatz.shared.einstellungen.PlattformEinstellungen;
import ch.sponsorplatz.shared.mail.MailService;
import ch.sponsorplatz.shared.einstellungen.PlattformEinstellungenService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Admin-UI für SMTP-Einstellungen.
 *
 * <p>Persistente Werte überschreiben ENV-Defaults zur Laufzeit (siehe
 * {@link MailService}). Test-Mail-Button sendet sofort eine Mail an die
 * konfigurierte Test-Empfänger-Adresse.
 *
 * <p>Live-Modus ({@code MAIL_LIVE}) ist <strong>nicht</strong> editierbar
 * — bewusst nur via ENV gesteuert (Sicherheit gegen Klick-Unfälle).
 */
@Controller
@RequestMapping("/admin/mail-einstellungen")
@PreAuthorize("hasRole('PLATFORM_ADMIN')")
public class AdminMailEinstellungenController {

    private static final Logger log = LoggerFactory.getLogger(AdminMailEinstellungenController.class);

    private final PlattformEinstellungenService einstellungenService;
    private final MailService mailService;

    public AdminMailEinstellungenController(PlattformEinstellungenService einstellungenService,
                                            MailService mailService) {
        this.einstellungenService = einstellungenService;
        this.mailService = mailService;
    }

    @GetMapping
    public String anzeigen(Model model) {
        PlattformEinstellungen e = einstellungenService.lade();
        model.addAttribute("einstellungen", MailEinstellungenView.von(
                e,
                mailService.effektiverHost(),
                mailService.effektiverAbsender(),
                mailService.effektiverTestEmpfaenger(),
                mailService.istLiveMode(),
                mailService.istKonfiguriert()
        ));
        if (!model.containsAttribute("form")) {
            MailEinstellungenFormDto form = new MailEinstellungenFormDto();
            form.setSmtpHost(e.getSmtpHost());
            form.setSmtpPort(e.getSmtpPort());
            form.setSmtpUser(e.getSmtpUser());
            // smtpPassword bewusst nicht ins Form — Klartext bleibt in DB
            form.setSmtpAuth(e.isSmtpAuth());
            form.setSmtpStarttls(e.isSmtpStarttls());
            form.setMailAbsender(e.getMailAbsender());
            form.setMailTestEmpfaenger(e.getMailTestEmpfaenger());
            model.addAttribute("form", form);
        }
        return "admin/mail-einstellungen";
    }

    @PostMapping("/speichern")
    public String speichern(@ModelAttribute("form") MailEinstellungenFormDto form,
                            Authentication auth,
                            RedirectAttributes redirect) {
        PlattformEinstellungen e = einstellungenService.lade();
        e.setSmtpHost(blankToNull(form.getSmtpHost()));
        e.setSmtpPort(form.getSmtpPort());
        e.setSmtpUser(blankToNull(form.getSmtpUser()));
        if (form.getSmtpPassword() != null && !form.getSmtpPassword().isBlank()) {
            e.setSmtpPassword(form.getSmtpPassword());
        }
        e.setSmtpAuth(form.isSmtpAuth());
        e.setSmtpStarttls(form.isSmtpStarttls());
        e.setMailAbsender(blankToNull(form.getMailAbsender()));
        e.setMailTestEmpfaenger(blankToNull(form.getMailTestEmpfaenger()));
        einstellungenService.speichere(e, auth.getName());

        log.info("Mail-Einstellungen aktualisiert von {}", auth.getName());
        redirect.addFlashAttribute("erfolgsMeldung", "Mail-Einstellungen gespeichert.");
        return "redirect:/admin/mail-einstellungen";
    }

    @PostMapping("/test")
    public String testMail(RedirectAttributes redirect) {
        try {
            mailService.sendeTestMail();
            redirect.addFlashAttribute("erfolgsMeldung",
                    "Test-Mail wurde an " + mailService.effektiverTestEmpfaenger() + " gesendet.");
        } catch (IllegalStateException e) {
            redirect.addFlashAttribute("fehlerMeldung", e.getMessage());
        } catch (RuntimeException e) {
            log.warn("Test-Mail fehlgeschlagen: {}", e.getMessage());
            redirect.addFlashAttribute("fehlerMeldung",
                    "Test-Mail-Versand fehlgeschlagen: " + e.getMessage());
        }
        return "redirect:/admin/mail-einstellungen";
    }

    private static String blankToNull(String s) {
        return (s == null || s.isBlank()) ? null : s.trim();
    }
}
