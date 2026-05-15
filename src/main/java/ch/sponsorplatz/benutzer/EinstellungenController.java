package ch.sponsorplatz.benutzer;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Map;
import java.util.UUID;

/**
 * Einstellungen — Profil, Passwort, DSG-Datenexport und Profilbild-Upload.
 */
@Controller
@RequestMapping("/einstellungen")
public class EinstellungenController {

    private final BenutzerDatenExport datenExport;
    private final AppUserService appUserService;
    private final ProfilbildSpeicherung profilbildSpeicherung;

    public EinstellungenController(BenutzerDatenExport datenExport,
                                   AppUserService appUserService,
                                   ProfilbildSpeicherung profilbildSpeicherung) {
        this.datenExport = datenExport;
        this.appUserService = appUserService;
        this.profilbildSpeicherung = profilbildSpeicherung;
    }

    @GetMapping
    public String einstellungen(Authentication auth, Model model) {
        model.addAttribute("aktiveSeite", "einstellungen");
        model.addAttribute("profil", appUserService.findeProfilViewNachEmail(auth.getName()));
        model.addAttribute("profilForm", appUserService.findeProfilFormularNachEmail(auth.getName()));
        model.addAttribute("sprachen", VERFUEGBARE_SPRACHEN);
        return "einstellungen";
    }

    @PostMapping("/profil")
    public String profilSpeichern(@Valid @ModelAttribute("profilForm") ProfilFormDto dto,
                                   BindingResult br,
                                   Authentication auth,
                                   Model model,
                                   HttpServletResponse response,
                                   RedirectAttributes redirect) {
        if (br.hasErrors()) {
            model.addAttribute("aktiveSeite", "einstellungen");
            model.addAttribute("profil", appUserService.findeProfilViewNachEmail(auth.getName()));
            model.addAttribute("sprachen", VERFUEGBARE_SPRACHEN);
            return "einstellungen";
        }

        UUID userId = appUserService.findeIdNachEmail(auth.getName());
        try {
            appUserService.aktualisiereProfil(userId, dto);
            synchronisiereSprache(dto.getSprache(), response);
            redirect.addFlashAttribute("erfolgsMeldung", "Profil gespeichert");
        } catch (IllegalArgumentException e) {
            redirect.addFlashAttribute("fehlermeldung", e.getMessage());
        }
        return "redirect:/einstellungen";
    }

    @PostMapping("/profilbild")
    public String profilbildHochladen(@RequestParam("datei") MultipartFile datei,
                                       Authentication auth,
                                       RedirectAttributes redirect) {
        UUID userId = appUserService.findeIdNachEmail(auth.getName());
        try {
            UUID assetId = profilbildSpeicherung.speichereProfilbild(datei, userId);
            appUserService.setzeProfilbild(userId, assetId);
            redirect.addFlashAttribute("erfolgsMeldung", "Profilbild aktualisiert");
        } catch (Exception e) {
            redirect.addFlashAttribute("fehlermeldung", "Upload fehlgeschlagen: " + e.getMessage());
        }
        return "redirect:/einstellungen";
    }

    @GetMapping("/datenexport")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> datenExport(Authentication auth) {
        UUID userId = appUserService.findeIdNachEmail(auth.getName());
        Map<String, Object> export = datenExport.exportiere(userId);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"sponsorplatz-export.json\"")
                .contentType(MediaType.APPLICATION_JSON)
                .body(export);
    }

    @PostMapping("/passwort")
    public String passwortAendern(@RequestParam String altesPasswort,
                                   @RequestParam String neuesPasswort,
                                   Authentication auth,
                                   RedirectAttributes redirectAttributes) {
        UUID userId = appUserService.findeIdNachEmail(auth.getName());
        try {
            appUserService.aenderePasswort(userId, altesPasswort, neuesPasswort);
            redirectAttributes.addFlashAttribute("erfolgsMeldung", "Passwort erfolgreich geändert");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("fehlermeldung", e.getMessage());
        }
        return "redirect:/einstellungen";
    }

    private static final Map<String, String> VERFUEGBARE_SPRACHEN = Map.of(
            "de_CH", "Deutsch (Schweiz)",
            "de_DE", "Deutsch (Deutschland)",
            "fr_CH", "Français (Suisse)",
            "it_CH", "Italiano (Svizzera)",
            "en", "English"
    );

    /**
     * Synchronisiert den lang-Cookie sofort nach Profil-Speichern,
     * damit der naechste Seitenaufruf bereits in der neuen Sprache ist.
     */
    private void synchronisiereSprache(String sprache, HttpServletResponse response) {
        if (sprache != null && !sprache.isBlank()) {
            Cookie langCookie = new Cookie("lang", sprache.replace('_', '-'));
            langCookie.setPath("/");
            langCookie.setMaxAge(365 * 24 * 60 * 60);
            langCookie.setHttpOnly(false);
            response.addCookie(langCookie);
        }
    }
}
