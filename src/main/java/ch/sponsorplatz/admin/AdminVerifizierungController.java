package ch.sponsorplatz.admin;

import java.util.List;
import java.util.UUID;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import ch.sponsorplatz.organisation.Organisation;
import ch.sponsorplatz.organisation.OrganisationService;
import ch.sponsorplatz.organisation.OrganisationView;
import ch.sponsorplatz.shared.config.ModelAttributeNames;

/**
 * Admin-Bereich: Verifizierungs-Queue für PENDING-Organisationen.
 * Schutz deklarativ via {@code @PreAuthorize("hasRole('PLATFORM_ADMIN')")} —
 * die Rolle wird vom {@code SponsorplatzUserDetailsService} aus
 * {@code AppUser.platformRolle}
 * als {@code ROLE_PLATFORM_ADMIN}-GrantedAuthority gesetzt; kein DB-Roundtrip
 * pro Request.
 */
@Controller
@RequestMapping("/admin/verifizierungen")
@PreAuthorize("hasRole('PLATFORM_ADMIN')")
public class AdminVerifizierungController {

    private final OrganisationService organisationService;

    public AdminVerifizierungController(OrganisationService organisationService) {
        this.organisationService = organisationService;
    }

    @GetMapping
    public String liste(Model model) {
        List<Organisation> pendingOrgs = organisationService.findePending();
        model.addAttribute(ModelAttributeNames.AKTIVE_SEITE, "admin");
        model.addAttribute("pendingOrgs", OrganisationView.von(pendingOrgs));
        return "admin/verifizierungen";
    }

    @PostMapping("/{id}/verifizieren")
    public String verifizieren(@PathVariable UUID id) {
        organisationService.verifiziere(id);
        return "redirect:/admin/verifizierungen";
    }

    @PostMapping("/{id}/ablehnen")
    public String ablehnen(@PathVariable UUID id) {
        organisationService.suspendiere(id);
        return "redirect:/admin/verifizierungen";
    }
}
