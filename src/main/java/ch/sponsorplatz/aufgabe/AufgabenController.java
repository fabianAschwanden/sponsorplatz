package ch.sponsorplatz.aufgabe;

import ch.sponsorplatz.shared.config.ModelAttributeNames;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.UUID;

/**
 * "Meine Aufgaben"-Seite — listet alle offenen Aufgaben des eingeloggten Users
 * (Org-Aufgaben + ggf. Platform-Admin-Aufgaben) und erlaubt das manuelle
 * Abhaken. Auto-Erledigung über Status-Wechsel der Trigger-Entity läuft
 * separat über {@link AufgabenEngine}.
 */
@Controller
public class AufgabenController {

    private final AufgabenService aufgabenService;

    public AufgabenController(AufgabenService aufgabenService) {
        this.aufgabenService = aufgabenService;
    }

    @GetMapping("/aufgaben")
    @PreAuthorize("isAuthenticated()")
    public String meineAufgaben(Authentication auth, Model model) {
        model.addAttribute(ModelAttributeNames.AKTIVE_SEITE, "aufgaben");
        model.addAttribute("aufgaben",
                AufgabeView.von(aufgabenService.meineOffenen(auth.getName())));
        return "aufgaben";
    }

    @PostMapping("/aufgaben/{id}/erledigen")
    @PreAuthorize("isAuthenticated()")
    public String erledige(@PathVariable UUID id, Authentication auth, RedirectAttributes redirect) {
        AufgabeView v = aufgabenService.markiereErledigt(id, auth.getName());
        redirect.addFlashAttribute(ModelAttributeNames.ERFOLGS_MELDUNG,
                "Aufgabe \"" + v.titel() + "\" als erledigt markiert.");
        return "redirect:/aufgaben";
    }
}
