package ch.sponsorplatz.benutzer;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class VerifikationsController {

    private final VerifikationsService verifikationsService;

    public VerifikationsController(VerifikationsService verifikationsService) {
        this.verifikationsService = verifikationsService;
    }

    @GetMapping("/verifizieren")
    public String verifiziere(@RequestParam String token, Model model) {
        try {
            verifikationsService.verifiziere(token);
            model.addAttribute("erfolg", true);
        } catch (IllegalArgumentException | IllegalStateException ex) {
            model.addAttribute("erfolg", false);
            model.addAttribute("fehlermeldung", ex.getMessage());
        }
        return "benutzer/verifizierung";
    }
}

