package ch.sponsorplatz.projekt;

import ch.sponsorplatz.shared.config.ModelAttributeNames;
import ch.sponsorplatz.benutzer.AppUserService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.UUID;

@Controller
@RequestMapping("/watchlist")
public class WatchlistController {

    private final WatchlistService watchlistService;
    private final AppUserService appUserService;

    public WatchlistController(WatchlistService watchlistService,
                               AppUserService appUserService) {
        this.watchlistService = watchlistService;
        this.appUserService = appUserService;
    }

    @GetMapping
    public String liste(Authentication auth, Model model) {
        UUID userId = appUserService.findeIdNachEmail(auth.getName());
        model.addAttribute(ModelAttributeNames.AKTIVE_SEITE, "watchlist");
        model.addAttribute("eintraege", watchlistService.findeViewsNachUser(userId));
        return "watchlist";
    }

    @PostMapping("/hinzufuegen/{projektSlug}")
    public String hinzufuegen(@PathVariable String projektSlug,
                              Authentication auth,
                              RedirectAttributes redirect) {
        UUID userId = appUserService.findeIdNachEmail(auth.getName());
        String projektName = watchlistService.hinzufuegenNachSlug(userId, projektSlug);
        redirect.addFlashAttribute(ModelAttributeNames.ERFOLGS_MELDUNG,
                "\"" + projektName + "\" zur Watchlist hinzugefügt.");
        return "redirect:/marktplatz/" + projektSlug;
    }

    @PostMapping("/entfernen/{projektSlug}")
    public String entfernen(@PathVariable String projektSlug,
                            Authentication auth,
                            RedirectAttributes redirect) {
        UUID userId = appUserService.findeIdNachEmail(auth.getName());
        String projektName = watchlistService.entfernenNachSlug(userId, projektSlug);
        redirect.addFlashAttribute(ModelAttributeNames.ERFOLGS_MELDUNG,
                "\"" + projektName + "\" von der Watchlist entfernt.");
        return "redirect:/watchlist";
    }
}
