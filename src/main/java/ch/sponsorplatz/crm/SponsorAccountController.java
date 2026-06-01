package ch.sponsorplatz.crm;

import ch.sponsorplatz.organisation.Branche;
import ch.sponsorplatz.organisation.OrgTyp;
import ch.sponsorplatz.organisation.OrganisationFormDto;
import ch.sponsorplatz.organisation.OrganisationService;
import ch.sponsorplatz.organisation.OrganisationView;
import ch.sponsorplatz.shared.config.ModelAttributeNames;
import jakarta.validation.Valid;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.util.UUID;

/**
 * UI für die private Sponsor-CRM-Layer (ADR-0011). Alle Routen sind unter
 * {@code /crm/{sponsorSlug}} verankert; der {@link SponsorAccountService}
 * setzt die Mandanten-Isolation durch ({@code kannSponsorDatenSehen}) — der
 * Controller reicht nur den aufgelösten Sponsor-Org-Schlüssel durch.
 *
 * <p>View-DTO-Pflicht: Model bekommt ausschliesslich {@code SponsorAccountView}
 * + {@code OrganisationView}, nie Entities.
 */
@Controller
@RequestMapping("/crm/{sponsorSlug}")
public class SponsorAccountController {

    private final SponsorAccountService accountService;
    private final KontaktPersonService kontaktService;
    private final AktivitaetService aktivitaetService;
    private final RenewalService renewalService;
    private final CrmImportExportService importExportService;
    private final OrganisationService organisationService;

    public SponsorAccountController(SponsorAccountService accountService,
                                    KontaktPersonService kontaktService,
                                    AktivitaetService aktivitaetService,
                                    RenewalService renewalService,
                                    CrmImportExportService importExportService,
                                    OrganisationService organisationService) {
        this.accountService = accountService;
        this.kontaktService = kontaktService;
        this.aktivitaetService = aktivitaetService;
        this.renewalService = renewalService;
        this.importExportService = importExportService;
        this.organisationService = organisationService;
    }

    /** Portfolio-Liste der gesponserten Vereine. */
    @GetMapping
    public String portfolio(@PathVariable String sponsorSlug,
                            @RequestParam(required = false) String suche,
                            @RequestParam(required = false) AccountStatus status,
                            @RequestParam(required = false) PipelineStage pipeline,
                            @RequestParam(required = false) String sort,
                            @RequestParam(required = false, defaultValue = "asc") String dir,
                            @RequestParam(required = false, defaultValue = "1") int seite,
                            @RequestParam(required = false, defaultValue = "25") int groesse,
                            Authentication auth, Model model) {
        UUID sponsorOrgId = organisationService.findeIdNachSlug(sponsorSlug);
        // Zugriffs-Schranke ZUERST — wirft AccessDenied bevor irgendwelche
        // Org-Daten (z.B. der Name) geladen werden.
        var accounts = accountService.findePortfolio(sponsorOrgId, auth);
        boolean absteigend = "desc".equalsIgnoreCase(dir);
        PortfolioAnsicht ansicht = PortfolioAnsicht.erstelle(
                accounts, suche, status, pipeline, sort, absteigend, seite, groesse);

        model.addAttribute(ModelAttributeNames.AKTIVE_SEITE, "organisationen");
        model.addAttribute("sponsorSlug", sponsorSlug);
        model.addAttribute("sponsorName", organisationService.findeKopfNachSlug(sponsorSlug).name());
        model.addAttribute("ansicht", ansicht);
        model.addAttribute("renewals", renewalService.findeAuslaufende(sponsorOrgId, auth));
        model.addAttribute("statusWerte", AccountStatus.values());
        model.addAttribute("tierWerte", AccountTier.values());
        model.addAttribute("pipelineStageWerte", PipelineStage.values());
        model.addAttribute("seitenGroessen", PortfolioAnsicht.SEITENGROESSEN);
        return "crm/portfolio";
    }

    /** Formular: neuen Account anlegen (Verein-Picker) bzw. Verein neu anlegen. */
    @GetMapping("/neu")
    public String neuesFormular(@PathVariable String sponsorSlug, Authentication auth, Model model) {
        UUID sponsorOrgId = organisationService.findeIdNachSlug(sponsorSlug);
        // Zugriffs-Schranke früh ziehen — findePortfolio wirft AccessDenied falls fremd.
        accountService.findePortfolio(sponsorOrgId, auth);
        return zeigeNeuFormular(sponsorSlug, model);
    }

    /** Füllt das gemeinsame Model für die /neu-Seite (Picker + Inline-Anlage). */
    private String zeigeNeuFormular(String sponsorSlug, Model model) {
        model.addAttribute(ModelAttributeNames.AKTIVE_SEITE, "organisationen");
        model.addAttribute("sponsorSlug", sponsorSlug);
        model.addAttribute("vereine", organisationService.findeAktiveVereineAlsViews());
        if (!model.containsAttribute("vereinForm")) {
            OrganisationFormDto leeresForm = new OrganisationFormDto();
            // Typ vorbelegen, damit das per th:field gebundene Hidden-Feld
            // tatsächlich value="VEREIN" rendert (sonst überschreibt der null-Wert
            // des frischen DTO das statische value und @Valid scheitert an @NotNull typ).
            leeresForm.setTyp(OrgTyp.VEREIN);
            model.addAttribute("vereinForm", leeresForm);
        }
        model.addAttribute("branchen", Branche.values());
        return "crm/account-form";
    }

    /**
     * Bulk-Aktion auf markierte Accounts. {@code bulkAktion} ist kodiert als
     * {@code aktion:wert} (z.B. {@code status:AKTIV}, {@code pipeline:GEWONNEN},
     * {@code tier:CORE}) bzw. {@code entfernen}. So genügt ein Select ohne
     * abhängiges JS. Mandanten-Bindung an die Seiten-Org liegt im Service.
     *
     * <p>{@code alleGefiltert=true} wendet die Aktion auf ALLE Treffer der
     * aktuellen Filter/Suche an (über Seitengrenzen hinweg) — die IDs werden
     * server-seitig aus dem zugriffsgeprüften, neu gefilterten Set abgeleitet,
     * nicht aus dem Request. Das ist robuster + sicherer als hunderte Hidden-IDs.
     */
    @PostMapping("/bulk")
    public String bulk(@PathVariable String sponsorSlug,
                       @RequestParam("bulkAktion") String bulkAktion,
                       @RequestParam(value = "ids", required = false) java.util.List<UUID> ids,
                       @RequestParam(value = "alleGefiltert", required = false, defaultValue = "false") boolean alleGefiltert,
                       @RequestParam(required = false) String suche,
                       @RequestParam(required = false) AccountStatus status,
                       @RequestParam(required = false) PipelineStage pipeline,
                       Authentication auth, RedirectAttributes redirectAttributes) {
        UUID sponsorOrgId = organisationService.findeIdNachSlug(sponsorSlug);

        // „Alle Treffer" → IDs aus dem gefilterten Set ableiten (zugriffsgeprüft via findePortfolio).
        if (alleGefiltert) {
            var alle = accountService.findePortfolio(sponsorOrgId, auth);
            ids = PortfolioAnsicht.erstelle(alle, suche, status, pipeline, null, false, 1, Integer.MAX_VALUE)
                    .gefilterteIds();
        }
        if (ids == null || ids.isEmpty() || bulkAktion == null || bulkAktion.isBlank()) {
            redirectAttributes.addFlashAttribute("fehlerMeldung", "Keine Auswahl oder Aktion.");
            return "redirect:/crm/" + sponsorSlug;
        }
        String[] teile = bulkAktion.split(":", 2);
        String aktion = teile[0];
        String wert = teile.length > 1 ? teile[1] : null;

        int n = switch (aktion) {
            case "status" -> accountService.bulkSetzeStatus(sponsorOrgId, ids, AccountStatus.valueOf(wert), auth);
            case "pipeline" -> accountService.bulkSetzePipeline(sponsorOrgId, ids, PipelineStage.valueOf(wert), auth);
            case "tier" -> accountService.bulkSetzeTier(sponsorOrgId, ids, AccountTier.valueOf(wert), auth);
            case "entfernen" -> accountService.bulkLoesche(sponsorOrgId, ids, auth);
            default -> throw new IllegalArgumentException("Unbekannte Bulk-Aktion: " + aktion);
        };
        redirectAttributes.addFlashAttribute("erfolgsMeldung", n + " Vereine aktualisiert.");
        return "redirect:/crm/" + sponsorSlug;
    }

    /** Portfolio als CSV exportieren (Excel-kompatibel). */
    @GetMapping("/export.csv")
    public ResponseEntity<ByteArrayResource> exportCsv(@PathVariable String sponsorSlug, Authentication auth) {
        UUID sponsorOrgId = organisationService.findeIdNachSlug(sponsorSlug);
        byte[] csv = importExportService.exportiere(sponsorOrgId, auth);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"crm-" + sponsorSlug + ".csv\"")
                .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
                .body(new ByteArrayResource(csv));
    }

    /** Import-Formular (CSV-Upload). */
    @GetMapping("/import")
    public String importFormular(@PathVariable String sponsorSlug, Authentication auth, Model model) {
        UUID sponsorOrgId = organisationService.findeIdNachSlug(sponsorSlug);
        accountService.findePortfolio(sponsorOrgId, auth); // Zugriffs-Schranke
        model.addAttribute(ModelAttributeNames.AKTIVE_SEITE, "organisationen");
        model.addAttribute("sponsorSlug", sponsorSlug);
        return "crm/import";
    }

    /** CSV importieren (Upsert je verein_slug) und Ergebnis-Report rendern. */
    @PostMapping("/import")
    public String importieren(@PathVariable String sponsorSlug,
                              @RequestParam("datei") MultipartFile datei,
                              Authentication auth, Model model) throws IOException {
        UUID sponsorOrgId = organisationService.findeIdNachSlug(sponsorSlug);
        model.addAttribute(ModelAttributeNames.AKTIVE_SEITE, "organisationen");
        model.addAttribute("sponsorSlug", sponsorSlug);
        if (datei == null || datei.isEmpty()) {
            model.addAttribute("keineDatei", true);
            return "crm/import";
        }
        model.addAttribute("ergebnis", importExportService.importiere(sponsorOrgId, datei.getBytes(), auth));
        return "crm/import";
    }

    /** Account anlegen (bestehender Verein aus dem Picker). */
    @PostMapping
    public String erstelle(@PathVariable String sponsorSlug,
                           @RequestParam UUID vereinOrgId,
                           Authentication auth,
                           RedirectAttributes redirectAttributes) {
        UUID sponsorOrgId = organisationService.findeIdNachSlug(sponsorSlug);
        accountService.erstelle(sponsorOrgId, vereinOrgId, auth);
        redirectAttributes.addFlashAttribute("erfolgsMeldung", "Account angelegt");
        return "redirect:/crm/" + sponsorSlug;
    }

    /**
     * Verein inline neu anlegen und sofort als Account aufnehmen. Deckt den Fall
     * ab, dass der Zielverein noch gar nicht auf der Plattform ist — statt ihn
     * erst separat über {@code /organisationen/neu} zu registrieren, legt der
     * Sponsor ihn hier mit den Pflichtfeldern (Name + Branche) direkt an.
     *
     * <p>Der Typ wird serverseitig auf {@link OrgTyp#VEREIN} fixiert (das Formular
     * trägt kein Typ-Feld) — Mass-Assignment-Schutz, und die XOR-Branche-Validierung
     * im {@code OrganisationService} verlangt dann zwingend eine Verein-Branche.
     * Die neue Org bekommt KEINEN Eigentümer: sie gehört nicht dem Sponsor, der
     * Sponsor verknüpft sie nur in seinem privaten CRM.
     */
    @PostMapping("/verein-anlegen")
    public String vereinAnlegen(@PathVariable String sponsorSlug,
                                @Valid @ModelAttribute("vereinForm") OrganisationFormDto vereinForm,
                                BindingResult br,
                                Authentication auth,
                                Model model,
                                RedirectAttributes redirectAttributes) {
        UUID sponsorOrgId = organisationService.findeIdNachSlug(sponsorSlug);
        vereinForm.setTyp(OrgTyp.VEREIN);
        if (br.hasErrors()) {
            return zeigeNeuFormular(sponsorSlug, model);
        }
        try {
            OrganisationView neuerVerein = organisationService.erstelleAlsView(vereinForm);
            accountService.erstelle(sponsorOrgId, neuerVerein.id(), auth);
        } catch (IllegalArgumentException ex) {
            // Service-Fehler (z.B. fehlende Branche, Slug-Konflikt): Formular mit
            // den Eingaben erneut zeigen statt 500 — kein Account angelegt.
            model.addAttribute(ModelAttributeNames.FEHLERMELDUNG, ex.getMessage());
            return zeigeNeuFormular(sponsorSlug, model);
        }
        // Erfolg → Post/Redirect/Get aufs Portfolio, wo der neue Account nun sichtbar ist.
        redirectAttributes.addFlashAttribute(ModelAttributeNames.ERFOLGS_MELDUNG,
                "Verein \"" + vereinForm.getName() + "\" angelegt und ins Portfolio aufgenommen.");
        return "redirect:/crm/" + sponsorSlug;
    }

    /** Status / Tier / Notiz eines Accounts aktualisieren. */
    @PostMapping("/{accountId}")
    public String aktualisiere(@PathVariable String sponsorSlug,
                               @PathVariable UUID accountId,
                               @RequestParam AccountStatus status,
                               @RequestParam(required = false) AccountTier tier,
                               @RequestParam(required = false) PipelineStage pipelineStage,
                               @RequestParam(required = false) java.math.BigDecimal forecastBetragChf,
                               @RequestParam(required = false) String notiz,
                               Authentication auth,
                               RedirectAttributes redirectAttributes) {
        accountService.aktualisiere(accountId, status, tier, pipelineStage, forecastBetragChf, notiz, auth);
        redirectAttributes.addFlashAttribute("erfolgsMeldung", "Account aktualisiert");
        return "redirect:/crm/" + sponsorSlug + "/" + accountId;
    }

    /** Account-Detail (Master-Detail): Account-Daten + Kontakte (Dynamics Account↔Contact). */
    @GetMapping("/{accountId}")
    public String accountDetail(@PathVariable String sponsorSlug, @PathVariable UUID accountId,
                                Authentication auth, Model model) {
        // findeAccount zieht die Mandanten-Schranke; findeKontakte ebenfalls.
        model.addAttribute("account", accountService.findeAccount(accountId, auth));
        model.addAttribute("kontakte", kontaktService.findeKontakte(accountId, auth));
        model.addAttribute("aktivitaeten", aktivitaetService.findeTimeline(accountId, auth));
        model.addAttribute(ModelAttributeNames.AKTIVE_SEITE, "organisationen");
        model.addAttribute("sponsorSlug", sponsorSlug);
        model.addAttribute("statusWerte", AccountStatus.values());
        model.addAttribute("tierWerte", AccountTier.values());
        model.addAttribute("pipelineStageWerte", PipelineStage.values());
        model.addAttribute("kontaktRollen", KontaktRolle.values());
        model.addAttribute("aktivitaetTypen", AktivitaetTyp.values());
        model.addAttribute("heute", java.time.LocalDate.now());
        return "crm/account-detail";
    }

    /** Kontakt anlegen (Dynamics Contact unter Account). */
    @PostMapping("/{accountId}/kontakte")
    public String kontaktErstellen(@PathVariable String sponsorSlug, @PathVariable UUID accountId,
                                   @RequestParam String vorname,
                                   @RequestParam String nachname,
                                   @RequestParam(required = false) String funktion,
                                   @RequestParam(required = false) KontaktRolle kontaktRolle,
                                   @RequestParam(required = false) String email,
                                   @RequestParam(required = false) String telefon,
                                   @RequestParam(required = false) String mobile,
                                   Authentication auth,
                                   RedirectAttributes redirectAttributes) {
        kontaktService.erstelle(accountId, vorname, nachname, funktion, kontaktRolle,
                email, telefon, mobile, auth);
        redirectAttributes.addFlashAttribute("erfolgsMeldung", "Kontakt angelegt");
        return "redirect:/crm/" + sponsorSlug + "/" + accountId;
    }

    /** Kontakt löschen. */
    @PostMapping("/{accountId}/kontakte/{kontaktId}/loeschen")
    public String kontaktLoeschen(@PathVariable String sponsorSlug, @PathVariable UUID accountId,
                                  @PathVariable UUID kontaktId, Authentication auth,
                                  RedirectAttributes redirectAttributes) {
        kontaktService.loesche(kontaktId, auth);
        redirectAttributes.addFlashAttribute("erfolgsMeldung", "Kontakt entfernt");
        return "redirect:/crm/" + sponsorSlug + "/" + accountId;
    }

    /** Aktivität erfassen (Dynamics Activity: Anruf/E-Mail/Meeting/Event/Notiz). */
    @PostMapping("/{accountId}/aktivitaeten")
    public String aktivitaetErfassen(@PathVariable String sponsorSlug, @PathVariable UUID accountId,
                                     @RequestParam AktivitaetTyp typ,
                                     @RequestParam @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) java.time.LocalDate datum,
                                     @RequestParam String betreff,
                                     @RequestParam(required = false) String notiz,
                                     @RequestParam(required = false) UUID kontaktPersonId,
                                     Authentication auth,
                                     RedirectAttributes redirectAttributes) {
        aktivitaetService.erstelle(accountId, typ, datum, betreff, notiz, kontaktPersonId, auth);
        redirectAttributes.addFlashAttribute("erfolgsMeldung", "Aktivität erfasst");
        return "redirect:/crm/" + sponsorSlug + "/" + accountId;
    }

    /** Aktivität löschen. */
    @PostMapping("/{accountId}/aktivitaeten/{aktivitaetId}/loeschen")
    public String aktivitaetLoeschen(@PathVariable String sponsorSlug, @PathVariable UUID accountId,
                                     @PathVariable UUID aktivitaetId, Authentication auth,
                                     RedirectAttributes redirectAttributes) {
        aktivitaetService.loesche(aktivitaetId, auth);
        redirectAttributes.addFlashAttribute("erfolgsMeldung", "Aktivität entfernt");
        return "redirect:/crm/" + sponsorSlug + "/" + accountId;
    }
}
