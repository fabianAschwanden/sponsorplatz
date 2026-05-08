package ch.sponsorplatz.anfrage;

import ch.sponsorplatz.organisation.Branche;
import ch.sponsorplatz.shared.config.ModelAttributeNames;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

/**
 * Öffentliches Engagement-Schaufenster: zeigt welche Vereine ein Sponsor unterstützt.
 * Route: /marken/{slug}/engagements (permitAll)
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
                               @RequestParam(required = false) String region,
                               @RequestParam(required = false) Branche branche,
                               Model model) {
        List<SponsoringAnfrage> anfragen;

        if (region != null && !region.isBlank()) {
            anfragen = engagementService.findeNachSponsorSlugUndRegion(slug, region);
        } else if (branche != null) {
            anfragen = engagementService.findeNachSponsorSlugUndBranche(slug, branche);
        } else {
            anfragen = engagementService.findeNachSponsorSlug(slug);
        }

        model.addAttribute(ModelAttributeNames.AKTIVE_SEITE, "marktplatz");
        model.addAttribute("engagements", EngagementView.von(anfragen));
        model.addAttribute("sponsorSlug", slug);
        model.addAttribute("filterRegion", region);
        model.addAttribute("filterBranche", branche);
        model.addAttribute("alleBranchen", Branche.values());
        return "engagement-schaufenster";
    }
}

