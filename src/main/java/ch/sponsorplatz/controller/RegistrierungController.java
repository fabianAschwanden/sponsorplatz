package ch.sponsorplatz.controller;

import ch.sponsorplatz.config.ModelAttributeNames;
import ch.sponsorplatz.dto.AppUserFormDto;
import ch.sponsorplatz.service.AppUserService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

/**
 * Controller für Benutzer-Registrierung.
 */
@Controller
public class RegistrierungController {

    private final AppUserService appUserService;

    public RegistrierungController(AppUserService appUserService) {
        this.appUserService = appUserService;
    }

    @GetMapping("/registrieren")
    public String formular(Model model) {
        model.addAttribute("userForm", new AppUserFormDto());
        return "registrieren";
    }

    @PostMapping("/registrieren")
    public String registriere(@Valid @ModelAttribute("userForm") AppUserFormDto dto,
                              BindingResult br,
                              Model model) {
        if (br.hasErrors()) {
            return "registrieren";
        }
        try {
            appUserService.registriere(dto);
            return "redirect:/login?registriert";
        } catch (IllegalArgumentException ex) {
            model.addAttribute(ModelAttributeNames.FEHLERMELDUNG, ex.getMessage());
            return "registrieren";
        }
    }
}

