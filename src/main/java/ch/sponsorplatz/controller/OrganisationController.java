package ch.sponsorplatz.controller;

import ch.sponsorplatz.config.ModelAttributeNames;
import ch.sponsorplatz.dto.OrganisationFormDto;
import ch.sponsorplatz.exception.NotFoundException;
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
        model.addAttribute("organisationen", service.alle());
        return "organisationen";
    }

    @GetMapping("/neu")
    public String neuesFormular(Model model) {
        model.addAttribute(ModelAttributeNames.AKTIVE_SEITE, "organisationen");
        model.addAttribute("orgForm", new OrganisationFormDto());
        model.addAttribute("typen", OrgTyp.values());
        return "organisation-form";
    }

    @PostMapping("/speichern")
    public String speichere(@Valid @ModelAttribute("orgForm") OrganisationFormDto dto,
                            BindingResult br,
                            Model model,
                            RedirectAttributes redirect) {
        if (br.hasErrors()) {
            model.addAttribute(ModelAttributeNames.AKTIVE_SEITE, "organisationen");
            model.addAttribute("typen", OrgTyp.values());
            return "organisation-form";
        }
        try {
            Organisation gespeichert = service.speichere(dto);
            redirect.addFlashAttribute(ModelAttributeNames.ERFOLGS_MELDUNG,
                "Organisation \"" + gespeichert.getName() + "\" gespeichert.");
            return "redirect:/organisationen/" + gespeichert.getSlug();
        } catch (IllegalArgumentException ex) {
            model.addAttribute(ModelAttributeNames.AKTIVE_SEITE, "organisationen");
            model.addAttribute("typen", OrgTyp.values());
            model.addAttribute(ModelAttributeNames.FEHLERMELDUNG, ex.getMessage());
            return "organisation-form";
        }
    }

    @GetMapping("/{slug}")
    public String detail(@PathVariable String slug, Model model) {
        Organisation org = service.findeNachSlug(slug)
            .orElseThrow(() -> new NotFoundException("Organisation nicht gefunden: " + slug));
        model.addAttribute(ModelAttributeNames.AKTIVE_SEITE, "organisationen");
        model.addAttribute("org", org);
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
        return "organisation-form";
    }

    @PostMapping("/{slug}/loeschen")
    public String loesche(@PathVariable String slug, Authentication auth, RedirectAttributes redirect) {
        if (!accessControl.kannOrgVerwaltenNachSlug(slug, auth)) {
            throw new AccessDeniedException("Keine Verwalten-Berechtigung für Org: " + slug);
        }
        Organisation org = service.findeNachSlug(slug)
            .orElseThrow(() -> new NotFoundException("Organisation nicht gefunden: " + slug));
        service.loesche(org.getId());
        redirect.addFlashAttribute(ModelAttributeNames.ERFOLGS_MELDUNG,
            "Organisation \"" + org.getName() + "\" gelöscht.");
        return "redirect:/organisationen";
    }

    private OrganisationFormDto inFormDto(Organisation org) {
        OrganisationFormDto dto = new OrganisationFormDto();
        dto.setId(org.getId());
        dto.setTyp(org.getTyp());
        dto.setName(org.getName());
        dto.setSlug(org.getSlug());
        dto.setRechtsform(org.getRechtsform());
        dto.setBranche(org.getBranche());
        dto.setBeschreibung(org.getBeschreibung());
        dto.setWebsiteUrl(org.getWebsiteUrl());
        return dto;
    }
}
