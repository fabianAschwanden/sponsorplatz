package ch.sponsorplatz.projekt;

import java.util.List;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import ch.sponsorplatz.benutzer.AppUserService;
import ch.sponsorplatz.organisation.MitgliedschaftService;
import ch.sponsorplatz.shared.config.ModelAttributeNames;

/**
 * Aggregierte „Meine Projekte"-Ansicht — listet alle Projekte aller
 * Organisationen, in denen der eingeloggte User Mitglied ist.
 * Ergänzt das pro-Org-Listing unter {@code /organisationen/{slug}/projekte}
 * mit einer org-übergreifenden Sicht für die Sidebar-Navigation.
 */
@Controller
public class MeineProjekteController {

    private final AppUserService appUserService;
    private final MitgliedschaftService mitgliedschaftService;
    private final ProjektService projektService;

    public MeineProjekteController(AppUserService appUserService,
            MitgliedschaftService mitgliedschaftService,
            ProjektService projektService) {
        this.appUserService = appUserService;
        this.mitgliedschaftService = mitgliedschaftService;
        this.projektService = projektService;
    }

    @GetMapping("/meine-projekte")
    @PreAuthorize("isAuthenticated()")
    public String meineProjekte(Authentication auth, Model model) {
        var userId = appUserService.findeIdNachEmail(auth.getName());
        var orgIds = mitgliedschaftService.findeOrgIdsVonUser(userId);
        List<ProjektView> projekte = projektService.findeNachOrgIds(orgIds).stream()
                .map(ProjektView::von)
                .toList();

        model.addAttribute(ModelAttributeNames.AKTIVE_SEITE, "projekte");
        model.addAttribute("projekte", projekte);
        return "meine-projekte";
    }
}
