package ch.sponsorplatz.controller;

import ch.sponsorplatz.shared.config.ModelAttributeNames;
import ch.sponsorplatz.benutzer.AdminBenutzerView;
import ch.sponsorplatz.dto.OrganisationView;
import ch.sponsorplatz.shared.exception.NotFoundException;
import ch.sponsorplatz.benutzer.AppUser;
import ch.sponsorplatz.model.AuditAktion;
import ch.sponsorplatz.model.Organisation;
import ch.sponsorplatz.benutzer.PlatformRolle;
import ch.sponsorplatz.benutzer.AppUserRepository;
import ch.sponsorplatz.service.AuditService;
import ch.sponsorplatz.service.OrganisationService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.UUID;

/**
 * Admin-Bereich: Benutzer- und Organisations-Verwaltung.
 */
@Controller
@RequestMapping("/admin")
@PreAuthorize("hasRole('PLATFORM_ADMIN')")
public class AdminBenutzerController {

    private final AppUserRepository userRepository;
    private final OrganisationService organisationService;
    private final AuditService auditService;

    public AdminBenutzerController(AppUserRepository userRepository,
                                    OrganisationService organisationService,
                                    AuditService auditService) {
        this.userRepository = userRepository;
        this.organisationService = organisationService;
        this.auditService = auditService;
    }

    // --- Benutzer-Verwaltung ---

    @GetMapping("/benutzer")
    public String benutzerListe(Model model) {
        List<AppUser> users = userRepository.findAllByOrderByRegistriertAmDesc();
        model.addAttribute(ModelAttributeNames.AKTIVE_SEITE, "admin");
        model.addAttribute("benutzer", AdminBenutzerView.von(users));
        model.addAttribute("rollen", PlatformRolle.values());
        return "admin/benutzer";
    }

    @PostMapping("/benutzer/{id}/sperren")
    public String benutzerSperren(@PathVariable UUID id, RedirectAttributes redirect) {
        AppUser user = findeUser(id);
        user.setAktiv(false);
        userRepository.save(user);
        auditService.protokolliere(AuditAktion.GESPERRT, "BENUTZER", id, "AppUser", user.getEmail());
        redirect.addFlashAttribute(ModelAttributeNames.ERFOLGS_MELDUNG,
                "Benutzer «" + user.getAnzeigename() + "» wurde gesperrt.");
        return "redirect:/admin/benutzer";
    }

    @PostMapping("/benutzer/{id}/entsperren")
    public String benutzerEntsperren(@PathVariable UUID id, RedirectAttributes redirect) {
        AppUser user = findeUser(id);
        user.setAktiv(true);
        userRepository.save(user);
        auditService.protokolliere(AuditAktion.ENTSPERRT, "BENUTZER", id, "AppUser", user.getEmail());
        redirect.addFlashAttribute(ModelAttributeNames.ERFOLGS_MELDUNG,
                "Benutzer «" + user.getAnzeigename() + "» wurde entsperrt.");
        return "redirect:/admin/benutzer";
    }

    @PostMapping("/benutzer/{id}/rolle")
    public String rolleAendern(@PathVariable UUID id,
                               @RequestParam(required = false) String rolle,
                               RedirectAttributes redirect) {
        AppUser user = findeUser(id);
        String alteRolle = user.getPlatformRolle() != null ? user.getPlatformRolle().name() : "KEINE";
        if (rolle == null || rolle.isBlank()) {
            user.setPlatformRolle(null);
        } else {
            user.setPlatformRolle(PlatformRolle.valueOf(rolle));
        }
        userRepository.save(user);
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

    private AppUser findeUser(UUID id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Benutzer nicht gefunden: " + id));
    }
}

