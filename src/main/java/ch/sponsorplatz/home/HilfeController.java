package ch.sponsorplatz.home;

import ch.sponsorplatz.shared.config.ModelAttributeNames;
import ch.sponsorplatz.shared.exception.NotFoundException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;
import java.util.stream.IntStream;

/**
 * Hilfe-Seite mit Feature-Übersicht ({@code /hilfe}) und thematischen
 * Detailseiten mit Schritt-für-Schritt-Anleitungen ({@code /hilfe/{thema}}).
 * Nur für eingeloggte User — die Hilfe-Inhalte referenzieren interne Funktionen
 * (Dashboard, Anfragen, CRM etc.).
 */
@Controller
public class HilfeController {

    @GetMapping("/hilfe")
    public String hilfe(Model model) {
        model.addAttribute(ModelAttributeNames.AKTIVE_SEITE, "hilfe");
        return "home/hilfe";
    }

    /**
     * Detailseite eines Themas. Der Slug wird auf ein {@link HilfeThema}
     * aufgelöst; unbekannte Slugs → 404 (GlobalExceptionHandler). Ins Model
     * gehen nur primitive/i18n-Schlüssel, keine Entity (View-Pflicht).
     */
    @GetMapping("/hilfe/{thema}")
    public String detail(@PathVariable String thema, Model model) {
        HilfeThema h = HilfeThema.nachSlug(thema)
                .orElseThrow(() -> new NotFoundException("Hilfe-Thema nicht gefunden: " + thema));

        // Schritt-Indizes 1..n — das Template zieht je Index die i18n-Texte
        // hilfe.{key}.schritt.{i}.titel / .text.
        List<Integer> schritte = IntStream.rangeClosed(1, h.anzahlSchritte()).boxed().toList();

        model.addAttribute(ModelAttributeNames.AKTIVE_SEITE, "hilfe");
        model.addAttribute("themaKey", h.i18nSchluessel());
        model.addAttribute("themaSlug", h.slug());
        model.addAttribute("schritte", schritte);
        return "home/hilfe-detail";
    }
}
