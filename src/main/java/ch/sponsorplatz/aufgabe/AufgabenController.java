package ch.sponsorplatz.aufgabe;

import ch.sponsorplatz.shared.config.ModelAttributeNames;
import ch.sponsorplatz.shared.util.ListenSeite;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Comparator;
import java.util.List;
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
    public String meineAufgaben(Authentication auth,
                                @RequestParam(required = false) String suche,
                                @RequestParam(required = false) String sort,
                                @RequestParam(required = false, defaultValue = "asc") String dir,
                                @RequestParam(required = false, defaultValue = "1") int seite,
                                @RequestParam(required = false, defaultValue = "25") int groesse,
                                Model model) {
        List<AufgabeView> alle = AufgabeView.von(aufgabenService.meineOffenen(auth.getName()));
        String such = (suche != null && !suche.isBlank()) ? suche.trim().toLowerCase() : null;
        List<AufgabeView> gefiltert = alle.stream()
                .filter(a -> such == null || passtZurSuche(a, such))
                .toList();
        boolean absteigend = "desc".equalsIgnoreCase(dir);
        List<AufgabeView> sortiert = sortiere(gefiltert, sort, absteigend);
        var liste = ListenSeite.von(sortiert, seite, groesse, sort, absteigend);

        model.addAttribute(ModelAttributeNames.AKTIVE_SEITE, "aufgaben");
        model.addAttribute("aufgaben", liste.inhalt());
        model.addAttribute("liste", liste);
        model.addAttribute("anzahlGesamt", alle.size());
        model.addAttribute("anzahlGezeigt", gefiltert.size());
        model.addAttribute("filterAktiv", such != null);
        model.addAttribute("filterSuche", suche);
        model.addAttribute("seitenGroessen", ListenSeite.SEITENGROESSEN);
        return "aufgabe/aufgaben";
    }

    private static boolean passtZurSuche(AufgabeView a, String such) {
        return (a.titel() != null && a.titel().toLowerCase().contains(such))
                || (a.assigneeOrgName() != null && a.assigneeOrgName().toLowerCase().contains(such));
    }

    private static List<AufgabeView> sortiere(List<AufgabeView> liste, String sort, boolean absteigend) {
        if (sort == null) return liste;
        Comparator<AufgabeView> c = switch (sort) {
            case "titel" -> Comparator.comparing(AufgabeView::titel, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER));
            case "zugewiesen" -> Comparator.comparing(a -> a.assigneeOrgName() != null ? a.assigneeOrgName() : "",
                    Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER));
            case "erstellt" -> Comparator.comparing(AufgabeView::erstelltAm, Comparator.nullsFirst(Comparator.naturalOrder()));
            default -> null;
        };
        if (c == null) return liste;
        return liste.stream().sorted(absteigend ? c.reversed() : c).toList();
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
