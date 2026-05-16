package ch.sponsorplatz.anfrage;

import ch.sponsorplatz.shared.config.ModelAttributeNames;
import ch.sponsorplatz.organisation.OrganisationView;
import ch.sponsorplatz.shared.exception.NotFoundException;
import ch.sponsorplatz.organisation.AccessControl;
import ch.sponsorplatz.benutzer.AppUserService;
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

/**
 * Controller für die Inbox (Nachrichten-Thread) einer angenommenen Anfrage.
 */
@Controller
@RequestMapping("/organisationen/{orgSlug}/anfragen/{anfrageId}/nachrichten")
public class NachrichtController {

    private final NachrichtService nachrichtService;
    private final OrganisationService organisationService;
    private final SponsoringAnfrageService anfrageService;
    private final AppUserService appUserService;
    private final AccessControl accessControl;

    public NachrichtController(NachrichtService nachrichtService,
                               OrganisationService organisationService,
                               SponsoringAnfrageService anfrageService,
                               AppUserService appUserService,
                               AccessControl accessControl) {
        this.nachrichtService = nachrichtService;
        this.organisationService = organisationService;
        this.anfrageService = anfrageService;
        this.appUserService = appUserService;
        this.accessControl = accessControl;
    }

    @GetMapping
    public String thread(@PathVariable String orgSlug,
                         @PathVariable UUID anfrageId,
                         Authentication auth,
                         Model model) {
        pruefeEditRecht(orgSlug, auth);
        OrganisationView org = organisationService.findeViewNachSlug(orgSlug)
                .orElseThrow(() -> new NotFoundException("Organisation nicht gefunden: " + orgSlug));
        AnfrageView anfrage = anfrageService.findeViewNachId(anfrageId);

        model.addAttribute(ModelAttributeNames.AKTIVE_SEITE, "anfragen");
        model.addAttribute("org", org);
        model.addAttribute("anfrage", anfrage);
        model.addAttribute("nachrichten", NachrichtView.von(nachrichtService.findeNachAnfrage(anfrageId)));
        // aktuellerUserId fürs Template — Bubble-Style entscheidet per UUID-
        // Vergleich (Authentication.email matched nie gegen UUID).
        model.addAttribute("aktuellerUserId",
                appUserService.findeOptionalIdNachEmail(auth.getName()).orElse(null));
        return "anfrage/nachrichten-thread";
    }

    @PostMapping
    public String sende(@PathVariable String orgSlug,
                        @PathVariable UUID anfrageId,
                        @RequestParam String text,
                        Authentication auth,
                        RedirectAttributes redirect) {
        pruefeEditRecht(orgSlug, auth);

        UUID userId = appUserService.findeIdNachEmail(auth.getName());

        try {
            nachrichtService.sendeNachricht(anfrageId, userId, text);
            redirect.addFlashAttribute(ModelAttributeNames.ERFOLGS_MELDUNG, "Nachricht gesendet.");
        } catch (IllegalArgumentException ex) {
            redirect.addFlashAttribute(ModelAttributeNames.FEHLERMELDUNG, ex.getMessage());
        }

        return "redirect:/organisationen/" + orgSlug + "/anfragen/" + anfrageId + "/nachrichten";
    }

    private void pruefeEditRecht(String slug, Authentication auth) {
        if (!accessControl.kannOrgEditierenNachSlug(slug, auth)) {
            throw new AccessDeniedException("Keine Berechtigung für Nachrichten von Org: " + slug);
        }
    }
}
