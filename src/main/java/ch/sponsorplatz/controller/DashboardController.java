package ch.sponsorplatz.controller;

import ch.sponsorplatz.config.ModelAttributeNames;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.WeekFields;
import java.util.Locale;

/**
 * Dashboard für angemeldete Benutzer.
 *
 * Phase 0.x — UI-Skelett mit statischen Platzhaltern. Service-Aufrufe
 * werden in folgenden Iterationen verkabelt:
 *
 *   - {@code anzahlOrganisationen}     → OrganisationService.countByMitgliedschaft(authUser)
 *   - {@code anzahlProjekte}            → ProjektService.countByOrgsMitMitgliedschaft(authUser, sichtbarkeit=PUBLIC)
 *   - {@code anzahlOffeneAnfragen}      → SponsoringAnfrageService.countOffenFuer(authUser)  // Phase 4
 *   - {@code anzahlAnfragen}            → wie oben (Total)
 *   - {@code naechstesEvent1/2}         → ProjektService.findeNaechsteEvents(2)
 *
 * Sidebar-Markierung "aktiv" wird im Template anhand von {@code aktiveSeite}
 * gesetzt — analog zu den anderen Seiten.
 */
@Controller
public class DashboardController {

    private static final DateTimeFormatter MONAT_JAHR =
        DateTimeFormatter.ofPattern("MMMM yyyy", Locale.GERMAN);

    @GetMapping("/dashboard")
    @PreAuthorize("isAuthenticated()")
    public String dashboard(Model model) {
        LocalDate heute = LocalDate.now();
        int kw = heute.get(WeekFields.ISO.weekOfWeekBasedYear());

        model.addAttribute(ModelAttributeNames.AKTIVE_SEITE, "dashboard");
        model.addAttribute("aktuellerMonat", heute.format(MONAT_JAHR));
        model.addAttribute("aktuelleKw", "KW " + kw + " ▾");

        // TODO Phase 0.x: durch echte Service-Aufrufe ersetzen
        model.addAttribute("anzahlOrganisationen", 0);
        model.addAttribute("anzahlProjekte", 0);
        model.addAttribute("anzahlAnfragen", 0);
        model.addAttribute("anzahlOffeneAnfragen", 0);

        return "dashboard";
    }
}
