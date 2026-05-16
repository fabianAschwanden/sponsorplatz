package ch.sponsorplatz.anfrage;

import ch.sponsorplatz.benutzer.AppUserService;
import ch.sponsorplatz.organisation.AccessControl;
import ch.sponsorplatz.organisation.MitgliedschaftService;
import ch.sponsorplatz.organisation.OrgTyp;
import ch.sponsorplatz.organisation.OrganisationService;
import ch.sponsorplatz.organisation.OrganisationView;
import ch.sponsorplatz.projekt.SponsoringPaketService;
import ch.sponsorplatz.shared.config.ModelAttributeNames;
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

    private final SponsoringAnfrageService anfrageService;
    private final AppUserService appUserService;
    private final MitgliedschaftService mitgliedschaftService;
    private final AccessControl accessControl;
    private final SponsoringPaketService paketService;
    private final OrganisationService organisationService;

    public MeineAnfragenController(SponsoringAnfrageService anfrageService,
                                   AppUserService appUserService,
                                   MitgliedschaftService mitgliedschaftService,
                                   AccessControl accessControl,
                                   SponsoringPaketService paketService,
                                   OrganisationService organisationService) {
        this.anfrageService = anfrageService;
        this.appUserService = appUserService;
        this.mitgliedschaftService = mitgliedschaftService;
        this.accessControl = accessControl;
        this.paketService = paketService;
        this.organisationService = organisationService;
    }

    @GetMapping("/anfragen")
    public String meineAnfragen(Authentication auth, Model model) {
        UUID userId = appUserService.findeIdNachEmail(auth.getName());

        MitgliedschaftService.AnfragenSeitenDaten daten =
                mitgliedschaftService.findeAnfragenSeitenDaten(userId);

        List<AnfrageView> eingehend = anfrageService.findeAlleEingehendenViews(daten.alleOrgIds());
        long offene = anfrageService.zaehleNeue(daten.alleOrgIds());

        boolean istVereinsMitglied = !daten.vereinsOrgIds().isEmpty();
        List<AnfrageView> meineAusgehend = istVereinsMitglied
                ? anfrageService.findeAusgehendeVonUserViews(userId)
                : List.of();
        List<AnfrageView> orgAusgehend = istVereinsMitglied
                ? anfrageService.findeAusgehendeMeinerOrgsOhneUserViews(daten.vereinsOrgIds(), userId)
                : List.of();

        model.addAttribute(ModelAttributeNames.AKTIVE_SEITE, "anfragen");
        model.addAttribute("anfragen", eingehend);
        model.addAttribute("meineAusgehendeAnfragen", meineAusgehend);
        model.addAttribute("orgAusgehendeAnfragen", orgAusgehend);
        model.addAttribute("meineOrgNamen", daten.vereinsOrgNamen());
        model.addAttribute("anzahlOffene", offene);
        model.addAttribute("kannKontaktanfrageStellen", istVereinsMitglied);
        return "anfrage/meine-anfragen";
    }

    /**
     * Sponsor-Picker für die Kontakt-Anfrage. Sichtbar nur für User mit
     * mindestens einer VEREIN-Mitgliedschaft (Edit-Rolle). Zeigt aktive
     * Sponsor-Orgs (UNTERNEHMEN, VERIFIED/ACTIVE).
     */
    @GetMapping("/anfragen/neu-kontakt")
    public String kontaktFormular(Authentication auth, Model model) {
        UUID userId = appUserService.findeIdNachEmail(auth.getName());
        List<OrganisationView> meineVereinsOrgs =
                mitgliedschaftService.findeMeineVereinsOrgViews(userId);
        if (meineVereinsOrgs.isEmpty()) {
            throw new AccessDeniedException(
                    "Kontakt-Anfragen können nur Vereins-Mitglieder mit Edit-Recht stellen.");
        }

        List<OrganisationView> sponsoren = organisationService.findeAktiveSponsorenAlsViews();
        AppUserService.KontaktSnapshot kontakt =
                appUserService.findeKontaktSnapshotNachEmail(auth.getName());

        KontaktAnfrageFormDto form = new KontaktAnfrageFormDto();
        form.setKontaktName(kontakt.anzeigename());
        form.setKontaktEmail(kontakt.email());

        model.addAttribute(ModelAttributeNames.AKTIVE_SEITE, "anfragen");
        model.addAttribute("kontaktForm", form);
        model.addAttribute("meineOrgs", meineVereinsOrgs);
        model.addAttribute("sponsoren", sponsoren);
        return "anfrage/anfrage-kontakt-neu";
    }

    @PostMapping("/anfragen/kontakt-erstellen")
    public String kontaktErstellen(@Valid @ModelAttribute("kontaktForm") KontaktAnfrageFormDto form,
                                   BindingResult binding,
                                   Authentication auth,
                                   Model model,
                                   RedirectAttributes redirect) {
        // Berechtigung: User muss Edit-Recht auf der gewählten anfragenderOrg haben
        // UND die anfragenderOrg muss VEREIN sein (Sponsoren dürfen nicht selber
        // Kontakt-Anfragen stellen — siehe Anforderung).
        OrganisationService.OrgInfo anfragenderInfo =
                organisationService.findeInfoNachId(form.getAnfragenderOrgId());
        if (anfragenderInfo.typ() != OrgTyp.VEREIN) {
            throw new AccessDeniedException("Kontakt-Anfragen sind nur für Vereins-Orgs.");
        }
        if (!accessControl.kannOrgEditieren(anfragenderInfo.id(), auth)) {
            throw new AccessDeniedException("Keine Berechtigung für die gewählte Org.");
        }

        OrganisationService.OrgInfo empfaengerInfo =
                organisationService.findeInfoNachId(form.getEmpfaengerOrgId());
        if (empfaengerInfo.typ() != OrgTyp.UNTERNEHMEN) {
            binding.reject("empfaengerOrg.typ", "Empfänger muss ein Unternehmen sein.");
        }
        if (anfragenderInfo.id().equals(empfaengerInfo.id())) {
            binding.reject("empfaengerOrg.self", "Eigene Org kann nicht angefragt werden.");
        }

        if (binding.hasErrors()) {
            UUID userId = appUserService.findeIdNachEmail(auth.getName());
            model.addAttribute(ModelAttributeNames.AKTIVE_SEITE, "anfragen");
            model.addAttribute("meineOrgs", mitgliedschaftService.findeMeineVereinsOrgViews(userId));
            model.addAttribute("sponsoren", organisationService.findeAktiveSponsorenAlsViews());
            return "anfrage/anfrage-kontakt-neu";
        }

        UUID erstelltVonUserId = appUserService.findeIdNachEmail(auth.getName());
        anfrageService.erstelleKontaktAnfrageNachIds(
                anfragenderInfo.id(), empfaengerInfo.id(),
                form.getBetreff(), form.getNachricht(),
                form.getKontaktName(), form.getKontaktEmail(),
                form.getWunschBetragChf(),
                erstelltVonUserId);

        redirect.addFlashAttribute(ModelAttributeNames.ERFOLGS_MELDUNG,
                "Kontakt-Anfrage an " + empfaengerInfo.name() + " wurde gesendet.");
        return "redirect:/anfragen";
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
        SponsoringPaketService.PaketAnfrageInfo paketInfo =
                paketService.findePaketAnfrageInfo(paketId);

        UUID userId = appUserService.findeIdNachEmail(auth.getName());
        List<OrganisationView> meineOrgs =
                mitgliedschaftService.findeMeineOrgsAusser(userId, paketInfo.empfaengerOrg().id());
        if (meineOrgs.isEmpty()) {
            throw new AccessDeniedException(
                    "Du brauchst eine eigene Org (mit Edit-Recht), um eine Anfrage zu stellen — "
                            + "und die Org darf nicht der Empfänger sein.");
        }

        AppUserService.KontaktSnapshot kontakt =
                appUserService.findeKontaktSnapshotNachEmail(auth.getName());

        AnfrageFormDto form = new AnfrageFormDto();
        form.setPaketId(paketId);
        form.setKontaktName(kontakt.anzeigename());
        form.setKontaktEmail(kontakt.email());

        model.addAttribute(ModelAttributeNames.AKTIVE_SEITE, "anfragen");
        model.addAttribute("anfrageForm", form);
        model.addAttribute("paketName", paketInfo.paketName());
        model.addAttribute("paketPreisChf", paketInfo.paketPreisChf());
        model.addAttribute("projektName", paketInfo.projektName());
        model.addAttribute("projektSlug", paketInfo.projektSlug());
        model.addAttribute("empfaengerOrg", paketInfo.empfaengerOrg());
        model.addAttribute("meineOrgs", meineOrgs);
        return "anfrage/anfrage-neu";
    }

    @PostMapping("/anfragen/erstellen")
    public String erstellen(@Valid @ModelAttribute("anfrageForm") AnfrageFormDto form,
                            BindingResult binding,
                            @RequestParam UUID anfragenderOrgId,
                            Authentication auth,
                            Model model,
                            RedirectAttributes redirect) {
        SponsoringPaketService.PaketAnfrageInfo paketInfo =
                paketService.findePaketAnfrageInfo(form.getPaketId());
        UUID empfaengerOrgId = paketInfo.empfaengerOrg().id();

        if (anfragenderOrgId.equals(empfaengerOrgId)) {
            binding.reject("anfragenderOrg.self",
                    "Du kannst keine Anfrage an deine eigene Organisation stellen.");
        }
        if (!accessControl.kannOrgEditieren(anfragenderOrgId, auth)) {
            throw new AccessDeniedException(
                    "Keine Berechtigung — du bist kein Editor/Owner dieser Org.");
        }

        if (binding.hasErrors()) {
            UUID userId = appUserService.findeIdNachEmail(auth.getName());
            model.addAttribute(ModelAttributeNames.AKTIVE_SEITE, "anfragen");
            model.addAttribute("paketName", paketInfo.paketName());
            model.addAttribute("paketPreisChf", paketInfo.paketPreisChf());
            model.addAttribute("projektName", paketInfo.projektName());
            model.addAttribute("projektSlug", paketInfo.projektSlug());
            model.addAttribute("empfaengerOrg", paketInfo.empfaengerOrg());
            model.addAttribute("meineOrgs",
                    mitgliedschaftService.findeMeineOrgsAusser(userId, empfaengerOrgId));
            return "anfrage/anfrage-neu";
        }

        UUID erstelltVonUserId = appUserService.findeIdNachEmail(auth.getName());
        anfrageService.erstelleNachIds(form.getPaketId(), anfragenderOrgId,
                form.getNachricht(), form.getKontaktName(), form.getKontaktEmail(),
                erstelltVonUserId);

        redirect.addFlashAttribute(ModelAttributeNames.ERFOLGS_MELDUNG,
                "Anfrage an " + paketInfo.empfaengerOrg().name() + " wurde gesendet.");
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
}
