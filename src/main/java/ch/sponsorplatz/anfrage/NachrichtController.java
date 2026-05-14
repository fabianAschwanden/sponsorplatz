package ch.sponsorplatz.anfrage;

import ch.sponsorplatz.shared.config.ModelAttributeNames;
import ch.sponsorplatz.organisation.OrganisationView;
import ch.sponsorplatz.shared.exception.NotFoundException;
import ch.sponsorplatz.benutzer.AppUser;
import ch.sponsorplatz.organisation.Organisation;
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

import java.util.List;
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
        Organisation org = ladeOrg(orgSlug);
        SponsoringAnfrage anfrage = ladeAnfrage(anfrageId);

        List<Nachricht> nachrichten = nachrichtService.findeNachAnfrage(anfrageId);

        // aktuellerUserId fürs Template — damit der Bubble-Style (eigen vs.
        // eingehend) per UUID-Vergleich entscheiden kann, anstatt UUID gegen
        // Authentication-Email zu vergleichen (was nie matchte).
        UUID aktuellerUserId = appUserService.findeNachEmail(auth.getName())
                .map(AppUser::getId)
                .orElse(null);

        model.addAttribute(ModelAttributeNames.AKTIVE_SEITE, "anfragen");
        model.addAttribute("org", OrganisationView.von(org));
        model.addAttribute("anfrage", AnfrageView.von(anfrage));
        model.addAttribute("nachrichten", NachrichtView.von(nachrichten));
        model.addAttribute("aktuellerUserId", aktuellerUserId);
        return "nachrichten-thread";
    }

    @PostMapping
    public String sende(@PathVariable String orgSlug,
                        @PathVariable UUID anfrageId,
                        @RequestParam String text,
                        Authentication auth,
                        RedirectAttributes redirect) {
        pruefeEditRecht(orgSlug, auth);

        AppUser user = appUserService.findeNachEmail(auth.getName())
                .orElseThrow(() -> new NotFoundException("Benutzer nicht gefunden"));

        try {
            nachrichtService.sendeNachricht(anfrageId, user.getId(), text);
            redirect.addFlashAttribute(ModelAttributeNames.ERFOLGS_MELDUNG, "Nachricht gesendet.");
        } catch (IllegalArgumentException ex) {
            redirect.addFlashAttribute(ModelAttributeNames.FEHLERMELDUNG, ex.getMessage());
        }

        return "redirect:/organisationen/" + orgSlug + "/anfragen/" + anfrageId + "/nachrichten";
    }

    private Organisation ladeOrg(String slug) {
        return organisationService.findeNachSlug(slug)
                .orElseThrow(() -> new NotFoundException("Organisation nicht gefunden: " + slug));
    }

    private SponsoringAnfrage ladeAnfrage(UUID id) {
        return anfrageService.findeNachId(id);
    }

    private void pruefeEditRecht(String slug, Authentication auth) {
        if (!accessControl.kannOrgEditierenNachSlug(slug, auth)) {
            throw new AccessDeniedException("Keine Berechtigung für Nachrichten von Org: " + slug);
        }
    }
}

