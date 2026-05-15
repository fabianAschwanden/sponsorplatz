package ch.sponsorplatz.projekt;

import ch.sponsorplatz.organisation.Branche;
import ch.sponsorplatz.shared.config.ModelAttributeNames;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * Landing-Page für Sponsor-Marken — öffentlich erreichbar.
 * Zeigt Health-Use-Cases und Live-Statistiken.
 */
@Controller
@RequestMapping("/fuer-marken")
public class MarkenLandingController {

    private final StatistikService statistikService;

    public MarkenLandingController(StatistikService statistikService) {
        this.statistikService = statistikService;
    }

    @GetMapping
    public String landing(Model model) {
        model.addAttribute(ModelAttributeNames.AKTIVE_SEITE, "fuer-marken");
        model.addAttribute("vereineProBranche", statistikService.vereineProBranche());
        model.addAttribute("anzahlProjekte", statistikService.anzahlAktiveProjekte());
        model.addAttribute("alleBranchen", Branche.values());
        return "marken-landing";
    }
}

