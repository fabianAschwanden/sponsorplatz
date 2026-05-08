package ch.sponsorplatz.controller;
import ch.sponsorplatz.model.Einladung;

import ch.sponsorplatz.dto.EinladungVorschauView;
import ch.sponsorplatz.shared.exception.BenutzerNichtRegistriertException;
import ch.sponsorplatz.service.EinladungsService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * Mitglieder-Einladungs-Annahme — bewusst als 2-Step-Pattern (K3-Fix):
 *
 * <ul>
 *   <li>{@code GET /einladung/annehmen?token=…} ist <strong>safe</strong> (idempotent, keine
 *       State-Änderung) — zeigt nur eine Vorschau-Seite mit Bestätigungs-Button.</li>
 *   <li>{@code POST /einladung/annehmen} mit Token im Body führt die Annahme aus
 *       (CSRF-geschützt).</li>
 * </ul>
 *
 * Damit konsumieren Outlook-Safe-Link, Slack-Link-Preview, MS-Defender u.ä.
 * den Einladungs-Token nicht versehentlich, bevor der eingeladene User klickt.
 */
@Controller
@RequestMapping("/einladung/annehmen")
public class EinladungsController {

    private final EinladungsService einladungsService;

    public EinladungsController(EinladungsService einladungsService) {
        this.einladungsService = einladungsService;
    }

    /** GET ist safe — zeigt nur die Vorschau, ruft niemals {@code nimmAn} auf. */
    @GetMapping
    public String vorschau(@RequestParam String token, Model model) {
        EinladungVorschauView vorschau = einladungsService.ladeVorschau(token);
        model.addAttribute("vorschau", vorschau);
        return "einladung-vorschau";
    }

    /** POST führt die Annahme aus — User hat die Vorschau-Page bestätigt. */
    @PostMapping
    public String annehmen(@RequestParam String token, Model model) {
        try {
            einladungsService.nimmAn(token);
        } catch (BenutzerNichtRegistriertException ex) {
            // M3-Fix: User muss sich erst registrieren — Redirect mit pre-filled E-Mail
            String email = URLEncoder.encode(ex.getEmail(), StandardCharsets.UTF_8);
            return "redirect:/registrieren?email=" + email + "&einladung=offen";
        }
        model.addAttribute("erfolgsMeldung", "Einladung angenommen! Sie sind jetzt Mitglied.");
        return "einladung-erfolg";
    }
}
