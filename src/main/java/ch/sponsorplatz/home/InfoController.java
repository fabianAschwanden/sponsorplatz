package ch.sponsorplatz.home;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Statische Public-Pages: Impressum, Datenschutzerklärung und AGB.
 *
 * <p>Pflicht in der Schweiz (DSG) sobald die Plattform öffentlich erreichbar ist
 * und personenbezogene Daten verarbeitet werden (Login = Daten).
 */
@Controller
public class InfoController {

    @GetMapping("/impressum")
    public String impressum() {
        return "impressum";
    }

    @GetMapping("/datenschutz")
    public String datenschutz() {
        return "datenschutz";
    }

    @GetMapping("/agb")
    public String agb() {
        return "agb";
    }
}
