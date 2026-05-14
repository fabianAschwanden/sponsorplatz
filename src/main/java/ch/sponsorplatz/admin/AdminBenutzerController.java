package ch.sponsorplatz.admin;

import java.util.List;
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
import ch.sponsorplatz.benutzer.AppUser;
import ch.sponsorplatz.benutzer.AppUserService;
import ch.sponsorplatz.benutzer.PlatformRolle;
import ch.sponsorplatz.organisation.Organisation;
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

    public AdminBenutzerController(AppUserService appUserService,
            OrganisationService organisationService,
            AuditService auditService) {
        this.appUserService = appUserService;
        this.organisationService = organisationService;
        this.auditService = auditService;
    }

    // --- Benutzer-Verwaltung ---

    @GetMapping("/benutzer")
    public String benutzerListe(Model model) {
        List<AppUser> users = appUserService.findeAlleNeuesteZuerst();
        model.addAttribute(ModelAttributeNames.AKTIVE_SEITE, "admin");
        model.addAttribute("benutzer", AdminBenutzerView.von(users));
        model.addAttribute("rollen", PlatformRolle.values());
        return "admin/benutzer";
    }

    @PostMapping("/benutzer/{id}/sperren")
    public String benutzerSperren(@PathVariable UUID id, RedirectAttributes redirect) {
        AppUser user = appUserService.setzeAktiv(id, false);
        auditService.protokolliere(AuditAktion.GESPERRT, "BENUTZER", id, "AppUser", user.getEmail());
        redirect.addFlashAttribute(ModelAttributeNames.ERFOLGS_MELDUNG,
                "Benutzer «" + user.getAnzeigename() + "» wurde gesperrt.");
        return "redirect:/admin/benutzer";
    }

    @PostMapping("/benutzer/{id}/entsperren")
    public String benutzerEntsperren(@PathVariable UUID id, RedirectAttributes redirect) {
        AppUser user = appUserService.setzeAktiv(id, true);
        auditService.protokolliere(AuditAktion.ENTSPERRT, "BENUTZER", id, "AppUser", user.getEmail());
        redirect.addFlashAttribute(ModelAttributeNames.ERFOLGS_MELDUNG,
                "Benutzer «" + user.getAnzeigename() + "» wurde entsperrt.");
        return "redirect:/admin/benutzer";
    }

    @PostMapping("/benutzer/{id}/rolle")
    public String rolleAendern(@PathVariable UUID id,
            @RequestParam(required = false) String rolle,
            RedirectAttributes redirect) {
        PlatformRolle neueRolle = (rolle == null || rolle.isBlank())
                ? null
                : PlatformRolle.valueOf(rolle);
        AppUser vorher = appUserService.findeNachId(id).orElseThrow();
        String alteRolle = vorher.getPlatformRolle() != null ? vorher.getPlatformRolle().name() : "KEINE";
        AppUser user = appUserService.setzePlatformRolle(id, neueRolle);
        auditService.protokolliere(AuditAktion.ROLLE_GEAENDERT, "BENUTZER", id, "AppUser",
                alteRolle + " → " + (rolle != null ? rolle : "KEINE"));
        redirect.addFlashAttribute(ModelAttributeNames.ERFOLGS_MELDUNG,
                "Rolle von «" + user.getAnzeigename() + "» geändert.");
        return "redirect:/admin/benutzer";
    }

    // --- Organisations-Verwaltung ---

    @GetMapping("/organisationen")
    public String orgListe(Model model) {
        List<Organisation> orgs = organisationService.alle();
        model.addAttribute(ModelAttributeNames.AKTIVE_SEITE, "admin");
        model.addAttribute("organisationen", OrganisationView.von(orgs));
        return "admin/organisationen";
    }

    @PostMapping("/organisationen/{id}/verifizieren")
    public String orgVerifizieren(@PathVariable UUID id, RedirectAttributes redirect) {
        Organisation org = organisationService.verifiziere(id);
        auditService.protokolliere(AuditAktion.VERIFIZIERT, "ORGANISATION", id, "Organisation", org.getName());
        redirect.addFlashAttribute(ModelAttributeNames.ERFOLGS_MELDUNG,
                "Organisation «" + org.getName() + "» verifiziert.");
        return "redirect:/admin/organisationen";
    }

    @PostMapping("/organisationen/{id}/suspendieren")
    public String orgSuspendieren(@PathVariable UUID id, RedirectAttributes redirect) {
        Organisation org = organisationService.suspendiere(id);
        auditService.protokolliere(AuditAktion.SUSPENDIERT, "ORGANISATION", id, "Organisation", org.getName());
        redirect.addFlashAttribute(ModelAttributeNames.ERFOLGS_MELDUNG,
                "Organisation «" + org.getName() + "» suspendiert.");
        return "redirect:/admin/organisationen";
    }
}
