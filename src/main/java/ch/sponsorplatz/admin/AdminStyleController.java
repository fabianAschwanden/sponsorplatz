package ch.sponsorplatz.admin;

import ch.sponsorplatz.shared.einstellungen.PlattformEinstellungenService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Admin-UI zum Umschalten des Plattform-Styles. Die Auswahl landet als
 * String in {@code plattform_einstellungen.aktiver_style} und wird vom
 * {@code StyleAdvice} an alle Templates durchgereicht.
 */
@Controller
@RequestMapping("/admin/style")
@PreAuthorize("hasRole('PLATFORM_ADMIN')")
public class AdminStyleController {

    private static final Logger log = LoggerFactory.getLogger(AdminStyleController.class);

    private final PlattformEinstellungenService einstellungenService;

    public AdminStyleController(PlattformEinstellungenService einstellungenService) {
        this.einstellungenService = einstellungenService;
    }

    @GetMapping
    public String anzeigen(Model model) {
        model.addAttribute("aktiverStyle", einstellungenService.ladeAktivenStyle());
        model.addAttribute("verfuegbareStyles",
                PlattformEinstellungenService.GUELTIGE_STYLES);
        return "admin/style";
    }

    @PostMapping("/speichern")
    public String speichern(@RequestParam String style,
                            Authentication auth,
                            RedirectAttributes redirect) {
        try {
            einstellungenService.setzeAktivenStyle(style, auth.getName());
            log.info("Plattform-Style auf '{}' geschaltet von {}", style, auth.getName());
            redirect.addFlashAttribute("erfolgsMeldung",
                    "Plattform-Style wurde auf \"" + style + "\" umgeschaltet.");
        } catch (IllegalArgumentException e) {
            redirect.addFlashAttribute("fehlerMeldung", e.getMessage());
        }
        return "redirect:/admin/style";
    }
}
