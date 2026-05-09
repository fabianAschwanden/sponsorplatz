package ch.sponsorplatz.anfrage;

import ch.sponsorplatz.benutzer.AppUserRepository;
import ch.sponsorplatz.organisation.MitgliedschaftRepository;
import ch.sponsorplatz.shared.config.ModelAttributeNames;
import ch.sponsorplatz.shared.exception.NotFoundException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.UUID;

/**
 * Persönliche Anfragen-Übersicht — aggregiert eingehende Anfragen
 * über alle Organisationen des eingeloggten Users.
 *
 * <p>Route: {@code /anfragen} (GET), Aktionen: annehmen/ablehnen (POST).</p>
 */
@Controller
@PreAuthorize("isAuthenticated()")
public class MeineAnfragenController {

    private final SponsoringAnfrageService anfrageService;
    private final AppUserRepository appUserRepository;
    private final MitgliedschaftRepository mitgliedschaftRepository;

    public MeineAnfragenController(SponsoringAnfrageService anfrageService,
                                   AppUserRepository appUserRepository,
                                   MitgliedschaftRepository mitgliedschaftRepository) {
        this.anfrageService = anfrageService;
        this.appUserRepository = appUserRepository;
        this.mitgliedschaftRepository = mitgliedschaftRepository;
    }

    @GetMapping("/anfragen")
    public String meineAnfragen(Authentication auth, Model model) {
        List<UUID> orgIds = ladeOrgIds(auth);
        List<AnfrageView> anfragen = AnfrageView.von(anfrageService.findeAlleEingehenden(orgIds));

        long offene = anfrageService.zaehleNeue(orgIds);

        model.addAttribute(ModelAttributeNames.AKTIVE_SEITE, "anfragen");
        model.addAttribute("anfragen", anfragen);
        model.addAttribute("anzahlOffene", offene);
        return "meine-anfragen";
    }

    @PostMapping("/anfragen/{anfrageId}/annehmen")
    public String annehmen(@PathVariable UUID anfrageId,
                           @RequestParam(required = false) String antwort,
                           RedirectAttributes redirect) {
        anfrageService.annehme(anfrageId, antwort);
        redirect.addFlashAttribute(ModelAttributeNames.ERFOLGS_MELDUNG, "Anfrage angenommen.");
        return "redirect:/anfragen";
    }

    @PostMapping("/anfragen/{anfrageId}/ablehnen")
    public String ablehnen(@PathVariable UUID anfrageId,
                           @RequestParam(required = false) String antwort,
                           RedirectAttributes redirect) {
        anfrageService.lehneAb(anfrageId, antwort);
        redirect.addFlashAttribute(ModelAttributeNames.ERFOLGS_MELDUNG, "Anfrage abgelehnt.");
        return "redirect:/anfragen";
    }

    private List<UUID> ladeOrgIds(Authentication auth) {
        return appUserRepository.findByEmail(auth.getName())
                .map(user -> mitgliedschaftRepository.findOrgIdsByUserId(user.getId()))
                .orElseThrow(() -> new NotFoundException("User nicht gefunden"));
    }
}

