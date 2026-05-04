package ch.sponsorplatz.controller;

import ch.sponsorplatz.exception.NotFoundException;
import ch.sponsorplatz.model.AppUser;
import ch.sponsorplatz.repository.AppUserRepository;
import ch.sponsorplatz.service.DatenExportService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.Map;

/**
 * DSG-Datenexport — ermöglicht eingeloggten Usern, ihre Daten als JSON herunterzuladen.
 */
@Controller
@RequestMapping("/einstellungen")
public class EinstellungenController {

    private final DatenExportService datenExportService;
    private final AppUserRepository appUserRepository;

    public EinstellungenController(DatenExportService datenExportService,
                                   AppUserRepository appUserRepository) {
        this.datenExportService = datenExportService;
        this.appUserRepository = appUserRepository;
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
}

