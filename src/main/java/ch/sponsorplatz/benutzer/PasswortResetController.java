package ch.sponsorplatz.benutzer;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Controller für den Passwort-vergessen / Reset-Flow.
 *
 * Routen:
 *   GET  /passwort-vergessen       → Formular (E-Mail eingeben)
 *   POST /passwort-vergessen       → Mail senden, Bestätigung
 *   GET  /passwort-reset?token=... → Formular (neues PW eingeben)
 *   POST /passwort-reset           → PW setzen, Redirect zu Login
 */
@Controller
public class PasswortResetController {

    private final PasswortResetService resetService;

    public PasswortResetController(PasswortResetService resetService) {
        this.resetService = resetService;
    }

    @GetMapping("/passwort-vergessen")
    public String formularAnzeigen() {
        return "benutzer/passwort-vergessen";
    }

    @PostMapping("/passwort-vergessen")
    public String resetAnfordern(@RequestParam String email, Model model) {
        resetService.sendeResetMail(email);
        // Immer Erfolg zeigen (kein Information Leak ob E-Mail existiert)
        model.addAttribute("gesendet", true);
        return "benutzer/passwort-vergessen";
    }

    @GetMapping("/passwort-reset")
    public String neuesPasswortFormular(@RequestParam String token, Model model) {
        try {
            resetService.validiereToken(token);
            model.addAttribute("token", token);
            return "benutzer/passwort-reset";
        } catch (IllegalArgumentException | IllegalStateException e) {
            model.addAttribute("fehlermeldung", e.getMessage());
            return "benutzer/passwort-vergessen";
        }
    }

    @PostMapping("/passwort-reset")
    public String neuesPasswortSetzen(@RequestParam String token,
                                       @RequestParam String neuesPasswort,
                                       @RequestParam String passwortBestaetigung,
                                       RedirectAttributes redirect,
                                       Model model) {
        if (!neuesPasswort.equals(passwortBestaetigung)) {
            model.addAttribute("token", token);
            model.addAttribute("fehlermeldung", "Passwörter stimmen nicht überein");
            return "benutzer/passwort-reset";
        }

        try {
            resetService.setzeNeuesPasswort(token, neuesPasswort);
            redirect.addFlashAttribute("erfolgsMeldung",
                    "Passwort wurde erfolgreich zurückgesetzt. Bitte melden Sie sich an.");
            return "redirect:/login";
        } catch (IllegalArgumentException | IllegalStateException e) {
            model.addAttribute("token", token);
            model.addAttribute("fehlermeldung", e.getMessage());
            return "benutzer/passwort-reset";
        }
    }
}

