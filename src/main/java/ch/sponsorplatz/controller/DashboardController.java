package ch.sponsorplatz.controller;

import java.util.List;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import ch.sponsorplatz.shared.config.ModelAttributeNames;
import ch.sponsorplatz.dto.DashboardDaten;
import ch.sponsorplatz.dto.ProjektView;
import ch.sponsorplatz.service.AppUserService;
import ch.sponsorplatz.service.DashboardService;
import ch.sponsorplatz.service.MatchingService;

/**
 * Dashboard für angemeldete Benutzer — zeigt persönliche Übersicht
 * basierend auf den Mitgliedschaften des Users.
 */
@Controller
public class DashboardController {

    private final DashboardService dashboardService;
    private final MatchingService matchingService;
    private final AppUserService appUserService;

    public DashboardController(DashboardService dashboardService,
            MatchingService matchingService,
            AppUserService appUserService) {
        this.dashboardService = dashboardService;
        this.matchingService = matchingService;
        this.appUserService = appUserService;
    }

    @GetMapping("/dashboard")
    @PreAuthorize("isAuthenticated()")
    public String dashboard(Authentication auth, Model model) {
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
