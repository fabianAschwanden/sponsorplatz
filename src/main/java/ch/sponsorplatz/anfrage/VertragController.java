package ch.sponsorplatz.anfrage;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import jakarta.validation.Valid;

import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
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

import ch.sponsorplatz.organisation.AccessControl;
import ch.sponsorplatz.shared.exception.NotFoundException;
import ch.sponsorplatz.shared.pdf.PdfGeneratorService;

/**
 * Sponsoring-Vertrags-Verwaltung pro Organisation.
 */
@Controller
@RequestMapping("/organisationen/{slug}")
public class VertragController {

    private static final DateTimeFormatter DATEINAME_TS = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final VertragService vertragService;
    private final PdfGeneratorService pdfGenerator;
    private final AccessControl accessControl;

    public VertragController(VertragService vertragService,
            PdfGeneratorService pdfGenerator,
            AccessControl accessControl) {
        this.vertragService = vertragService;
        this.pdfGenerator = pdfGenerator;
        this.accessControl = accessControl;
    }

    @PostMapping("/anfragen/{anfrageId}/vertrag/erstellen")
    public String erstellen(@PathVariable String slug,
            @PathVariable UUID anfrageId,
            Authentication auth,
            RedirectAttributes redirect) {
        if (!accessControl.kannOrgEditierenNachSlug(slug, auth)) {
            throw new AccessDeniedException("Keine Edit-Berechtigung für Org: " + slug);
        }
        VertragView v = vertragService.erstelleAlsView(anfrageId, auth.getName());
        redirect.addFlashAttribute("erfolgsMeldung",
                "Vertrags-Entwurf erstellt. Konditionen jetzt ergänzen.");
        return "redirect:/organisationen/" + slug + "/vertraege/" + v.id();
    }

    @GetMapping("/vertraege/{id}")
    public String detail(@PathVariable String slug,
            @PathVariable UUID id,
            Authentication auth,
            Model model) {
        VertragView v = vertragService.findeViewNachId(id);
        pruefeAccess(slug, v, auth);

        model.addAttribute("vertrag", v);
        if (!model.containsAttribute("form")) {
            model.addAttribute("form", vertragService.findeFormularNachId(id));
        }
        return "vertrag-detail";
    }

    @PostMapping("/vertraege/{id}")
    public String speichern(@PathVariable String slug,
            @PathVariable UUID id,
            @Valid @ModelAttribute("form") VertragFormDto form,
            BindingResult bindingResult,
            Authentication auth,
            RedirectAttributes redirect) {
        if (!accessControl.kannOrgEditierenNachSlug(slug, auth)) {
            throw new AccessDeniedException("Keine Edit-Berechtigung für Org: " + slug);
        }
        if (bindingResult.hasErrors()) {
            redirect.addFlashAttribute("fehlermeldung", "Bitte Eingaben prüfen.");
            return "redirect:/organisationen/" + slug + "/vertraege/" + id;
        }
        vertragService.aktualisiereAusForm(id, form);
        redirect.addFlashAttribute("erfolgsMeldung", "Vertrag gespeichert.");
        return "redirect:/organisationen/" + slug + "/vertraege/" + id;
    }

    @PostMapping("/vertraege/{id}/unterzeichnen")
    public String unterzeichnen(@PathVariable String slug,
            @PathVariable UUID id,
            Authentication auth,
            RedirectAttributes redirect) {
        if (!accessControl.kannOrgVerwaltenNachSlug(slug, auth)) {
            throw new AccessDeniedException("Nur ORG_OWNER kann Verträge unterzeichnen.");
        }
        vertragService.markiereUnterzeichnet(id, auth.getName());
        redirect.addFlashAttribute("erfolgsMeldung", "Vertrag als unterzeichnet markiert.");
        return "redirect:/organisationen/" + slug + "/vertraege/" + id;
    }

    @GetMapping("/vertraege/{id}/pdf")
    public ResponseEntity<ByteArrayResource> pdf(@PathVariable String slug,
            @PathVariable UUID id,
            Authentication auth) {
        VertragView v = vertragService.findeViewNachId(id);
        pruefeAccess(slug, v, auth);

        Map<String, Object> vars = new HashMap<>();
        vars.put("vertrag", v);
        vars.put("erstelltAmDatum", LocalDate.now());

        byte[] pdf = pdfGenerator.erzeuge("vertrag-pdf", vars, "/");
        String dateiname = "sponsorplatz-vertrag-"
                + DATEINAME_TS.format(LocalDate.now()) + "-" + id + ".pdf";
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + dateiname + "\"")
                .contentLength(pdf.length)
                .body(new ByteArrayResource(pdf));
    }

    private void pruefeAccess(String slug, VertragView v, Authentication auth) {
        if (v.orgSlug() == null || !slug.equals(v.orgSlug())) {
            throw new NotFoundException("Vertrag nicht gefunden.");
        }
        if (!accessControl.kannOrgEditierenNachSlug(slug, auth)) {
            throw new AccessDeniedException("Keine Berechtigung für Org: " + slug);
        }
    }
}
