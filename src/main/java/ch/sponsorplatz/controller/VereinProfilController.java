package ch.sponsorplatz.controller;

import ch.sponsorplatz.config.ModelAttributeNames;
import ch.sponsorplatz.dto.OrganisationView;
import ch.sponsorplatz.exception.NotFoundException;
import ch.sponsorplatz.model.Organisation;
import ch.sponsorplatz.model.Projekt;
import ch.sponsorplatz.model.Sichtbarkeit;
import ch.sponsorplatz.service.OrganisationService;
import ch.sponsorplatz.service.ProjektService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

/**
 * Öffentliche Vereinsprofile — ohne Login erreichbar.
 */
@Controller
@RequestMapping("/vereine")
public class VereinProfilController {

    private final OrganisationService orgService;
    private final ProjektService projektService;

    public VereinProfilController(OrganisationService orgService, ProjektService projektService) {
        this.orgService = orgService;
        this.projektService = projektService;
    }

    @GetMapping("/{slug}")
    public String profil(@PathVariable String slug, Model model) {
        Organisation org = orgService.findeNachSlug(slug)
                .orElseThrow(() -> new NotFoundException("Verein nicht gefunden: " + slug));

        List<Projekt> oeffentlicheProjekte = projektService.findeNachOrg(org.getId()).stream()
                .filter(p -> p.getSichtbarkeit() == Sichtbarkeit.OEFFENTLICH)
                .toList();

        model.addAttribute(ModelAttributeNames.AKTIVE_SEITE, "vereine");
        model.addAttribute("org", OrganisationView.von(org));
        model.addAttribute("projekte", oeffentlicheProjekte);
        return "verein-profil";
    }
}

