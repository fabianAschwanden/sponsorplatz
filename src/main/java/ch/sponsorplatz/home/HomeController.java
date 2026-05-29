package ch.sponsorplatz.home;

import ch.sponsorplatz.anfrage.EngagementService;
import ch.sponsorplatz.anfrage.StartseitenTeaser;
import ch.sponsorplatz.shared.config.ModelAttributeNames;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Startseite und einfache Public-Routen.
 *
 * <p>Die Startseite zeigt — auch anonym — einen Engagement-Teaser: die neuesten
 * öffentlichen Sponsorings (angenommene Anfragen) als Beleg dafür, dass auf der
 * Plattform real gefördert wird. Verlinkt in die Marken-Schaufenster.
 */
@Controller
public class HomeController {

    /** Wie viele Engagements der Startseiten-Teaser höchstens zeigt. */
    private static final int ANZAHL_TEASER = 6;

    private final EngagementService engagementService;

    public HomeController(EngagementService engagementService) {
        this.engagementService = engagementService;
    }

    @GetMapping("/")
    public String index(@RequestParam(required = false) String kanton, Model model) {
        StartseitenTeaser teaser = engagementService.findeStartseitenEngagements(kanton, ANZAHL_TEASER);
        model.addAttribute(ModelAttributeNames.AKTIVE_SEITE, "home");
        model.addAttribute("featuredEngagements", teaser.engagements());
        model.addAttribute("teaserKantone", teaser.verfuegbareKantone());
        model.addAttribute("teaserKanton", teaser.filterKanton());
        model.addAttribute("teaserVorhanden", teaser.vorhanden());
        return "home/index";
    }
}
