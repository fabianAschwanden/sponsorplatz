package ch.sponsorplatz.projekt;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import ch.sponsorplatz.shared.config.ModelAttributeNames;
import ch.sponsorplatz.benutzer.AppUserService;
import ch.sponsorplatz.benutzer.AppUserService.OnboardingSnapshot;
import ch.sponsorplatz.einladung.EinladungsService;
import ch.sponsorplatz.organisation.MitgliedschaftService;

/**
 * Dashboard für angemeldete Benutzer — zeigt persönliche Übersicht
 * basierend auf den Mitgliedschaften des Users.
 * Leitet auf /onboarding um, falls der User noch keiner Org angehört
 * UND das Onboarding noch nicht gesehen hat. Plattform-Admins werden
 * nie in den Wizard geschickt.
 */
@Controller
public class DashboardController {

    /**
     * Maximal-Anzahl Projekte in der „Aktive Projekte"-Sektion. Mehr passt
     * visuell nicht ins Grid (4-spaltig auf Desktop, 2 Zeilen).
     */
    private static final int MAX_AKTIVE_PROJEKTE_AUF_DASHBOARD = 8;

    private final DashboardService dashboardService;
    private final MatchingService matchingService;
    private final ProjektService projektService;
    private final AppUserService appUserService;
    private final MitgliedschaftService mitgliedschaftService;
    private final EinladungsService einladungsService;

    public DashboardController(DashboardService dashboardService,
            MatchingService matchingService,
            ProjektService projektService,
            AppUserService appUserService,
            MitgliedschaftService mitgliedschaftService,
            EinladungsService einladungsService) {
        this.dashboardService = dashboardService;
        this.matchingService = matchingService;
        this.projektService = projektService;
        this.appUserService = appUserService;
        this.mitgliedschaftService = mitgliedschaftService;
        this.einladungsService = einladungsService;
    }

    @GetMapping("/dashboard")
    @PreAuthorize("isAuthenticated()")
    public String dashboard(Authentication auth, Model model) {
        // Onboarding-Redirect nur einmal direkt nach der Registrierung:
        // - Plattform-Admins werden nie umgeleitet.
        // - User, die das Onboarding bereits gesehen haben, ebenfalls nicht
        //   (auch ohne Org bleiben sie dann auf dem Dashboard).
        Optional<OnboardingSnapshot> snapshot =
                appUserService.findeOnboardingSnapshotNachEmail(auth.getName());
        if (snapshot.isPresent()) {
            OnboardingSnapshot s = snapshot.get();
            if (!s.istPlatformAdmin()
                    && !s.onboardingGesehen()
                    && mitgliedschaftService.findeOrgIdsVonUser(s.userId()).isEmpty()) {
                return "redirect:/onboarding";
            }
        }

        DashboardDaten daten = dashboardService.ladeDashboardDaten(auth.getName());

        model.addAttribute(ModelAttributeNames.AKTIVE_SEITE, "dashboard");
        model.addAttribute("aktuellerMonat", daten.aktuellerMonat());
        model.addAttribute("aktuelleKw", daten.aktuelleKw());
        model.addAttribute("anzahlOrganisationen", daten.anzahlOrganisationen());
        model.addAttribute("anzahlProjekte", daten.anzahlProjekte());
        model.addAttribute("anzahlAnfragen", daten.anzahlAnfragen());
        model.addAttribute("anzahlOffeneAnfragen", daten.anzahlOffeneAnfragen());
        model.addAttribute("naechsteEvents", daten.naechsteEvents());

        // Aktive Projekte der eigenen Orgs — Top-N fürs Dashboard-Grid.
        // Für Org-lose User bleibt die Liste leer (Template versteckt die Sektion).
        List<ProjektView> meineProjekte = List.of();
        if (snapshot.isPresent()) {
            List<UUID> orgIds = mitgliedschaftService.findeOrgIdsVonUser(snapshot.get().userId());
            if (!orgIds.isEmpty()) {
                meineProjekte = projektService.findeViewsNachOrgIds(orgIds).stream()
                        .limit(MAX_AKTIVE_PROJEKTE_AUF_DASHBOARD)
                        .toList();
            }
        }
        model.addAttribute("meineProjekte", meineProjekte);

        // Matching-Empfehlungen
        List<ProjektView> empfehlungen = snapshot
                .map(s -> matchingService.findeEmpfehlungenAlsViews(s.userId()))
                .orElse(List.of());
        model.addAttribute("empfehlungen", empfehlungen);

        // Offene Einladungen für die angemeldete E-Mail — frisch registrierte
        // User finden so ihre Einladungen ohne erneutes Anklicken des Mail-Links.
        model.addAttribute("offeneEinladungen",
                einladungsService.findeOffeneFuerEmail(auth.getName()));

        return "dashboard";
    }
}
