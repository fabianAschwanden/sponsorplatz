package ch.sponsorplatz.organisation;

import ch.sponsorplatz.shared.config.ModelAttributeNames;
import ch.sponsorplatz.shared.exception.NotFoundException;
import jakarta.validation.Valid;
import org.springframework.security.access.AccessDeniedException;
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
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/organisationen")
public class OrganisationController {

    private final OrganisationService service;
    private final AccessControl accessControl;
    private final OrgHierarchieService hierarchieService;

    public OrganisationController(OrganisationService service, AccessControl accessControl,
                                  OrgHierarchieService hierarchieService) {
        this.service = service;
        this.accessControl = accessControl;
        this.hierarchieService = hierarchieService;
    }

    @GetMapping
    public String liste(Model model) {
        model.addAttribute(ModelAttributeNames.AKTIVE_SEITE, "organisationen");
        model.addAttribute("organisationen", OrganisationView.von(service.alle()));
        return "organisationen";
    }

    /**
     * Wizard-Schritt 1: ohne Typ-Param zeigen wir die Typ-Auswahl-Seite.
     * Mit {@code ?typ=VEREIN|UNTERNEHMEN|...} gehen wir direkt zum
     * typ-spezifischen Formular — das hält die UX konsistent (Verein-Form
     * fragt anders als Sponsor-Form).
     */
    @GetMapping("/neu")
    public String neuesFormular(@RequestParam(required = false) OrgTyp typ, Model model) {
        model.addAttribute(ModelAttributeNames.AKTIVE_SEITE, "organisationen");
        if (typ == null) {
            model.addAttribute("typen", OrgTyp.values());
            return "organisation-typ-waehlen";
        }
        OrganisationFormDto dto = new OrganisationFormDto();
        dto.setTyp(typ);
        model.addAttribute("orgForm", dto);
        zeigeFormularModelDaten(model);
        return "organisation-form";
    }

    /** Create — POST /organisationen (kein id im Body). */
    @PostMapping
    public String erstelle(@Valid @ModelAttribute("orgForm") OrganisationFormDto dto,
                           BindingResult br,
                           Model model,
                           RedirectAttributes redirect) {
        if (br.hasErrors()) {
            return zeigeFormular(model);
        }
        try {
            Organisation neu = service.erstelle(dto);
            redirect.addFlashAttribute(ModelAttributeNames.ERFOLGS_MELDUNG,
                "Organisation \"" + neu.getName() + "\" erstellt.");
            return "redirect:/organisationen/" + neu.getSlug();
        } catch (IllegalArgumentException ex) {
            model.addAttribute(ModelAttributeNames.FEHLERMELDUNG, ex.getMessage());
            return zeigeFormular(model);
        }
    }

    /** Update — POST /organisationen/{slug}, Slug aus URL. AccessControl-Check vorab. */
    @PostMapping("/{slug}")
    public String aktualisiere(@PathVariable String slug,
                               @Valid @ModelAttribute("orgForm") OrganisationFormDto dto,
                               BindingResult br,
                               Authentication auth,
                               Model model,
                               RedirectAttributes redirect) {
        if (!accessControl.kannOrgEditierenNachSlug(slug, auth)) {
            throw new AccessDeniedException("Keine Edit-Berechtigung für Org: " + slug);
        }
        if (br.hasErrors()) {
            return zeigeFormular(model);
        }
        try {
            Organisation aktualisiert = service.aktualisiere(slug, dto);
            redirect.addFlashAttribute(ModelAttributeNames.ERFOLGS_MELDUNG,
                "Organisation \"" + aktualisiert.getName() + "\" aktualisiert.");
            return "redirect:/organisationen/" + aktualisiert.getSlug();
        } catch (IllegalArgumentException ex) {
            model.addAttribute(ModelAttributeNames.FEHLERMELDUNG, ex.getMessage());
            return zeigeFormular(model);
        }
    }

    private String zeigeFormular(Model model) {
        zeigeFormularModelDaten(model);
        return "organisation-form";
    }

    /** Pure Model-Befüllung — von zeigeFormular und vom Pre-Fill-GET genutzt. */
    private void zeigeFormularModelDaten(Model model) {
        model.addAttribute(ModelAttributeNames.AKTIVE_SEITE, "organisationen");
        model.addAttribute("typen", OrgTyp.values());
        model.addAttribute("branchen", Branche.values());
        model.addAttribute("sponsorBranchen", SponsorBranche.values());
        // Mögliche Eltern-Orgs für die Hierarchie-Auswahl. Konzeptionell sind das
        // alle Orgs (UNTERNEHMEN sowie STIFTUNG), nicht VEREINE — Vereine haben
        // keine Sub-Strukturen. Service-Validierung greift zusätzlich.
        model.addAttribute("verfuegbareElternOrgs",
                OrganisationView.von(service.alle()));
    }

    @GetMapping("/{slug}")
    public String detail(@PathVariable String slug, Model model) {
        Organisation org = service.findeNachSlug(slug)
            .orElseThrow(() -> new NotFoundException("Organisation nicht gefunden: " + slug));
        model.addAttribute(ModelAttributeNames.AKTIVE_SEITE, "organisationen");
        model.addAttribute("org", OrganisationView.von(org));
        model.addAttribute("statusOk", org.getStatus() == OrgStatus.ACTIVE || org.getStatus() == OrgStatus.VERIFIED);
        // Untergeordnete Orgs separat laden — der OneToMany ist LAZY und das
        // Sammeln über die Eltern-Beziehung wäre N+1. Eigene Repo-Query reicht.
        model.addAttribute("untergeordneteOrgs",
                OrganisationView.von(service.findeUntergeordnete(org.getId())));
        // Eltern-Kette von Wurzel bis zur aktuellen Org (inkl. sich selbst) —
        // für die visuelle Hierarchie-Darstellung im Template.
        model.addAttribute("elternkette", hierarchieService.findeElternkette(org));
        return "organisation-detail";
    }

    @GetMapping("/{slug}/bearbeiten")
    public String bearbeitenFormular(@PathVariable String slug, Authentication auth, Model model) {
        if (!accessControl.kannOrgEditierenNachSlug(slug, auth)) {
            throw new AccessDeniedException("Keine Edit-Berechtigung für Org: " + slug);
        }
        Organisation org = service.findeNachSlug(slug)
            .orElseThrow(() -> new NotFoundException("Organisation nicht gefunden: " + slug));
        model.addAttribute("orgForm", inFormDto(org));
        model.addAttribute("bearbeitenSlug", slug);
        zeigeFormularModelDaten(model);
        return "organisation-form";
    }

    @PostMapping("/{slug}/loeschen")
    public String loesche(@PathVariable String slug, Authentication auth, RedirectAttributes redirect) {
        if (!accessControl.kannOrgVerwaltenNachSlug(slug, auth)) {
            throw new AccessDeniedException("Keine Verwalten-Berechtigung für Org: " + slug);
        }
        Organisation org = service.findeNachSlug(slug)
            .orElseThrow(() -> new NotFoundException("Organisation nicht gefunden: " + slug));
        String name = org.getName();
        service.loesche(org.getId());
        redirect.addFlashAttribute(ModelAttributeNames.ERFOLGS_MELDUNG,
            "Organisation \"" + name + "\" gelöscht.");
        return "redirect:/organisationen";
    }

    private OrganisationFormDto inFormDto(Organisation org) {
        OrganisationFormDto dto = new OrganisationFormDto();
        dto.setTyp(org.getTyp());
        dto.setName(org.getName());
        dto.setSlug(org.getSlug());
        dto.setRechtsform(org.getRechtsform());
        dto.setBranche(org.getBranche());
        dto.setSponsorBranche(org.getSponsorBranche());
        dto.setBeschreibung(org.getBeschreibung());
        dto.setWebsiteUrl(org.getWebsiteUrl());
        dto.setIban(org.getIban());
        dto.setStrasse(org.getStrasse());
        dto.setPostleitzahl(org.getPostleitzahl());
        dto.setOrt(org.getOrt());
        if (org.getUebergeordneteOrg() != null) {
            dto.setUebergeordneteOrgId(org.getUebergeordneteOrg().getId());
        }
        return dto;
    }
}
