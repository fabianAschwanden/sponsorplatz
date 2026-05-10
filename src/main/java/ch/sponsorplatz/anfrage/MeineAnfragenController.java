package ch.sponsorplatz.anfrage;

import ch.sponsorplatz.benutzer.AppUser;
import ch.sponsorplatz.benutzer.AppUserRepository;
import ch.sponsorplatz.organisation.AccessControl;
import ch.sponsorplatz.organisation.Mitgliedschaft;
import ch.sponsorplatz.organisation.MitgliedschaftRepository;
import ch.sponsorplatz.organisation.Organisation;
import ch.sponsorplatz.organisation.OrganisationService;
import ch.sponsorplatz.organisation.OrganisationView;
import ch.sponsorplatz.organisation.Rolle;
import ch.sponsorplatz.projekt.SponsoringPaket;
import ch.sponsorplatz.projekt.SponsoringPaketService;
import ch.sponsorplatz.shared.config.ModelAttributeNames;
import ch.sponsorplatz.shared.exception.NotFoundException;
import jakarta.validation.Valid;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Persönliche Anfragen-Übersicht und Anfrage-Erstellung.
 *
 * <p>Routen:
 * <ul>
 *   <li>{@code GET  /anfragen} — Übersicht eingehender Anfragen über alle Orgs des Users</li>
 *   <li>{@code POST /anfragen/{id}/annehmen|ablehnen} — Status-Aktionen mit IDOR-Check</li>
 *   <li>{@code GET  /anfragen/neu?paketId=...} — Form zum Erstellen einer neuen Anfrage</li>
 *   <li>{@code POST /anfragen/erstellen} — speichert die Anfrage, prüft anfragenderOrg-Recht</li>
 * </ul>
 *
 * <p><b>Authorization:</b>
 * <ul>
 *   <li>annehmen/ablehnen: Edit-Recht auf der Empfänger-Org der Anfrage (IDOR-Schutz).</li>
 *   <li>erstellen: Edit-Recht auf der gewählten anfragenden Org. Empfänger-Org wird vom
 *       Paket abgeleitet — der Client kann die Empfänger-Identität nicht manipulieren.</li>
 * </ul>
 */
@Controller
@PreAuthorize("isAuthenticated()")
public class MeineAnfragenController {

    private static final Set<Rolle> EDIT_ROLLEN = Set.of(Rolle.ORG_OWNER, Rolle.ORG_EDITOR);

    private final SponsoringAnfrageService anfrageService;
    private final AppUserRepository appUserRepository;
    private final MitgliedschaftRepository mitgliedschaftRepository;
    private final AccessControl accessControl;
    private final SponsoringPaketService paketService;
    private final OrganisationService organisationService;

    public MeineAnfragenController(SponsoringAnfrageService anfrageService,
                                   AppUserRepository appUserRepository,
                                   MitgliedschaftRepository mitgliedschaftRepository,
                                   AccessControl accessControl,
                                   SponsoringPaketService paketService,
                                   OrganisationService organisationService) {
        this.anfrageService = anfrageService;
        this.appUserRepository = appUserRepository;
        this.mitgliedschaftRepository = mitgliedschaftRepository;
        this.accessControl = accessControl;
        this.paketService = paketService;
        this.organisationService = organisationService;
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
                           Authentication auth,
                           RedirectAttributes redirect) {
        pruefeRechtAufAnfrage(anfrageId, auth);
        anfrageService.annehme(anfrageId, antwort);
        redirect.addFlashAttribute(ModelAttributeNames.ERFOLGS_MELDUNG, "Anfrage angenommen.");
        return "redirect:/anfragen";
    }

    @PostMapping("/anfragen/{anfrageId}/ablehnen")
    public String ablehnen(@PathVariable UUID anfrageId,
                           @RequestParam(required = false) String antwort,
                           Authentication auth,
                           RedirectAttributes redirect) {
        pruefeRechtAufAnfrage(anfrageId, auth);
        anfrageService.lehneAb(anfrageId, antwort);
        redirect.addFlashAttribute(ModelAttributeNames.ERFOLGS_MELDUNG, "Anfrage abgelehnt.");
        return "redirect:/anfragen";
    }

    @GetMapping("/anfragen/neu")
    public String formular(@RequestParam UUID paketId,
                           Authentication auth,
                           Model model) {
        SponsoringPaket paket = paketService.findeNachIdMitProjektUndOrg(paketId)
                .orElseThrow(() -> new NotFoundException("Paket nicht gefunden: " + paketId));
        Organisation empfaengerOrg = paket.getProjekt().getOrg();

        List<OrganisationView> meineOrgs = ladeAnfragerOrgs(auth, empfaengerOrg.getId());
        if (meineOrgs.isEmpty()) {
            throw new AccessDeniedException(
                    "Du brauchst eine eigene Org (mit Edit-Recht), um eine Anfrage zu stellen — "
                            + "und die Org darf nicht der Empfänger sein.");
        }

        AppUser user = appUserRepository.findByEmail(auth.getName())
                .orElseThrow(() -> new NotFoundException("User nicht gefunden"));

        AnfrageFormDto form = new AnfrageFormDto();
        form.setPaketId(paketId);
        form.setKontaktName(user.getAnzeigename());
        form.setKontaktEmail(user.getEmail());

        model.addAttribute(ModelAttributeNames.AKTIVE_SEITE, "anfragen");
        model.addAttribute("anfrageForm", form);
        model.addAttribute("paketName", paket.getName());
        model.addAttribute("paketPreisChf", paket.getPreisChf());
        model.addAttribute("projektName", paket.getProjekt().getName());
        model.addAttribute("projektSlug", paket.getProjekt().getSlug());
        model.addAttribute("empfaengerOrg", OrganisationView.von(empfaengerOrg));
        model.addAttribute("meineOrgs", meineOrgs);
        return "anfrage-neu";
    }

    @PostMapping("/anfragen/erstellen")
    public String erstellen(@Valid @ModelAttribute("anfrageForm") AnfrageFormDto form,
                            BindingResult binding,
                            @RequestParam UUID anfragenderOrgId,
                            Authentication auth,
                            Model model,
                            RedirectAttributes redirect) {
        SponsoringPaket paket = paketService.findeNachIdMitProjektUndOrg(form.getPaketId())
                .orElseThrow(() -> new NotFoundException("Paket nicht gefunden: " + form.getPaketId()));
        Organisation empfaengerOrg = paket.getProjekt().getOrg();

        if (anfragenderOrgId.equals(empfaengerOrg.getId())) {
            binding.reject("anfragenderOrg.self",
                    "Du kannst keine Anfrage an deine eigene Organisation stellen.");
        }
        if (!accessControl.kannOrgEditieren(anfragenderOrgId, auth)) {
            throw new AccessDeniedException(
                    "Keine Berechtigung — du bist kein Editor/Owner dieser Org.");
        }

        if (binding.hasErrors()) {
            model.addAttribute(ModelAttributeNames.AKTIVE_SEITE, "anfragen");
            model.addAttribute("paketName", paket.getName());
            model.addAttribute("paketPreisChf", paket.getPreisChf());
            model.addAttribute("projektName", paket.getProjekt().getName());
            model.addAttribute("projektSlug", paket.getProjekt().getSlug());
            model.addAttribute("empfaengerOrg", OrganisationView.von(empfaengerOrg));
            model.addAttribute("meineOrgs", ladeAnfragerOrgs(auth, empfaengerOrg.getId()));
            return "anfrage-neu";
        }

        Organisation anfragenderOrg = organisationService.findeNachId(anfragenderOrgId)
                .orElseThrow(() -> new NotFoundException("Org nicht gefunden: " + anfragenderOrgId));

        anfrageService.erstelle(paket, anfragenderOrg, empfaengerOrg,
                form.getNachricht(), form.getKontaktName(), form.getKontaktEmail());

        redirect.addFlashAttribute(ModelAttributeNames.ERFOLGS_MELDUNG,
                "Anfrage an " + empfaengerOrg.getName() + " wurde gesendet.");
        return "redirect:/anfragen";
    }

    /**
     * IDOR-Schutz: prüft dass der eingeloggte User auf der Empfänger-Org
     * der Anfrage Edit-Recht hat. Ohne das könnte jeder die UUID raten und
     * Anfragen für fremde Orgs annehmen/ablehnen.
     */
    private void pruefeRechtAufAnfrage(UUID anfrageId, Authentication auth) {
        UUID empfaengerOrgId = anfrageService.findeEmpfaengerOrgId(anfrageId);
        if (!accessControl.kannOrgEditieren(empfaengerOrgId, auth)) {
            throw new AccessDeniedException(
                    "Keine Berechtigung — Anfrage gehört zu einer Org, die du nicht editierst");
        }
    }

    private List<UUID> ladeOrgIds(Authentication auth) {
        return appUserRepository.findByEmail(auth.getName())
                .map(user -> mitgliedschaftRepository.findOrgIdsByUserId(user.getId()))
                .orElseThrow(() -> new NotFoundException("User nicht gefunden"));
    }

    /**
     * Lädt die Orgs des Users mit Edit-Recht — abzüglich der angegebenen
     * Empfänger-Org (kein Self-Anfragen). Die Org wird per JOIN FETCH gezogen,
     * damit das Template Name/Slug ohne LazyInit anzeigen kann.
     */
    private List<OrganisationView> ladeAnfragerOrgs(Authentication auth, UUID empfaengerOrgId) {
        AppUser user = appUserRepository.findByEmail(auth.getName())
                .orElseThrow(() -> new NotFoundException("User nicht gefunden"));
        List<Mitgliedschaft> mitgliedschaften =
                mitgliedschaftRepository.findByUserIdAndRolleInMitOrg(user.getId(), EDIT_ROLLEN);
        return mitgliedschaften.stream()
                .map(Mitgliedschaft::getOrg)
                .filter(o -> !o.getId().equals(empfaengerOrgId))
                .map(OrganisationView::von)
                .toList();
    }
}
