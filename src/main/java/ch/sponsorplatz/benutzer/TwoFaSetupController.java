package ch.sponsorplatz.benutzer;

import ch.sponsorplatz.shared.config.ModelAttributeNames;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Optional;

/**
 * Setup-Flow für 2-Faktor-Authentifizierung (TOTP) — Phase 13.2.
 *
 * <p>Controller-Schicht: ausschliesslich HTTP-Verdrahtung + Session-State.
 * Alle 2FA-Operationen laufen über {@link TwoFaService} mit record-DTOs —
 * kein direkter Repository- oder Entity-Zugriff (ARCH-01 + ARCH-02).
 *
 * Spec: {@code specs/AUTH_2FA_TOTP.md}. Tests AUTH-2FA-06..09.
 */
@Controller
@RequestMapping("/einstellungen/2fa")
public class TwoFaSetupController {

    /** Session-Key für das unbestätigte Secret während des Setup-Flows. */
    static final String SESSION_PENDING_SECRET = "totp.pending.secret";
    /** Flash-Key für die einmalige Anzeige frischer Backup-Codes. */
    static final String FLASH_NEW_BACKUP_CODES = "neueBackupCodes";

    private final TwoFaService twoFaService;

    public TwoFaSetupController(TwoFaService twoFaService) {
        this.twoFaService = twoFaService;
    }

    @GetMapping
    public String anzeigen(Authentication auth, Model model, HttpSession session) {
        String email = userEmail(auth);
        TwoFaService.TwoFaStatus status = twoFaService.findStatus(email);
        model.addAttribute(ModelAttributeNames.AKTIVE_SEITE, "einstellungen");

        if (status.aktiv()) {
            model.addAttribute("totpAktiv", true);
            model.addAttribute("aktiviertAm", status.aktiviertAm());
            model.addAttribute("backupCodesVerbleibend", status.backupCodesVerbleibend());
            return "benutzer/2fa-status";
        }

        String pending = (String) session.getAttribute(SESSION_PENDING_SECRET);
        TwoFaService.TwoFaSetupContext ctx = twoFaService.bereiteSetupVor(email, pending);
        session.setAttribute(SESSION_PENDING_SECRET, ctx.pendingSecret());
        model.addAttribute("totpAktiv", false);
        model.addAttribute("manuellerCode", ctx.manuellerCode());
        model.addAttribute("qrDataUrl", ctx.qrDataUrl());
        return "benutzer/2fa-setup";
    }

    @PostMapping("/aktivieren")
    public String aktivieren(@RequestParam String code,
                             Authentication auth,
                             HttpSession session,
                             RedirectAttributes redirect) {
        String pending = (String) session.getAttribute(SESSION_PENDING_SECRET);
        TwoFaService.TwoFaAktivierungsErgebnis ergebnis =
                twoFaService.aktivieren(userEmail(auth), pending, code);

        if (ergebnis.bereitsAktiv()) {
            return "redirect:/einstellungen/2fa";
        }
        if (!ergebnis.erfolgreich()) {
            redirect.addFlashAttribute(ModelAttributeNames.FEHLERMELDUNG,
                    "Code falsch oder Setup-Session abgelaufen — bitte QR-Code neu scannen.");
            return "redirect:/einstellungen/2fa";
        }
        session.removeAttribute(SESSION_PENDING_SECRET);
        redirect.addFlashAttribute(ModelAttributeNames.ERFOLGS_MELDUNG,
                "2FA aktiviert — bitte Backup-Codes JETZT sicher speichern. Sie werden nicht mehr angezeigt.");
        redirect.addFlashAttribute(FLASH_NEW_BACKUP_CODES, ergebnis.neueBackupCodes());
        return "redirect:/einstellungen/2fa";
    }

    @PostMapping("/deaktivieren")
    public String deaktivieren(@RequestParam String aktuellesPasswort,
                               @RequestParam String code,
                               Authentication auth,
                               RedirectAttributes redirect) {
        boolean ok = twoFaService.deaktivieren(userEmail(auth), aktuellesPasswort, code);
        if (!ok) {
            redirect.addFlashAttribute(ModelAttributeNames.FEHLERMELDUNG,
                    "Aktuelles Passwort oder TOTP-Code falsch — Deaktivierung abgebrochen.");
            return "redirect:/einstellungen/2fa";
        }
        redirect.addFlashAttribute(ModelAttributeNames.ERFOLGS_MELDUNG,
                "2FA deaktiviert. Setup kann jederzeit neu durchgeführt werden.");
        return "redirect:/einstellungen/2fa";
    }

    @PostMapping("/backup-codes/regenerieren")
    public String backupCodesNeu(@RequestParam String code,
                                 Authentication auth,
                                 RedirectAttributes redirect) {
        Optional<List<String>> codes = twoFaService.regeneriereBackupCodes(userEmail(auth), code);
        if (codes.isEmpty()) {
            redirect.addFlashAttribute(ModelAttributeNames.FEHLERMELDUNG,
                    "TOTP-Code falsch oder 2FA nicht aktiv — Backup-Codes nicht regeneriert.");
            return "redirect:/einstellungen/2fa";
        }
        redirect.addFlashAttribute(ModelAttributeNames.ERFOLGS_MELDUNG,
                "Neue Backup-Codes erzeugt — bitte JETZT sicher speichern. Alte Codes sind ab sofort ungültig.");
        redirect.addFlashAttribute(FLASH_NEW_BACKUP_CODES, codes.get());
        return "redirect:/einstellungen/2fa";
    }

    private static String userEmail(Authentication auth) {
        if (auth == null || auth.getName() == null) {
            throw new IllegalStateException("Kein eingeloggter Benutzer im Security-Context");
        }
        return auth.getName();
    }
}
