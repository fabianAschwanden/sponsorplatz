package ch.sponsorplatz.home;

import ch.sponsorplatz.shared.config.ModelAttributeNames;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Startseite und einfache Public-Routen.
 *
 * Phase 0 — minimaler Stand. Wird in Phase 2/3 zur Marktplatz-Landing-Page ausgebaut.
 */
@Controller
public class HomeController {

    @GetMapping("/")
    public String index(Model model) {
        model.addAttribute(ModelAttributeNames.AKTIVE_SEITE, "home");
        return "index";
    }
}
