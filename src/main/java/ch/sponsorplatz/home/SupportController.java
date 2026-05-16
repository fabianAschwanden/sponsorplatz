package ch.sponsorplatz.home;

import ch.sponsorplatz.shared.config.ModelAttributeNames;
import ch.sponsorplatz.shared.mail.MailService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Support-Seite — eingeloggte User können eine Nachricht an den
 * Plattform-Admin senden. Die Mail geht an die konfigurierte
 * Admin-Adresse ({@code sponsorplatz.support.empfaenger}).
 */
@Controller
@RequestMapping("/support")
@PreAuthorize("isAuthenticated()")
public class SupportController {

    private static final Logger log = LoggerFactory.getLogger(SupportController.class);

    private final MailService mailService;
    private final String supportEmpfaenger;

    public SupportController(MailService mailService,
                             @Value("${sponsorplatz.support.empfaenger:#{null}}") String supportEmpfaenger) {
        this.mailService = mailService;
        this.supportEmpfaenger = supportEmpfaenger;
    }

    @GetMapping
    public String formular(Model model) {
        model.addAttribute(ModelAttributeNames.AKTIVE_SEITE, "support");
        model.addAttribute("supportForm", new SupportFormDto());
        return "home/support";
    }

    @PostMapping
    public String absenden(@Valid @ModelAttribute("supportForm") SupportFormDto dto,
                           BindingResult br,
                           Authentication auth,
                           Model model,
                           RedirectAttributes redirect) {
        if (br.hasErrors()) {
            model.addAttribute(ModelAttributeNames.AKTIVE_SEITE, "support");
            return "home/support";
        }

        String absenderEmail = auth.getName();
        String betreff = "[Sponsorplatz Support] " + dto.getBetreff();
        String inhalt = """
                Support-Anfrage von: %s
                Betreff: %s

                %s
                """.formatted(absenderEmail, dto.getBetreff(), dto.getNachricht());

        String empfaenger = ermittleEmpfaenger();

        try {
            mailService.sendePlain(empfaenger, betreff, inhalt);
            // PII-Reduktion: User-Email steht bereits in der Mail selbst, nicht
            // zusätzlich ins INFO-Log schreiben (DSG-Datenexport-Hygiene).
            log.info("Support-Anfrage gesendet (Empfänger: {})", empfaenger);
            redirect.addFlashAttribute(ModelAttributeNames.ERFOLGS_MELDUNG,
                    "Ihre Support-Anfrage wurde gesendet. Wir melden uns so schnell wie möglich.");
            return "redirect:/support";
        } catch (Exception e) {
            // Mail-Versand fehlgeschlagen — User darf nicht im Glauben gelassen
            // werden, dass die Anfrage angekommen ist (sie ist nicht persistiert).
            // Form bleibt offen, User sieht ehrliche Fehler-Meldung mit
            // direkter Mail-Adresse als Fallback.
            log.error("Support-Mail-Versand fehlgeschlagen — User muss manuell kontaktieren. Empfänger={}, Fehler={}",
                    empfaenger, e.getMessage());
            model.addAttribute(ModelAttributeNames.AKTIVE_SEITE, "support");
            model.addAttribute(ModelAttributeNames.FEHLERMELDUNG,
                    "Mail-Versand ist aktuell nicht möglich. Bitte senden Sie Ihre Anfrage direkt an "
                            + empfaenger + " — Ihre Eingabe bleibt unten erhalten.");
            return "home/support";
        }
    }

    private String ermittleEmpfaenger() {
        if (supportEmpfaenger != null && !supportEmpfaenger.isBlank()) {
            return supportEmpfaenger;
        }
        // Fallback: Absender-Adresse der Plattform (Admin bekommt es so oder so mit)
        return mailService.effektiverAbsender();
    }

    /**
     * DTO für das Support-Formular.
     */
    public static class SupportFormDto {

        @NotBlank(message = "Bitte geben Sie einen Betreff ein.")
        @Size(max = 200, message = "Betreff darf maximal 200 Zeichen lang sein.")
        private String betreff;

        @NotBlank(message = "Bitte beschreiben Sie Ihr Anliegen.")
        @Size(min = 10, max = 5000, message = "Nachricht muss zwischen 10 und 5000 Zeichen lang sein.")
        private String nachricht;

        public String getBetreff() { return betreff; }
        public void setBetreff(String betreff) { this.betreff = betreff; }

        public String getNachricht() { return nachricht; }
        public void setNachricht(String nachricht) { this.nachricht = nachricht; }
    }
}

