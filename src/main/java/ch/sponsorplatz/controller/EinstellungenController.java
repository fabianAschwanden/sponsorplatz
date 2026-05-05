package ch.sponsorplatz.controller;

import ch.sponsorplatz.exception.NotFoundException;
import ch.sponsorplatz.model.AppUser;
import ch.sponsorplatz.repository.AppUserRepository;
import ch.sponsorplatz.service.AppUserService;
import ch.sponsorplatz.service.DatenExportService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Map;

/**
 * Einstellungen — DSG-Datenexport und Passwort-Änderung.
 */
@Controller
@RequestMapping("/einstellungen")
public class EinstellungenController {

    private final DatenExportService datenExportService;
    private final AppUserRepository appUserRepository;
    private final AppUserService appUserService;

    public EinstellungenController(DatenExportService datenExportService,
                                   AppUserRepository appUserRepository,
                                   AppUserService appUserService) {
        this.datenExportService = datenExportService;
        this.appUserRepository = appUserRepository;
        this.appUserService = appUserService;
    }

    @GetMapping
    public String einstellungen(Model model) {
        model.addAttribute("aktiveSeite", "einstellungen");
        return "einstellungen";
    }

    @GetMapping("/datenexport")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> datenExport(Authentication auth) {
        AppUser user = appUserRepository.findByEmail(auth.getName())
                .orElseThrow(() -> new NotFoundException("User nicht gefunden"));

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
        AppUser user = appUserRepository.findByEmail(auth.getName())
                .orElseThrow(() -> new NotFoundException("User nicht gefunden"));

        try {
            appUserService.aenderePasswort(user.getId(), altesPasswort, neuesPasswort);
            redirectAttributes.addFlashAttribute("erfolgsMeldung", "Passwort erfolgreich geändert");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("fehlermeldung", e.getMessage());
        }

        return "redirect:/einstellungen";
    }
}

