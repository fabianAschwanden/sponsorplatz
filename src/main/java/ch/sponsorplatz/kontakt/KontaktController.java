package ch.sponsorplatz.kontakt;

import ch.sponsorplatz.shared.config.ModelAttributeNames;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Öffentliche Kontakt-/Anfrage-Seite — der einzige Funnel-Punkt für anonyme
 * Besucher neben Login/Registrierung. Versandt wird per Mail an die
 * PLATFORM_ADMINs ({@link KontaktService}), nicht in der DB persistiert.
 */
@Controller
@RequestMapping("/kontakt")
public class KontaktController {

    private final KontaktService kontaktService;

    public KontaktController(KontaktService kontaktService) {
        this.kontaktService = kontaktService;
    }

    @GetMapping
    public String formular(Model model) {
        if (!model.containsAttribute("kontaktForm")) {
            model.addAttribute("kontaktForm", new KontaktFormDto());
        }
        return "kontakt";
    }

    @PostMapping
    public String absenden(@Valid @ModelAttribute("kontaktForm") KontaktFormDto form,
                           BindingResult binding,
                           RedirectAttributes redirect) {
        // Honeypot: nicht-leer ⇒ Bot. Silent-Success — kein User-Hint, dass wir
        // gefiltert haben.
        if (form.getHomepage() != null && !form.getHomepage().isBlank()) {
            redirect.addFlashAttribute(ModelAttributeNames.ERFOLGS_MELDUNG,
                    "Vielen Dank — wir melden uns in Kürze.");
            return "redirect:/kontakt";
        }
        if (binding.hasErrors()) {
            return "kontakt";
        }
        kontaktService.verarbeite(form);
        redirect.addFlashAttribute(ModelAttributeNames.ERFOLGS_MELDUNG,
                "Vielen Dank — wir melden uns in Kürze.");
        return "redirect:/kontakt";
    }
}
