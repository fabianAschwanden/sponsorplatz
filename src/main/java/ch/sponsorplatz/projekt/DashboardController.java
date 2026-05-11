package ch.sponsorplatz.projekt;

import java.util.List;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import ch.sponsorplatz.shared.config.ModelAttributeNames;
import ch.sponsorplatz.benutzer.AppUser;
import ch.sponsorplatz.benutzer.AppUserRepository;
import ch.sponsorplatz.benutzer.AppUserService;
import ch.sponsorplatz.benutzer.PlatformRolle;
import ch.sponsorplatz.einladung.EinladungsService;
import ch.sponsorplatz.organisation.MitgliedschaftRepository;

/**
 * Dashboard für angemeldete Benutzer — zeigt persönliche Übersicht
 * basierend auf den Mitgliedschaften des Users.
 * Leitet auf /onboarding um, falls der User noch keiner Org angehört
 * UND das Onboarding noch nicht gesehen hat. Plattform-Admins werden
 * nie in den Wizard geschickt.
 */
@Controller
public class DashboardController {

    private final DashboardService dashboardService;
    private final MatchingService matchingService;
    private final AppUserService appUserService;
    private final AppUserRepository appUserRepository;
    private final MitgliedschaftRepository mitgliedschaftRepository;
    private final EinladungsService einladungsService;

    public DashboardController(DashboardService dashboardService,
            MatchingService matchingService,
            AppUserService appUserService,
            AppUserRepository appUserRepository,
            MitgliedschaftRepository mitgliedschaftRepository,
            EinladungsService einladungsService) {
        this.dashboardService = dashboardService;
        this.matchingService = matchingService;
        this.appUserService = appUserService;
        this.appUserRepository = appUserRepository;
        this.mitgliedschaftRepository = mitgliedschaftRepository;
        this.einladungsService = einladungsService;
    }

    @GetMapping("/dashboard")
    @PreAuthorize("isAuthenticated()")
    public String dashboard(Authentication auth, Model model) {
        // Onboarding-Redirect nur einmal direkt nach der Registrierung:
        // - Plattform-Admins werden nie umgeleitet.
        // - User, die das Onboarding bereits gesehen haben, ebenfalls nicht
        //   (auch ohne Org bleiben sie dann auf dem Dashboard).
        AppUser user = appUserRepository.findByEmail(auth.getName()).orElse(null);
        if (user != null
                && user.getPlatformRolle() != PlatformRolle.PLATFORM_ADMIN
                && !user.isOnboardingGesehen()
                && mitgliedschaftRepository.findOrgIdsByUserId(user.getId()).isEmpty()) {
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
                .map(u -> matchingService.findeEmpfehlungen(u.getId()))
                .orElse(List.of())
                .stream()
                .map(ProjektView::von)
                .toList();
        model.addAttribute("empfehlungen", empfehlungen);

        // Offene Einladungen für die angemeldete E-Mail — frisch registrierte
        // User finden so ihre Einladungen ohne erneutes Anklicken des Mail-Links.
        model.addAttribute("offeneEinladungen",
                einladungsService.findeOffeneFuerEmail(auth.getName()));

        return "dashboard";
    }
}
