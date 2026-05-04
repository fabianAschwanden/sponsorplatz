package ch.sponsorplatz.controller;

import ch.sponsorplatz.config.ModelAttributeNames;
import ch.sponsorplatz.model.AppUser;
import ch.sponsorplatz.model.Mitgliedschaft;
import ch.sponsorplatz.model.Organisation;
import ch.sponsorplatz.model.Rolle;
import ch.sponsorplatz.service.AppUserService;
import ch.sponsorplatz.service.MitgliedschaftService;
import ch.sponsorplatz.service.OrganisationService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Controller
@RequestMapping("/organisationen/{slug}/mitglieder")
public class MitgliederController {

    private final OrganisationService organisationService;
    private final MitgliedschaftService mitgliedschaftService;
    private final AppUserService appUserService;

    public MitgliederController(OrganisationService organisationService,
                                MitgliedschaftService mitgliedschaftService,
                                AppUserService appUserService) {
        this.organisationService = organisationService;
        this.mitgliedschaftService = mitgliedschaftService;
        this.appUserService = appUserService;
    }

    @GetMapping
    public String liste(@PathVariable String slug, Model model) {
        Organisation org = findeOrgOderWirf(slug);
        List<Mitgliedschaft> mitglieder = mitgliedschaftService.findeNachOrg(org.getId());
        model.addAttribute(ModelAttributeNames.AKTIVE_SEITE, "organisationen");
        model.addAttribute("org", org);
        model.addAttribute("mitglieder", mitglieder);
        return "mitglieder";
    }

    @PostMapping("/hinzufuegen")
    public String hinzufuegen(@PathVariable String slug,
                              @RequestParam String email,
                              @RequestParam Rolle rolle,
                              RedirectAttributes redirect) {
        Organisation org = findeOrgOderWirf(slug);
        Optional<AppUser> user = appUserService.findeNachEmail(email.trim().toLowerCase());
        if (user.isEmpty()) {
            redirect.addFlashAttribute(ModelAttributeNames.FEHLERMELDUNG,
                    "Kein Benutzer mit E-Mail \"" + email + "\" gefunden.");
            return "redirect:/organisationen/" + slug + "/mitglieder";
        }
        try {
            mitgliedschaftService.fuegeHinzu(org.getId(), user.get().getId(), rolle, null);
            redirect.addFlashAttribute(ModelAttributeNames.ERFOLGS_MELDUNG,
                    user.get().getAnzeigename() + " als " + rolle + " hinzugefuegt.");
        } catch (IllegalStateException ex) {
            redirect.addFlashAttribute(ModelAttributeNames.FEHLERMELDUNG, ex.getMessage());
        }
        return "redirect:/organisationen/" + slug + "/mitglieder";
    }

    @PostMapping("/{mitgliedschaftId}/entfernen")
    public String entfernen(@PathVariable String slug,
                            @PathVariable UUID mitgliedschaftId,
                            RedirectAttributes redirect) {
        findeOrgOderWirf(slug);
        mitgliedschaftService.entferne(mitgliedschaftId);
        redirect.addFlashAttribute(ModelAttributeNames.ERFOLGS_MELDUNG, "Mitglied entfernt.");
        return "redirect:/organisationen/" + slug + "/mitglieder";
    }

    private Organisation findeOrgOderWirf(String slug) {
        return organisationService.findeNachSlug(slug)
                .orElseThrow(() -> new IllegalArgumentException("Organisation nicht gefunden: " + slug));
    }
}

