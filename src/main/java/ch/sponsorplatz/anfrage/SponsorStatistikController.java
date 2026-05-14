package ch.sponsorplatz.anfrage;

import ch.sponsorplatz.shared.config.ModelAttributeNames;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Statistik-Dashboard (Phase 5.C) — beide Sichten in einem Endpoint
 * {@code /statistiken}. Der User sieht die zu seinen Org-Typen passende
 * Statistik:
 *
 * <ul>
 *   <li>VEREIN-Mitglied → Vereins-Statistik (Projekte, eingehende Anfragen,
 *       Vertrags-Einnahmen, Rechnungs-Status)</li>
 *   <li>UNTERNEHMEN-Mitglied → Sponsor-Statistik (Engagements, ausgehende
 *       Anfragen, Branchen-Verteilung, Rechnungs-Liquiditaet)</li>
 *   <li>Multi-Role-User (beide Typen) → beide Sektionen untereinander</li>
 *   <li>Weder noch → Empty-State mit Registrierungs-Hinweis</li>
 * </ul>
 *
 * <p>Vereins-User sehen ausdrücklich <b>nicht</b> die Sponsor-Statistik —
 * das war ein konkreter Bug-Report: Vereine bekamen die falsche Seite.
 */
@Controller
public class SponsorStatistikController {

    private final SponsorStatistikService sponsorStatistikService;
    private final VereinStatistikService vereinStatistikService;

    public SponsorStatistikController(SponsorStatistikService sponsorStatistikService,
                                       VereinStatistikService vereinStatistikService) {
        this.sponsorStatistikService = sponsorStatistikService;
        this.vereinStatistikService = vereinStatistikService;
    }

    @GetMapping("/statistiken")
    @PreAuthorize("isAuthenticated()")
    public String statistiken(Authentication auth, Model model) {
        SponsorStatistik sponsorStatistik = sponsorStatistikService.fuerUser(auth.getName());
        VereinStatistik vereinStatistik = vereinStatistikService.fuerUser(auth.getName());
        model.addAttribute(ModelAttributeNames.AKTIVE_SEITE, "statistiken");
        model.addAttribute("sponsorStatistik", sponsorStatistik);
        model.addAttribute("vereinStatistik", vereinStatistik);
        return "statistik";
    }
}
