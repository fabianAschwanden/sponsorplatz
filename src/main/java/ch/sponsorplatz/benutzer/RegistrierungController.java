package ch.sponsorplatz.benutzer;

import ch.sponsorplatz.shared.config.ModelAttributeNames;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Controller für Benutzer-Registrierung.
 */
@Controller
public class RegistrierungController {

    private final AppUserService appUserService;

    public RegistrierungController(AppUserService appUserService) {
        this.appUserService = appUserService;
    }

    /**
     * Zeigt das Registrierungs-Formular. Der optionale {@code email}-Parameter
     * wird gesetzt, wenn der User per Einladung kommt — dann ist die Adresse
     * read-only zu betrachten (Form-Submit greift weiter auf POST mit derselben
     * E-Mail). Das Flag {@code einladungOffen} steuert nur den Hinweistext.
     */
    @GetMapping("/registrieren")
    public String formular(@RequestParam(required = false) String email,
                           @RequestParam(required = false) String einladung,
                           Model model) {
        AppUserFormDto dto = new AppUserFormDto();
        if (email != null && !email.isBlank()) {
            dto.setEmail(email.trim().toLowerCase());
        }
        model.addAttribute("userForm", dto);
        model.addAttribute("einladungOffen", "offen".equals(einladung));
        return "benutzer/registrieren";
    }

    @PostMapping("/registrieren")
    public String registriere(@Valid @ModelAttribute("userForm") AppUserFormDto dto,
                              BindingResult br,
                              Model model) {
        if (br.hasErrors()) {
            return "benutzer/registrieren";
        }
        try {
            appUserService.registriere(dto);
            return "redirect:/login?registriert";
        } catch (IllegalArgumentException ex) {
            model.addAttribute(ModelAttributeNames.FEHLERMELDUNG, ex.getMessage());
            return "benutzer/registrieren";
        }
    }
}

