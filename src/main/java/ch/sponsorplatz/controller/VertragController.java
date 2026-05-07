package ch.sponsorplatz.controller;

import ch.sponsorplatz.dto.VertragFormDto;
import ch.sponsorplatz.dto.VertragView;
import ch.sponsorplatz.exception.NotFoundException;
import ch.sponsorplatz.model.Vertrag;
import ch.sponsorplatz.service.AccessControl;
import ch.sponsorplatz.service.PdfGeneratorService;
import ch.sponsorplatz.service.VertragService;
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

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Sponsoring-Vertrags-Verwaltung pro Organisation.
 *
 * <p>Routen:
 * <ul>
 *   <li>{@code POST /organisationen/{slug}/anfragen/{id}/vertrag/erstellen} — neu</li>
 *   <li>{@code GET  /organisationen/{slug}/vertraege/{id}} — Detail/Edit-Form</li>
 *   <li>{@code POST /organisationen/{slug}/vertraege/{id}} — Speichern</li>
 *   <li>{@code POST /organisationen/{slug}/vertraege/{id}/unterzeichnen} — Status-Wechsel</li>
 *   <li>{@code GET  /organisationen/{slug}/vertraege/{id}/pdf} — PDF-Download</li>
 * </ul>
 *
 * <p>Edit + Status-Wechsel: nur ORG_EDITOR/OWNER der Verein-Org.
 * PDF-Download: gleiche AccessControl (Vertrag enthält Sponsor-Kontaktdaten).
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
        Vertrag v = vertragService.erstelle(anfrageId, auth.getName());
        redirect.addFlashAttribute("erfolgsMeldung",
                "Vertrags-Entwurf erstellt. Konditionen jetzt ergänzen.");
        return "redirect:/organisationen/" + slug + "/vertraege/" + v.getId();
    }

    @GetMapping("/vertraege/{id}")
    public String detail(@PathVariable String slug,
                         @PathVariable UUID id,
                         Authentication auth,
                         Model model) {
        Vertrag v = vertragService.findeNachId(id);
        pruefeAccess(slug, v, auth);

        model.addAttribute("vertrag", VertragView.von(v));
        if (!model.containsAttribute("form")) {
            model.addAttribute("form", formAusVertrag(v));
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
        Vertrag aenderungen = new Vertrag();
        aenderungen.setPaketBeschreibung(form.getPaketBeschreibung());
        aenderungen.setPreisChf(form.getPreisChf());
        aenderungen.setLaufzeitVon(form.getLaufzeitVon());
        aenderungen.setLaufzeitBis(form.getLaufzeitBis());
        aenderungen.setLeistungVerein(form.getLeistungVerein());
        aenderungen.setLeistungSponsor(form.getLeistungSponsor());
        vertragService.aktualisiere(id, aenderungen);
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
        Vertrag v = vertragService.findeNachId(id);
        pruefeAccess(slug, v, auth);

        Map<String, Object> vars = new HashMap<>();
        vars.put("vertrag", VertragView.von(v));
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

    /** Vertrag muss zur URL-Slug-Org gehören + User muss Editor der Verein-Org sein. */
    private void pruefeAccess(String slug, Vertrag v, Authentication auth) {
        if (v.getOrg() == null || !slug.equals(v.getOrg().getSlug())) {
            throw new NotFoundException("Vertrag nicht gefunden.");
        }
        if (!accessControl.kannOrgEditierenNachSlug(slug, auth)) {
            throw new AccessDeniedException("Keine Berechtigung für Org: " + slug);
        }
    }

    private VertragFormDto formAusVertrag(Vertrag v) {
        VertragFormDto f = new VertragFormDto();
        f.setPaketBeschreibung(v.getPaketBeschreibung());
        f.setPreisChf(v.getPreisChf());
        f.setLaufzeitVon(v.getLaufzeitVon());
        f.setLaufzeitBis(v.getLaufzeitBis());
        f.setLeistungVerein(v.getLeistungVerein());
        f.setLeistungSponsor(v.getLeistungSponsor());
        return f;
    }
}
