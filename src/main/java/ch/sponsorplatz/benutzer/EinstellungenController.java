package ch.sponsorplatz.benutzer;

import ch.sponsorplatz.shared.exception.NotFoundException;
import ch.sponsorplatz.model.AssetTyp;
import ch.sponsorplatz.model.EntityTyp;
import ch.sponsorplatz.service.DatenExportService;
import ch.sponsorplatz.service.MedienAssetService;
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

/**
 * Einstellungen — Profil, Passwort, DSG-Datenexport und Profilbild-Upload.
 */
@Controller
@RequestMapping("/einstellungen")
public class EinstellungenController {

    private final DatenExportService datenExportService;
    private final AppUserRepository appUserRepository;
    private final AppUserService appUserService;
    private final MedienAssetService medienAssetService;

    public EinstellungenController(DatenExportService datenExportService,
                                   AppUserRepository appUserRepository,
                                   AppUserService appUserService,
                                   MedienAssetService medienAssetService) {
        this.datenExportService = datenExportService;
        this.appUserRepository = appUserRepository;
        this.appUserService = appUserService;
        this.medienAssetService = medienAssetService;
    }

    @GetMapping
    public String einstellungen(Authentication auth, Model model) {
        AppUser user = ladeUser(auth);
        model.addAttribute("aktiveSeite", "einstellungen");
        model.addAttribute("profil", ProfilView.von(user));
        model.addAttribute("profilForm", erstelleFormVonUser(user));
        model.addAttribute("sprachen", VERFUEGBARE_SPRACHEN);
        return "einstellungen";
    }

    @PostMapping("/profil")
    public String profilSpeichern(@Valid @ModelAttribute("profilForm") ProfilFormDto dto,
                                   BindingResult br,
                                   Authentication auth,
                                   Model model,
                                   RedirectAttributes redirect) {
        if (br.hasErrors()) {
            AppUser user = ladeUser(auth);
            model.addAttribute("aktiveSeite", "einstellungen");
            model.addAttribute("profil", ProfilView.von(user));
            model.addAttribute("sprachen", VERFUEGBARE_SPRACHEN);
            return "einstellungen";
        }

        AppUser user = ladeUser(auth);
        try {
            appUserService.aktualisiereProfil(user.getId(), dto);
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
        AppUser user = ladeUser(auth);
        try {
            var asset = medienAssetService.speichere(datei, EntityTyp.USER, user.getId(), AssetTyp.PROFILBILD);
            appUserService.setzeProfilbild(user.getId(), asset.getId());
            redirect.addFlashAttribute("erfolgsMeldung", "Profilbild aktualisiert");
        } catch (Exception e) {
            redirect.addFlashAttribute("fehlermeldung", "Upload fehlgeschlagen: " + e.getMessage());
        }
        return "redirect:/einstellungen";
    }

    @GetMapping("/datenexport")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> datenExport(Authentication auth) {
        AppUser user = ladeUser(auth);
        Map<String, Object> export = datenExportService.exportiere(user.getId());
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
        AppUser user = ladeUser(auth);
        try {
            appUserService.aenderePasswort(user.getId(), altesPasswort, neuesPasswort);
            redirectAttributes.addFlashAttribute("erfolgsMeldung", "Passwort erfolgreich geändert");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("fehlermeldung", e.getMessage());
        }
        return "redirect:/einstellungen";
    }

    private AppUser ladeUser(Authentication auth) {
        return appUserRepository.findByEmail(auth.getName())
                .orElseThrow(() -> new NotFoundException("User nicht gefunden"));
    }

    private ProfilFormDto erstelleFormVonUser(AppUser user) {
        ProfilFormDto dto = new ProfilFormDto();
        dto.setAnzeigename(user.getAnzeigename());
        dto.setSprache(user.getSprache());
        dto.setTelefon(user.getTelefon());
        dto.setBio(user.getBio());
        dto.setOrt(user.getOrt());
        dto.setWebsiteUrl(user.getWebsiteUrl());
        dto.setPositionTitel(user.getPositionTitel());
        return dto;
    }

    private static final Map<String, String> VERFUEGBARE_SPRACHEN = Map.of(
            "de_CH", "Deutsch (Schweiz)",
            "de_DE", "Deutsch (Deutschland)",
            "fr_CH", "Français (Suisse)",
            "it_CH", "Italiano (Svizzera)",
            "en", "English"
    );
}

