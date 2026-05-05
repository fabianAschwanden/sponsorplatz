package ch.sponsorplatz.controller;

import ch.sponsorplatz.config.ModelAttributeNames;
import ch.sponsorplatz.dto.SponsorRegistrierungFormDto;
import ch.sponsorplatz.model.Branche;
import ch.sponsorplatz.service.SponsorRegistrierungService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * Controller für Sponsor-Organisation-Selbstregistrierung.
 * Kombiniert User-Registrierung mit Firmen-Anlage in einem Schritt.
 */
@Controller
@RequestMapping("/sponsor/registrieren")
public class SponsorRegistrierungController {

    private final SponsorRegistrierungService sponsorRegistrierungService;

    public SponsorRegistrierungController(SponsorRegistrierungService sponsorRegistrierungService) {
        this.sponsorRegistrierungService = sponsorRegistrierungService;
    }

    @GetMapping
    public String formular(Model model) {
        model.addAttribute("sponsorForm", new SponsorRegistrierungFormDto());
        model.addAttribute("branchen", Branche.values());
        return "sponsor-registrieren";
    }

    @PostMapping
    public String registriere(@Valid @ModelAttribute("sponsorForm") SponsorRegistrierungFormDto dto,
                              BindingResult br,
                              Model model) {
        if (br.hasErrors()) {
            model.addAttribute("branchen", Branche.values());
            return "sponsor-registrieren";
        }
        try {
            sponsorRegistrierungService.registriereSponsor(dto);
            return "redirect:/login?registriert";
        } catch (IllegalArgumentException ex) {
            model.addAttribute(ModelAttributeNames.FEHLERMELDUNG, ex.getMessage());
            model.addAttribute("branchen", Branche.values());
            return "sponsor-registrieren";
        }
    }
}

