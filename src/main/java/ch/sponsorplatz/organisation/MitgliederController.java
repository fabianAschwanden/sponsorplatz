package ch.sponsorplatz.organisation;

import ch.sponsorplatz.shared.config.ModelAttributeNames;
import ch.sponsorplatz.shared.exception.NotFoundException;
import ch.sponsorplatz.benutzer.AppUser;
import ch.sponsorplatz.benutzer.AppUserService;
import ch.sponsorplatz.einladung.EinladungsService;
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
import java.util.Optional;
import java.util.UUID;

@Controller
@RequestMapping("/organisationen/{slug}/mitglieder")
public class MitgliederController {

    private final OrganisationService organisationService;
    private final MitgliedschaftService mitgliedschaftService;
    private final AppUserService appUserService;
    private final EinladungsService einladungsService;
    private final AccessControl accessControl;

    public MitgliederController(OrganisationService organisationService,
                                MitgliedschaftService mitgliedschaftService,
                                AppUserService appUserService,
                                EinladungsService einladungsService,
                                AccessControl accessControl) {
        this.organisationService = organisationService;
        this.mitgliedschaftService = mitgliedschaftService;
        this.appUserService = appUserService;
        this.einladungsService = einladungsService;
        this.accessControl = accessControl;
    }

    @GetMapping
    public String liste(@PathVariable String slug, Authentication auth, Model model) {
        if (!accessControl.kannOrgEditierenNachSlug(slug, auth)) {
            throw new AccessDeniedException("Keine Edit-Berechtigung für Org: " + slug);
        }
        Organisation org = findeOrgOderWirf(slug);
        List<Mitgliedschaft> mitglieder = mitgliedschaftService.findeNachOrg(org.getId());
        model.addAttribute(ModelAttributeNames.AKTIVE_SEITE, "organisationen");
        model.addAttribute("org", OrganisationView.von(org));
        model.addAttribute("mitglieder", MitgliedView.von(mitglieder));
        return "mitglieder";
    }

    @PostMapping("/hinzufuegen")
    public String hinzufuegen(@PathVariable String slug,
                              @RequestParam String email,
                              @RequestParam Rolle rolle,
                              Authentication auth,
                              RedirectAttributes redirect) {
        if (!accessControl.kannOrgVerwaltenNachSlug(slug, auth)) {
            throw new AccessDeniedException("Keine Verwalten-Berechtigung für Org: " + slug);
        }
        Organisation org = findeOrgOderWirf(slug);
        String normalisierteEmail = email.trim().toLowerCase();
        Optional<AppUser> user = appUserService.findeNachEmail(normalisierteEmail);

        // Existiert kein User mit dieser E-Mail → Einladung erstellen.
        // Der EinladungsMailListener verschickt den Link nach AFTER_COMMIT;
        // der Eingeladene landet via /einladung/annehmen auf /registrieren mit
        // pre-filled E-Mail.
        if (user.isEmpty()) {
            UUID eingeladenVonId = appUserService.findeIdNachEmail(auth.getName());
            try {
                einladungsService.erstelleEinladung(org.getId(), normalisierteEmail, rolle, eingeladenVonId);
                redirect.addFlashAttribute(ModelAttributeNames.ERFOLGS_MELDUNG,
                        "Einladung an \"" + normalisierteEmail + "\" wurde erstellt.");
            } catch (IllegalArgumentException ex) {
                redirect.addFlashAttribute(ModelAttributeNames.FEHLERMELDUNG, ex.getMessage());
            }
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
                            Authentication auth,
                            RedirectAttributes redirect) {
        if (!accessControl.kannOrgVerwaltenNachSlug(slug, auth)) {
            throw new AccessDeniedException("Keine Verwalten-Berechtigung für Org: " + slug);
        }
        findeOrgOderWirf(slug);
        mitgliedschaftService.entferne(mitgliedschaftId);
        redirect.addFlashAttribute(ModelAttributeNames.ERFOLGS_MELDUNG, "Mitglied entfernt.");
        return "redirect:/organisationen/" + slug + "/mitglieder";
    }

    private Organisation findeOrgOderWirf(String slug) {
        return organisationService.findeNachSlug(slug)
                .orElseThrow(() -> new NotFoundException("Organisation nicht gefunden: " + slug));
    }
}
