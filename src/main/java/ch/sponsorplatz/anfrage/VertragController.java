package ch.sponsorplatz.anfrage;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import jakarta.validation.Valid;

import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import ch.sponsorplatz.organisation.AccessControl;
import ch.sponsorplatz.shared.exception.NotFoundException;
import ch.sponsorplatz.shared.pdf.PdfGeneratorService;
import ch.sponsorplatz.shared.storage.StorageObjectNotFoundException;
import ch.sponsorplatz.shared.storage.StorageService;

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
    private final StorageService storageService;

    public VertragController(VertragService vertragService,
            PdfGeneratorService pdfGenerator,
            AccessControl accessControl,
            StorageService storageService) {
        this.vertragService = vertragService;
        this.pdfGenerator = pdfGenerator;
        this.accessControl = accessControl;
        this.storageService = storageService;
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
        return "anfrage/vertrag-detail";
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
    public ResponseEntity<? extends Resource> pdf(@PathVariable String slug,
            @PathVariable UUID id,
            Authentication auth) {
        VertragView v = vertragService.findeViewNachId(id);
        pruefeAccess(slug, v, auth);

        // Hochgeladenes Dokument hat Vorrang vor dem generierten PDF.
        if (v.hatHochgeladenesDokument()) {
            return liefereHochgeladenesDokument(id);
        }

        Map<String, Object> vars = new HashMap<>();
        vars.put("vertrag", v);
        vars.put("erstelltAmDatum", LocalDate.now());

        byte[] pdf = pdfGenerator.erzeuge("anfrage/vertrag-pdf", vars, "/");
        String dateiname = "sponsorplatz-vertrag-"
                + DATEINAME_TS.format(LocalDate.now()) + "-" + id + ".pdf";
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + dateiname + "\"")
                .contentLength(pdf.length)
                .body(new ByteArrayResource(pdf));
    }

    /**
     * Lädt ein eigenes Vertragsdokument (PDF) hoch. Ist eines vorhanden, wird es
     * ersetzt. Erlaubt in jedem Vertrags-Status (reine Beleg-Beilage).
     */
    @PostMapping("/vertraege/{id}/dokument")
    public String dokumentHochladen(@PathVariable String slug,
            @PathVariable UUID id,
            @RequestParam("dokument") MultipartFile dokument,
            Authentication auth,
            RedirectAttributes redirect) {
        if (!accessControl.kannOrgEditierenNachSlug(slug, auth)) {
            throw new AccessDeniedException("Keine Edit-Berechtigung für Org: " + slug);
        }
        // Org-Zugehörigkeit des Vertrags prüfen, bevor wir schreiben.
        pruefeAccess(slug, vertragService.findeViewNachId(id), auth);
        try {
            vertragService.speichereDokument(id, dokument, auth.getName());
            redirect.addFlashAttribute("erfolgsMeldung",
                    "Vertragsdokument hochgeladen — es wird jetzt beim PDF-Download ausgeliefert.");
        } catch (IllegalArgumentException e) {
            redirect.addFlashAttribute("fehlermeldung", e.getMessage());
        }
        return "redirect:/organisationen/" + slug + "/vertraege/" + id;
    }

    /** Entfernt ein hochgeladenes Vertragsdokument — danach gilt wieder das generierte PDF. */
    @PostMapping("/vertraege/{id}/dokument/entfernen")
    public String dokumentEntfernen(@PathVariable String slug,
            @PathVariable UUID id,
            Authentication auth,
            RedirectAttributes redirect) {
        if (!accessControl.kannOrgEditierenNachSlug(slug, auth)) {
            throw new AccessDeniedException("Keine Edit-Berechtigung für Org: " + slug);
        }
        pruefeAccess(slug, vertragService.findeViewNachId(id), auth);
        try {
            vertragService.entferneDokument(id, auth.getName());
            redirect.addFlashAttribute("erfolgsMeldung",
                    "Vertragsdokument entfernt — der PDF-Download liefert wieder das generierte PDF.");
        } catch (IllegalStateException e) {
            redirect.addFlashAttribute("fehlermeldung", e.getMessage());
        }
        return "redirect:/organisationen/" + slug + "/vertraege/" + id;
    }

    private ResponseEntity<Resource> liefereHochgeladenesDokument(UUID id) {
        VertragService.DokumentSnapshot snap = vertragService.findeDokumentSnapshot(id);
        Resource resource;
        try {
            resource = storageService.ladeAlsResource(snap.storagePfad());
        } catch (StorageObjectNotFoundException e) {
            // Orphaned: DB-Verweis vorhanden, Storage-Objekt fehlt → 404 statt 500.
            return ResponseEntity.notFound().build();
        }
        // ContentDisposition.attachment().filename(...) encodet RFC-5987 +
        // filtert Quotes/Newlines → kein Header-Injection über den Dateinamen.
        ContentDisposition disposition = ContentDisposition.attachment()
                .filename(snap.dateiname() != null ? snap.dateiname() : "vertrag.pdf")
                .build();
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .body(resource);
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
