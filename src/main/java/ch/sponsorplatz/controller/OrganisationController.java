package ch.sponsorplatz.controller;

import ch.sponsorplatz.config.ModelAttributeNames;
import ch.sponsorplatz.dto.OrganisationFormDto;
import ch.sponsorplatz.dto.OrganisationView;
import ch.sponsorplatz.exception.NotFoundException;
import ch.sponsorplatz.model.Branche;
import ch.sponsorplatz.model.OrgStatus;
import ch.sponsorplatz.model.OrgTyp;
import ch.sponsorplatz.model.Organisation;
import ch.sponsorplatz.service.AccessControl;
import ch.sponsorplatz.service.OrganisationService;
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
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/organisationen")
public class OrganisationController {

    private final OrganisationService service;
    private final AccessControl accessControl;

    public OrganisationController(OrganisationService service, AccessControl accessControl) {
        this.service = service;
        this.accessControl = accessControl;
    }

    @GetMapping
    public String liste(Model model) {
        model.addAttribute(ModelAttributeNames.AKTIVE_SEITE, "organisationen");
        model.addAttribute("organisationen", OrganisationView.von(service.alle()));
        return "organisationen";
    }

    @GetMapping("/neu")
    public String neuesFormular(Model model) {
        model.addAttribute(ModelAttributeNames.AKTIVE_SEITE, "organisationen");
        model.addAttribute("orgForm", new OrganisationFormDto());
        model.addAttribute("typen", OrgTyp.values());
        model.addAttribute("branchen", Branche.values());
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
        model.addAttribute(ModelAttributeNames.AKTIVE_SEITE, "organisationen");
        model.addAttribute("typen", OrgTyp.values());
        model.addAttribute("branchen", Branche.values());
        return "organisation-form";
    }

    @GetMapping("/{slug}")
    public String detail(@PathVariable String slug, Model model) {
        Organisation org = service.findeNachSlug(slug)
            .orElseThrow(() -> new NotFoundException("Organisation nicht gefunden: " + slug));
        model.addAttribute(ModelAttributeNames.AKTIVE_SEITE, "organisationen");
        model.addAttribute("org", OrganisationView.von(org));
        model.addAttribute("statusOk", org.getStatus() == OrgStatus.ACTIVE || org.getStatus() == OrgStatus.VERIFIED);
        return "organisation-detail";
    }

    @GetMapping("/{slug}/bearbeiten")
    public String bearbeitenFormular(@PathVariable String slug, Authentication auth, Model model) {
        if (!accessControl.kannOrgEditierenNachSlug(slug, auth)) {
            throw new AccessDeniedException("Keine Edit-Berechtigung für Org: " + slug);
        }
        Organisation org = service.findeNachSlug(slug)
            .orElseThrow(() -> new NotFoundException("Organisation nicht gefunden: " + slug));
        model.addAttribute(ModelAttributeNames.AKTIVE_SEITE, "organisationen");
        model.addAttribute("orgForm", inFormDto(org));
        model.addAttribute("typen", OrgTyp.values());
        model.addAttribute("branchen", Branche.values());
        model.addAttribute("bearbeitenSlug", slug);
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
        dto.setBeschreibung(org.getBeschreibung());
        dto.setWebsiteUrl(org.getWebsiteUrl());
        dto.setIban(org.getIban());
        dto.setStrasse(org.getStrasse());
        dto.setPostleitzahl(org.getPostleitzahl());
        dto.setOrt(org.getOrt());
        return dto;
    }
}
