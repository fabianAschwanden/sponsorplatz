package ch.sponsorplatz.anfrage;

import ch.sponsorplatz.organisation.Branche;
import ch.sponsorplatz.shared.config.ModelAttributeNames;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Öffentliches Engagement-Schaufenster: zeigt, welche Vereine eine Marke in
 * welcher Region unterstützt (PROJEKT_INFO §"Öffentliches Schaufenster").
 * Route: /marken/{slug}/engagements (permitAll).
 */
@Controller
@RequestMapping("/marken/{slug}/engagements")
public class EngagementController {

    private final EngagementService engagementService;

    public EngagementController(EngagementService engagementService) {
        this.engagementService = engagementService;
    }

    @GetMapping
    public String schaufenster(@PathVariable String slug,
                               @RequestParam(required = false) String kanton,
                               @RequestParam(required = false) Branche branche,
                               Model model) {
        model.addAttribute(ModelAttributeNames.AKTIVE_SEITE, "marktplatz");
        model.addAttribute("ansicht", engagementService.findeSchaufenster(slug, kanton, branche));
        model.addAttribute("sponsorSlug", slug);
        return "anfrage/engagement-schaufenster";
    }
}
