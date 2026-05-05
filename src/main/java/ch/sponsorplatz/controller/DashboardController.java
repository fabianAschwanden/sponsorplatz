package ch.sponsorplatz.controller;

import ch.sponsorplatz.config.ModelAttributeNames;
import ch.sponsorplatz.dto.DashboardDaten;
import ch.sponsorplatz.service.DashboardService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Dashboard für angemeldete Benutzer — zeigt persönliche Übersicht
 * basierend auf den Mitgliedschaften des Users.
 *
 * <p>Controller bleibt bewusst dünn: alle View-Strings (Monat/KW) und Zähler
 * kommen aus {@link DashboardDaten} — der Service kümmert sich um beides
 * (M5-Fix: keine View-Logik mehr im Controller).</p>
 */
@Controller
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
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

        return "dashboard";
    }
}
