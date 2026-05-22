package ch.sponsorplatz.admin;

import java.util.UUID;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import ch.sponsorplatz.audit.AuditAktion;
import ch.sponsorplatz.audit.AuditService;
import ch.sponsorplatz.benutzer.AdminBenutzerView;
import ch.sponsorplatz.benutzer.AppUserService;
import ch.sponsorplatz.benutzer.PlatformRolle;
import ch.sponsorplatz.benutzer.TwoFaService;
import ch.sponsorplatz.organisation.OrganisationService;
import ch.sponsorplatz.organisation.OrganisationView;
import ch.sponsorplatz.shared.config.ModelAttributeNames;

/**
 * Admin-Bereich: Benutzer- und Organisations-Verwaltung.
 */
@Controller
@RequestMapping("/admin")
@PreAuthorize("hasRole('PLATFORM_ADMIN')")
public class AdminBenutzerController {

    private final AppUserService appUserService;
    private final OrganisationService organisationService;
    private final AuditService auditService;
    private final TwoFaService twoFaService;

    public AdminBenutzerController(AppUserService appUserService,
            OrganisationService organisationService,
            AuditService auditService,
            TwoFaService twoFaService) {
        this.appUserService = appUserService;
        this.organisationService = organisationService;
        this.auditService = auditService;
        this.twoFaService = twoFaService;
    }

    // --- Benutzer-Verwaltung ---

    @GetMapping("/benutzer")
    public String benutzerListe(Model model) {
        model.addAttribute(ModelAttributeNames.AKTIVE_SEITE, "admin");
        model.addAttribute("benutzer", appUserService.findeAlleAdminViews());
        model.addAttribute("rollen", PlatformRolle.values());
        return "admin/benutzer";
    }

    @PostMapping("/benutzer/{id}/sperren")
    public String benutzerSperren(@PathVariable UUID id, RedirectAttributes redirect) {
        AdminBenutzerView v = appUserService.setzeAktiv(id, false);
        auditService.protokolliere(AuditAktion.GESPERRT, "BENUTZER", id, "AppUser", v.email());
        redirect.addFlashAttribute(ModelAttributeNames.ERFOLGS_MELDUNG,
                "Benutzer «" + v.anzeigename() + "» wurde gesperrt.");
        return "redirect:/admin/benutzer";
    }

    @PostMapping("/benutzer/{id}/entsperren")
    public String benutzerEntsperren(@PathVariable UUID id, RedirectAttributes redirect) {
        AdminBenutzerView v = appUserService.setzeAktiv(id, true);
        auditService.protokolliere(AuditAktion.ENTSPERRT, "BENUTZER", id, "AppUser", v.email());
        redirect.addFlashAttribute(ModelAttributeNames.ERFOLGS_MELDUNG,
                "Benutzer «" + v.anzeigename() + "» wurde entsperrt.");
        return "redirect:/admin/benutzer";
    }

    @PostMapping("/benutzer/{id}/rolle")
    public String rolleAendern(@PathVariable UUID id,
            @RequestParam(required = false) String rolle,
            RedirectAttributes redirect) {
        PlatformRolle neueRolle = (rolle == null || rolle.isBlank())
                ? null
                : PlatformRolle.valueOf(rolle);
        AdminBenutzerView vorher = appUserService.findeAdminViewNachId(id);
        String alteRolle = vorher.platformRolle() != null ? vorher.platformRolle().name() : "KEINE";
        AdminBenutzerView v = appUserService.setzePlatformRolle(id, neueRolle);
        auditService.protokolliere(AuditAktion.ROLLE_GEAENDERT, "BENUTZER", id, "AppUser",
                alteRolle + " → " + (rolle != null ? rolle : "KEINE"));
        redirect.addFlashAttribute(ModelAttributeNames.ERFOLGS_MELDUNG,
                "Rolle von «" + v.anzeigename() + "» geändert.");
        return "redirect:/admin/benutzer";
    }

    /**
     * Recovery: PLATFORM_ADMIN setzt 2FA eines anderen Users zurück, wenn
     * dieser Authenticator + alle Backup-Codes verloren hat. Idempotent —
     * funktioniert auch wenn 2FA bereits inaktiv ist.
     */
    @PostMapping("/benutzer/{id}/2fa-reset")
    public String zweiFaktorZuruecksetzen(@PathVariable UUID id, RedirectAttributes redirect) {
        twoFaService.adminResetFuerUser(id).ifPresentOrElse(ergebnis -> {
            String detail = "2FA-Reset (Recovery) für " + ergebnis.email()
                    + (ergebnis.warVorhAktiv() ? "" : " — war bereits inaktiv");
            auditService.protokolliere(AuditAktion.TOTP_RECOVERY_DURCH_ADMIN, "BENUTZER",
                    id, "AppUser", detail);
            redirect.addFlashAttribute(ModelAttributeNames.ERFOLGS_MELDUNG,
                    "2FA für «" + ergebnis.email() + "» zurückgesetzt. "
                            + "Der User loggt sich beim nächsten Mal nur mit Passwort ein "
                            + "und kann 2FA neu einrichten.");
        }, () -> redirect.addFlashAttribute(ModelAttributeNames.FEHLERMELDUNG,
                "Benutzer nicht gefunden."));
        return "redirect:/admin/benutzer";
    }

    // --- Organisations-Verwaltung ---

    @GetMapping("/organisationen")
    public String orgListe(Model model) {
        model.addAttribute(ModelAttributeNames.AKTIVE_SEITE, "admin");
        model.addAttribute("organisationen", OrganisationView.von(organisationService.alle()));
        return "admin/organisationen";
    }

    @PostMapping("/organisationen/{id}/verifizieren")
    public String orgVerifizieren(@PathVariable UUID id, RedirectAttributes redirect) {
        OrganisationView v = OrganisationView.von(organisationService.verifiziere(id));
        auditService.protokolliere(AuditAktion.VERIFIZIERT, "ORGANISATION", id, "Organisation", v.name());
        redirect.addFlashAttribute(ModelAttributeNames.ERFOLGS_MELDUNG,
                "Organisation «" + v.name() + "» verifiziert.");
        return "redirect:/admin/organisationen";
    }

    @PostMapping("/organisationen/{id}/suspendieren")
    public String orgSuspendieren(@PathVariable UUID id, RedirectAttributes redirect) {
        OrganisationView v = OrganisationView.von(organisationService.suspendiere(id));
        auditService.protokolliere(AuditAktion.SUSPENDIERT, "ORGANISATION", id, "Organisation", v.name());
        redirect.addFlashAttribute(ModelAttributeNames.ERFOLGS_MELDUNG,
                "Organisation «" + v.name() + "» suspendiert.");
        return "redirect:/admin/organisationen";
    }
}
