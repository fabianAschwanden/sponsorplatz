package ch.sponsorplatz.controller;

import ch.sponsorplatz.config.ModelAttributeNames;
import ch.sponsorplatz.dto.AnfrageView;
import ch.sponsorplatz.dto.OrganisationView;
import ch.sponsorplatz.exception.NotFoundException;
import ch.sponsorplatz.model.Organisation;
import ch.sponsorplatz.model.SponsoringAnfrage;
import ch.sponsorplatz.service.AccessControl;
import ch.sponsorplatz.service.OrganisationService;
import ch.sponsorplatz.service.SponsoringAnfrageService;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.UUID;

@Controller
@RequestMapping("/organisationen/{orgSlug}/anfragen")
public class AnfragenController {

    private final SponsoringAnfrageService anfrageService;
    private final OrganisationService orgService;
    private final AccessControl accessControl;

    public AnfragenController(SponsoringAnfrageService anfrageService,
                              OrganisationService orgService,
                              AccessControl accessControl) {
        this.anfrageService = anfrageService;
        this.orgService = orgService;
        this.accessControl = accessControl;
    }

    @GetMapping
    public String liste(@PathVariable String orgSlug, Authentication auth, Model model) {
        pruefeEditRecht(orgSlug, auth);
        Organisation org = ladeOrg(orgSlug);

        List<SponsoringAnfrage> eingehende = anfrageService.findeEingehende(org.getId());
        model.addAttribute(ModelAttributeNames.AKTIVE_SEITE, "anfragen");
        model.addAttribute("org", OrganisationView.von(org));
        model.addAttribute("anfragen", AnfrageView.von(eingehende));
        return "anfragen-liste";
    }

    @PostMapping("/{anfrageId}/annehmen")
    public String annehmen(@PathVariable String orgSlug,
                           @PathVariable UUID anfrageId,
                           @RequestParam(required = false) String antwort,
                           Authentication auth,
                           RedirectAttributes redirect) {
        pruefeEditRecht(orgSlug, auth);
        anfrageService.annehme(anfrageId, antwort);
        redirect.addFlashAttribute(ModelAttributeNames.ERFOLGS_MELDUNG, "Anfrage angenommen.");
        return "redirect:/organisationen/" + orgSlug + "/anfragen";
    }

    @PostMapping("/{anfrageId}/ablehnen")
    public String ablehnen(@PathVariable String orgSlug,
                           @PathVariable UUID anfrageId,
                           @RequestParam(required = false) String antwort,
                           Authentication auth,
                           RedirectAttributes redirect) {
        pruefeEditRecht(orgSlug, auth);
        anfrageService.lehneAb(anfrageId, antwort);
        redirect.addFlashAttribute(ModelAttributeNames.ERFOLGS_MELDUNG, "Anfrage abgelehnt.");
        return "redirect:/organisationen/" + orgSlug + "/anfragen";
    }

    private Organisation ladeOrg(String slug) {
        return orgService.findeNachSlug(slug)
                .orElseThrow(() -> new NotFoundException("Organisation nicht gefunden: " + slug));
    }

    private void pruefeEditRecht(String slug, Authentication auth) {
        if (!accessControl.kannOrgEditierenNachSlug(slug, auth)) {
            throw new AccessDeniedException("Keine Berechtigung für Anfragen von Org: " + slug);
        }
    }
}

