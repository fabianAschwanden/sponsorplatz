package ch.sponsorplatz.projekt;

import java.util.List;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import ch.sponsorplatz.benutzer.AppUser;
import ch.sponsorplatz.benutzer.AppUserRepository;
import ch.sponsorplatz.organisation.MitgliedschaftRepository;
import ch.sponsorplatz.shared.config.ModelAttributeNames;

/**
 * Aggregierte „Meine Projekte"-Ansicht — listet alle Projekte aller
 * Organisationen, in denen der eingeloggte User Mitglied ist.
 * Ergänzt das pro-Org-Listing unter {@code /organisationen/{slug}/projekte}
 * mit einer org-übergreifenden Sicht für die Sidebar-Navigation.
 */
@Controller
public class MeineProjekteController {

    private final AppUserRepository appUserRepository;
    private final MitgliedschaftRepository mitgliedschaftRepository;
    private final ProjektService projektService;

    public MeineProjekteController(AppUserRepository appUserRepository,
            MitgliedschaftRepository mitgliedschaftRepository,
            ProjektService projektService) {
        this.appUserRepository = appUserRepository;
        this.mitgliedschaftRepository = mitgliedschaftRepository;
        this.projektService = projektService;
    }

    @GetMapping("/meine-projekte")
    @PreAuthorize("isAuthenticated()")
    public String meineProjekte(Authentication auth, Model model) {
        List<ProjektView> projekte = appUserRepository.findByEmail(auth.getName())
                .map(AppUser::getId)
                .map(mitgliedschaftRepository::findOrgIdsByUserId)
                .map(projektService::findeNachOrgIds)
                .orElse(List.of())
                .stream()
                .map(ProjektView::von)
                .toList();

        model.addAttribute(ModelAttributeNames.AKTIVE_SEITE, "projekte");
        model.addAttribute("projekte", projekte);
        return "meine-projekte";
    }
}
