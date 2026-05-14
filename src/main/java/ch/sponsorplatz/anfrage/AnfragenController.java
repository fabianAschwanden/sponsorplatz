package ch.sponsorplatz.anfrage;

import ch.sponsorplatz.shared.config.ModelAttributeNames;
import ch.sponsorplatz.organisation.OrganisationView;
import ch.sponsorplatz.shared.exception.NotFoundException;
import ch.sponsorplatz.organisation.AccessControl;
import ch.sponsorplatz.organisation.OrganisationService;
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
        OrganisationView org = orgService.findeViewNachSlug(orgSlug)
                .orElseThrow(() -> new NotFoundException("Organisation nicht gefunden: " + orgSlug));

        model.addAttribute(ModelAttributeNames.AKTIVE_SEITE, "anfragen");
        model.addAttribute("org", org);
        model.addAttribute("anfragen", anfrageService.findeEingehendeViews(org.id()));
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

    private void pruefeEditRecht(String slug, Authentication auth) {
        if (!accessControl.kannOrgEditierenNachSlug(slug, auth)) {
            throw new AccessDeniedException("Keine Berechtigung für Anfragen von Org: " + slug);
        }
    }
}
