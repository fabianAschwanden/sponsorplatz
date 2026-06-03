package ch.sponsorplatz.home;

import ch.sponsorplatz.shared.config.ModelAttributeNames;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Hilfe-Seite mit Feature-Übersicht und Kurzanleitungen für die wichtigsten
 * Plattform-Funktionen. Nur für eingeloggte User — die Hilfe-Inhalte
 * referenzieren interne Funktionen (Dashboard, Anfragen, CRM etc.).
 */
@Controller
public class HilfeController {

    @GetMapping("/hilfe")
    public String hilfe(Model model) {
        model.addAttribute(ModelAttributeNames.AKTIVE_SEITE, "hilfe");
        return "home/hilfe";
    }
}

