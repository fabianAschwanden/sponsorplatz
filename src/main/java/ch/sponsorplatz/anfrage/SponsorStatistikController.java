package ch.sponsorplatz.anfrage;

import ch.sponsorplatz.shared.config.ModelAttributeNames;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Sponsor-Statistik-Dashboard (Phase 5.C). Sichtbar unter {@code /statistiken}
 * für eingeloggte User mit mindestens einer UNTERNEHMEN-Org-Mitgliedschaft.
 * Vereins-only-User landen auf einer Empty-Page mit Hinweis-Text.
 */
@Controller
public class SponsorStatistikController {

    private final SponsorStatistikService statistikService;

    public SponsorStatistikController(SponsorStatistikService statistikService) {
        this.statistikService = statistikService;
    }

    @GetMapping("/statistiken")
    @PreAuthorize("isAuthenticated()")
    public String statistiken(Authentication auth, Model model) {
        SponsorStatistik statistik = statistikService.fuerUser(auth.getName());
        model.addAttribute(ModelAttributeNames.AKTIVE_SEITE, "statistiken");
        model.addAttribute("statistik", statistik);
        return "sponsor-statistik";
    }
}
