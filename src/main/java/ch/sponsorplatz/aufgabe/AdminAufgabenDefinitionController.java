package ch.sponsorplatz.aufgabe;

import ch.sponsorplatz.shared.config.ModelAttributeNames;
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
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.UUID;

/**
 * Admin-UI: pflegt {@link AufgabenDefinition}en — Custom-Tasktypen erstellen,
 * vorhandene aktivieren/deaktivieren, Titel/Beschreibung/Link anpassen.
 * System-Seeds (V36) sind nicht löschbar, behalten ihre Trigger-Felder.
 */
@Controller
@RequestMapping("/admin/aufgaben-definitionen")
@PreAuthorize("hasRole('PLATFORM_ADMIN')")
public class AdminAufgabenDefinitionController {

    private final AufgabenDefinitionService service;

    public AdminAufgabenDefinitionController(AufgabenDefinitionService service) {
        this.service = service;
    }

    @GetMapping
    public String liste(Model model) {
        model.addAttribute(ModelAttributeNames.AKTIVE_SEITE, "admin");
        model.addAttribute("definitionen", AufgabenDefinitionView.von(service.alle()));
        return "admin/aufgaben-definitionen";
    }

    @GetMapping("/neu")
    public String neuesFormular(Model model) {
        zeigeFormular(model, new AufgabenDefinitionFormDto(), null, false);
        return "admin/aufgaben-definition-form";
    }

    @PostMapping
    public String erstelle(@Valid @ModelAttribute("defForm") AufgabenDefinitionFormDto dto,
                            BindingResult br, Authentication auth,
                            Model model, RedirectAttributes redirect) {
        if (br.hasErrors()) {
            zeigeFormular(model, dto, null, false);
            return "admin/aufgaben-definition-form";
        }
        try {
            AufgabenDefinition def = service.erstelle(dto, auth.getName());
            redirect.addFlashAttribute(ModelAttributeNames.ERFOLGS_MELDUNG,
                    "Aufgaben-Definition \"" + def.getTitel() + "\" erstellt.");
            return "redirect:/admin/aufgaben-definitionen";
        } catch (IllegalArgumentException ex) {
            model.addAttribute(ModelAttributeNames.FEHLERMELDUNG, ex.getMessage());
            zeigeFormular(model, dto, null, false);
            return "admin/aufgaben-definition-form";
        }
    }

    @GetMapping("/{id}/bearbeiten")
    public String bearbeitenFormular(@PathVariable UUID id, Model model) {
        AufgabenDefinition def = service.findeNachId(id);
        zeigeFormular(model, inForm(def), id, def.isSystemDefinition());
        return "admin/aufgaben-definition-form";
    }

    @PostMapping("/{id}")
    public String aktualisiere(@PathVariable UUID id,
                                @Valid @ModelAttribute("defForm") AufgabenDefinitionFormDto dto,
                                BindingResult br, Model model, RedirectAttributes redirect) {
        AufgabenDefinition vorhanden = service.findeNachId(id);
        if (br.hasErrors()) {
            zeigeFormular(model, dto, id, vorhanden.isSystemDefinition());
            return "admin/aufgaben-definition-form";
        }
        AufgabenDefinition def = service.aktualisiere(id, dto);
        redirect.addFlashAttribute(ModelAttributeNames.ERFOLGS_MELDUNG,
                "Aufgaben-Definition \"" + def.getTitel() + "\" aktualisiert.");
        return "redirect:/admin/aufgaben-definitionen";
    }

    @PostMapping("/{id}/loeschen")
    public String loesche(@PathVariable UUID id, RedirectAttributes redirect) {
        try {
            service.loesche(id);
            redirect.addFlashAttribute(ModelAttributeNames.ERFOLGS_MELDUNG,
                    "Aufgaben-Definition gelöscht.");
        } catch (IllegalStateException ex) {
            redirect.addFlashAttribute(ModelAttributeNames.FEHLERMELDUNG, ex.getMessage());
        }
        return "redirect:/admin/aufgaben-definitionen";
    }

    private void zeigeFormular(Model model, AufgabenDefinitionFormDto dto,
                                UUID bearbeitenId, boolean systemSchutz) {
        model.addAttribute(ModelAttributeNames.AKTIVE_SEITE, "admin");
        model.addAttribute("defForm", dto);
        model.addAttribute("bearbeitenId", bearbeitenId);
        model.addAttribute("systemSchutz", systemSchutz);
        model.addAttribute("entityTypen", TriggerEntityTyp.values());
        model.addAttribute("assigneeRegeln", AssigneeRegel.values());
    }

    private static AufgabenDefinitionFormDto inForm(AufgabenDefinition def) {
        AufgabenDefinitionFormDto dto = new AufgabenDefinitionFormDto();
        dto.setTitel(def.getTitel());
        dto.setBeschreibung(def.getBeschreibung());
        dto.setTriggerEntityTyp(def.getTriggerEntityTyp());
        dto.setTriggerStatus(def.getTriggerStatus());
        dto.setZielStatus(def.getZielStatus());
        dto.setAssigneeRegel(def.getAssigneeRegel());
        dto.setLinkTemplate(def.getLinkTemplate());
        dto.setAktiv(def.isAktiv());
        return dto;
    }
}
