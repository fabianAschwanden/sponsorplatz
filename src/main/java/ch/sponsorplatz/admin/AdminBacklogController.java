package ch.sponsorplatz.admin;

import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.UUID;

/**
 * Admin-UI für den Feature-Backlog (PLATFORM_ADMIN-only).
 *
 * <p>Routen unter {@code /admin/backlog}:
 * <ul>
 *   <li>{@code GET  /} — Liste + neues Item-Form</li>
 *   <li>{@code POST /erstellen} — neues Item anlegen</li>
 *   <li>{@code POST /{id}/aktualisieren} — Titel/Beschreibung/Priorität ändern</li>
 *   <li>{@code POST /{id}/status?status=…} — Status-Wechsel</li>
 *   <li>{@code POST /{id}/loeschen} — endgültig löschen</li>
 * </ul>
 */
@Controller
@RequestMapping("/admin/backlog")
@PreAuthorize("hasRole('PLATFORM_ADMIN')")
public class AdminBacklogController {

    private final BacklogService service;

    public AdminBacklogController(BacklogService service) {
        this.service = service;
    }

    @GetMapping
    public String liste(Model model) {
        model.addAttribute("items", BacklogItemView.von(service.findeAlleSortiert()));
        model.addAttribute("offenAnzahl", service.zaehleOffen());
        model.addAttribute("statusValues", BacklogStatus.values());
        model.addAttribute("prioritaetValues", BacklogPrioritaet.values());
        if (!model.containsAttribute("form")) {
            model.addAttribute("form", new BacklogFormDto());
        }
        return "admin/backlog";
    }

    @PostMapping("/erstellen")
    public String erstellen(@Valid @ModelAttribute("form") BacklogFormDto form,
                            BindingResult binding,
                            Authentication auth,
                            RedirectAttributes redirect) {
        if (binding.hasErrors()) {
            redirect.addFlashAttribute("fehlermeldung",
                    "Validierungsfehler: " + binding.getAllErrors().get(0).getDefaultMessage());
            return "redirect:/admin/backlog";
        }
        String erstellt = auth != null && auth.getName() != null ? auth.getName() : "system";
        // Titel direkt aus dem FormDto — kein Entity-Lookup, hält Controller
        // ARCH-02-konform (kein @Entity-Touch).
        service.erstelle(form.getTitel(), form.getBeschreibung(), form.getPrioritaet(), erstellt);
        redirect.addFlashAttribute("erfolgsMeldung",
                "Item «" + form.getTitel() + "» angelegt.");
        return "redirect:/admin/backlog";
    }

    @PostMapping("/{id}/aktualisieren")
    public String aktualisieren(@PathVariable UUID id,
                                @RequestParam String titel,
                                @RequestParam(required = false) String beschreibung,
                                @RequestParam BacklogPrioritaet prioritaet,
                                RedirectAttributes redirect) {
        try {
            service.aktualisiere(id, titel, beschreibung, prioritaet);
            redirect.addFlashAttribute("erfolgsMeldung", "Item aktualisiert.");
        } catch (RuntimeException e) {
            redirect.addFlashAttribute("fehlermeldung", e.getMessage());
        }
        return "redirect:/admin/backlog";
    }

    @PostMapping("/{id}/status")
    public String aendereStatus(@PathVariable UUID id,
                                @RequestParam BacklogStatus status,
                                RedirectAttributes redirect) {
        try {
            service.aendereStatus(id, status);
            redirect.addFlashAttribute("erfolgsMeldung",
                    "Status auf «" + status.getLabel() + "» geändert.");
        } catch (RuntimeException e) {
            redirect.addFlashAttribute("fehlermeldung", e.getMessage());
        }
        return "redirect:/admin/backlog";
    }

    @PostMapping("/{id}/loeschen")
    public String loeschen(@PathVariable UUID id, RedirectAttributes redirect) {
        try {
            service.loesche(id);
            redirect.addFlashAttribute("erfolgsMeldung", "Item gelöscht.");
        } catch (RuntimeException e) {
            redirect.addFlashAttribute("fehlermeldung", e.getMessage());
        }
        return "redirect:/admin/backlog";
    }
}
