package ch.sponsorplatz.controller;

import ch.sponsorplatz.shared.config.ModelAttributeNames;
import ch.sponsorplatz.dto.WatchlistEintragView;
import ch.sponsorplatz.shared.exception.NotFoundException;
import ch.sponsorplatz.model.AppUser;
import ch.sponsorplatz.model.Projekt;
import ch.sponsorplatz.model.WatchlistEintrag;
import ch.sponsorplatz.repository.AppUserRepository;
import ch.sponsorplatz.service.ProjektService;
import ch.sponsorplatz.service.WatchlistService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/watchlist")
public class WatchlistController {

    private final WatchlistService watchlistService;
    private final ProjektService projektService;
    private final AppUserRepository appUserRepository;

    public WatchlistController(WatchlistService watchlistService,
                               ProjektService projektService,
                               AppUserRepository appUserRepository) {
        this.watchlistService = watchlistService;
        this.projektService = projektService;
        this.appUserRepository = appUserRepository;
    }

    @GetMapping
    public String liste(Authentication auth, Model model) {
        AppUser user = ladeUser(auth);
        List<WatchlistEintrag> eintraege = watchlistService.findeNachUser(user.getId());
        model.addAttribute(ModelAttributeNames.AKTIVE_SEITE, "watchlist");
        model.addAttribute("eintraege", WatchlistEintragView.von(eintraege));
        return "watchlist";
    }

    @PostMapping("/hinzufuegen/{projektSlug}")
    public String hinzufuegen(@PathVariable String projektSlug,
                              Authentication auth,
                              RedirectAttributes redirect) {
        AppUser user = ladeUser(auth);
        Projekt projekt = projektService.findeNachSlug(projektSlug)
                .orElseThrow(() -> new NotFoundException("Projekt nicht gefunden: " + projektSlug));
        watchlistService.hinzufuegen(user, projekt);
        redirect.addFlashAttribute(ModelAttributeNames.ERFOLGS_MELDUNG,
                "\"" + projekt.getName() + "\" zur Watchlist hinzugefügt.");
        return "redirect:/marktplatz/" + projektSlug;
    }

    @PostMapping("/entfernen/{projektSlug}")
    public String entfernen(@PathVariable String projektSlug,
                            Authentication auth,
                            RedirectAttributes redirect) {
        AppUser user = ladeUser(auth);
        Projekt projekt = projektService.findeNachSlug(projektSlug)
                .orElseThrow(() -> new NotFoundException("Projekt nicht gefunden: " + projektSlug));
        watchlistService.entferne(user.getId(), projekt.getId());
        redirect.addFlashAttribute(ModelAttributeNames.ERFOLGS_MELDUNG,
                "\"" + projekt.getName() + "\" von der Watchlist entfernt.");
        return "redirect:/watchlist";
    }

    private AppUser ladeUser(Authentication auth) {
        return appUserRepository.findByEmail(auth.getName())
                .orElseThrow(() -> new NotFoundException("User nicht gefunden"));
    }
}

