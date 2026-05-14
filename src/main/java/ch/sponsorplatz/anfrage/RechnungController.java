package ch.sponsorplatz.anfrage;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import ch.sponsorplatz.organisation.AccessControl;
import ch.sponsorplatz.shared.exception.NotFoundException;
import ch.sponsorplatz.shared.pdf.PdfGeneratorService;

/**
 * Sponsoring-Rechnungen pro Organisation.
 *
 * <p>
 * Routen:
 * <ul>
 * <li>{@code POST /organisationen/{slug}/vertraege/{vertragId}/rechnung/erstellen}</li>
 * <li>{@code GET  /organisationen/{slug}/rechnungen/{id}} — Detail</li>
 * <li>{@code POST /organisationen/{slug}/rechnungen/{id}/bezahlt} —
 * Status-Wechsel</li>
 * <li>{@code POST /organisationen/{slug}/rechnungen/{id}/stornieren}</li>
 * <li>{@code GET  /organisationen/{slug}/rechnungen/{id}/pdf} — PDF mit
 * QR-Bill</li>
 * </ul>
 */
@Controller
@RequestMapping("/organisationen/{slug}")
public class RechnungController {

    private final RechnungService rechnungService;
    private final QrBillService qrBillService;
    private final PdfGeneratorService pdfGenerator;
    private final AccessControl accessControl;

    public RechnungController(RechnungService rechnungService,
            QrBillService qrBillService,
            PdfGeneratorService pdfGenerator,
            AccessControl accessControl) {
        this.rechnungService = rechnungService;
        this.qrBillService = qrBillService;
        this.pdfGenerator = pdfGenerator;
        this.accessControl = accessControl;
    }

    @PostMapping("/vertraege/{vertragId}/rechnung/erstellen")
    public String erstellen(@PathVariable String slug,
            @PathVariable UUID vertragId,
            Authentication auth,
            RedirectAttributes redirect) {
        if (!accessControl.kannOrgEditierenNachSlug(slug, auth)) {
            throw new AccessDeniedException("Keine Edit-Berechtigung für Org: " + slug);
        }
        try {
            RechnungView v = rechnungService.erstelleAlsView(vertragId, auth.getName());
            redirect.addFlashAttribute("erfolgsMeldung",
                    "Rechnung " + v.rechnungsnummer() + " erstellt — fällig am "
                            + v.faelligAm() + ".");
            return "redirect:/organisationen/" + slug + "/rechnungen/" + v.id();
        } catch (IllegalStateException e) {
            redirect.addFlashAttribute("fehlermeldung", e.getMessage());
            return "redirect:/organisationen/" + slug + "/vertraege/" + vertragId;
        }
    }

    @GetMapping("/rechnungen/{id}")
    public String detail(@PathVariable String slug,
            @PathVariable UUID id,
            Authentication auth,
            Model model) {
        RechnungView v = rechnungService.findeViewNachId(id);
        pruefeAccess(slug, v, auth);

        model.addAttribute("rechnung", v);
        model.addAttribute("qrBildDataUrl", qrBillService.erzeugeAlsDataUrlFuerId(id));
        return "rechnung-detail";
    }

    @PostMapping("/rechnungen/{id}/bezahlt")
    public String markiereBezahlt(@PathVariable String slug,
            @PathVariable UUID id,
            Authentication auth,
            RedirectAttributes redirect) {
        pruefeAccess(slug, rechnungService.findeViewNachId(id), auth);
        try {
            rechnungService.markiereBezahlt(id, auth.getName());
            redirect.addFlashAttribute("erfolgsMeldung", "Rechnung als bezahlt markiert.");
        } catch (IllegalStateException e) {
            redirect.addFlashAttribute("fehlermeldung", e.getMessage());
        }
        return "redirect:/organisationen/" + slug + "/rechnungen/" + id;
    }

    @PostMapping("/rechnungen/{id}/stornieren")
    public String stornieren(@PathVariable String slug,
            @PathVariable UUID id,
            @RequestParam(required = false) String grund,
            Authentication auth,
            RedirectAttributes redirect) {
        pruefeAccess(slug, rechnungService.findeViewNachId(id), auth);
        try {
            rechnungService.stornieren(id, grund);
            redirect.addFlashAttribute("erfolgsMeldung", "Rechnung storniert.");
        } catch (IllegalStateException e) {
            redirect.addFlashAttribute("fehlermeldung", e.getMessage());
        }
        return "redirect:/organisationen/" + slug + "/rechnungen/" + id;
    }

    @GetMapping("/rechnungen/{id}/pdf")
    public ResponseEntity<ByteArrayResource> pdf(@PathVariable String slug,
            @PathVariable UUID id,
            Authentication auth) {
        RechnungView v = rechnungService.findeViewNachId(id);
        pruefeAccess(slug, v, auth);

        Map<String, Object> vars = new HashMap<>();
        vars.put("rechnung", v);
        vars.put("qrBildDataUrl", qrBillService.erzeugeAlsDataUrlFuerId(id));
        vars.put("erstelltAmDatum", LocalDate.now());

        byte[] pdf = pdfGenerator.erzeuge("rechnung-pdf", vars, "/");
        String dateiname = "sponsorplatz-rechnung-" + v.rechnungsnummer() + ".pdf";
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + dateiname + "\"")
                .contentLength(pdf.length)
                .body(new ByteArrayResource(pdf));
    }

    private void pruefeAccess(String slug, RechnungView v, Authentication auth) {
        if (v.orgSlug() == null || !slug.equals(v.orgSlug())) {
            throw new NotFoundException("Rechnung nicht gefunden.");
        }
        if (!accessControl.kannOrgEditierenNachSlug(slug, auth)) {
            throw new AccessDeniedException("Keine Berechtigung für Org: " + slug);
        }
    }
}
