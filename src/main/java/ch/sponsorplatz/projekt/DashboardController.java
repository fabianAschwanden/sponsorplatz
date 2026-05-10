package ch.sponsorplatz.projekt;

import java.util.List;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import ch.sponsorplatz.shared.config.ModelAttributeNames;
import ch.sponsorplatz.benutzer.AppUserRepository;
import ch.sponsorplatz.benutzer.AppUserService;
import ch.sponsorplatz.organisation.MitgliedschaftRepository;

/**
 * Dashboard für angemeldete Benutzer — zeigt persönliche Übersicht
 * basierend auf den Mitgliedschaften des Users.
 * Leitet auf /onboarding um, falls der User noch keiner Org angehört.
 */
@Controller
public class DashboardController {

    private final DashboardService dashboardService;
    private final MatchingService matchingService;
    private final AppUserService appUserService;
    private final AppUserRepository appUserRepository;
    private final MitgliedschaftRepository mitgliedschaftRepository;

    public DashboardController(DashboardService dashboardService,
            MatchingService matchingService,
            AppUserService appUserService,
            AppUserRepository appUserRepository,
            MitgliedschaftRepository mitgliedschaftRepository) {
        this.dashboardService = dashboardService;
        this.matchingService = matchingService;
        this.appUserService = appUserService;
        this.appUserRepository = appUserRepository;
        this.mitgliedschaftRepository = mitgliedschaftRepository;
    }

    @GetMapping("/dashboard")
    @PreAuthorize("isAuthenticated()")
    public String dashboard(Authentication auth, Model model) {
        // Onboarding-Redirect: User ohne Mitgliedschaften → Wizard
        boolean hatOrgs = appUserRepository.findByEmail(auth.getName())
                .map(user -> !mitgliedschaftRepository.findOrgIdsByUserId(user.getId()).isEmpty())
                .orElse(false);
        if (!hatOrgs) {
            return "redirect:/onboarding";
        }

        DashboardDaten daten = dashboardService.ladeDashboardDaten(auth.getName());

        model.addAttribute(ModelAttributeNames.AKTIVE_SEITE, "dashboard");
        model.addAttribute("aktuellerMonat", daten.aktuellerMonat());
        model.addAttribute("aktuelleKw", daten.aktuelleKw());
        model.addAttribute("anzahlOrganisationen", daten.anzahlOrganisationen());
        model.addAttribute("anzahlProjekte", daten.anzahlProjekte());
        model.addAttribute("anzahlAnfragen", daten.anzahlAnfragen());
        model.addAttribute("anzahlOffeneAnfragen", daten.anzahlOffeneAnfragen());

        // Matching-Empfehlungen
        List<ProjektView> empfehlungen = appUserService.findeNachEmail(auth.getName())
                .map(user -> matchingService.findeEmpfehlungen(user.getId()))
                .orElse(List.of())
                .stream()
                .map(ProjektView::von)
                .toList();
        model.addAttribute("empfehlungen", empfehlungen);

        return "dashboard";
    }
}
