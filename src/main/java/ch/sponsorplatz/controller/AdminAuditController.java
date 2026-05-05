package ch.sponsorplatz.controller;

import ch.sponsorplatz.config.ModelAttributeNames;
import ch.sponsorplatz.dto.AuditLogView;
import ch.sponsorplatz.model.AuditLog;
import ch.sponsorplatz.service.AuditService;
import ch.sponsorplatz.service.BackupService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

/**
 * Admin-Controller für Audit-Log und Backup-Verwaltung.
 */
@Controller
@RequestMapping("/admin")
@PreAuthorize("hasRole('PLATFORM_ADMIN')")
public class AdminAuditController {

    private final AuditService auditService;
    private final BackupService backupService;

    public AdminAuditController(AuditService auditService, BackupService backupService) {
        this.auditService = auditService;
        this.backupService = backupService;
    }

    // --- Audit-Log ---

    @GetMapping("/audit")
    public String auditLog(Model model) {
        List<AuditLog> eintraege = auditService.letzteEintraege();
        model.addAttribute(ModelAttributeNames.AKTIVE_SEITE, "admin");
        model.addAttribute("auditLogs", AuditLogView.von(eintraege));
        return "admin/audit";
    }

    // --- Backup ---

    @GetMapping("/backups")
    public String backupListe(Model model) {
        try {
            List<Path> backups = backupService.listeBackups();
            model.addAttribute(ModelAttributeNames.AKTIVE_SEITE, "admin");
            model.addAttribute("backups", backups.stream()
                    .map(p -> new BackupInfo(p.getFileName().toString(), formatGroesse(p)))
                    .toList());
        } catch (IOException e) {
            model.addAttribute(ModelAttributeNames.FEHLERMELDUNG, "Backup-Verzeichnis nicht lesbar: " + e.getMessage());
            model.addAttribute("backups", List.of());
        }
        return "admin/backups";
    }

    @PostMapping("/backups/erstellen")
    public String backupErstellen(RedirectAttributes redirect) {
        try {
            Path pfad = backupService.erstelleBackup();
            redirect.addFlashAttribute(ModelAttributeNames.ERFOLGS_MELDUNG,
                    "Backup erstellt: " + pfad.getFileName());
        } catch (Exception e) {
            redirect.addFlashAttribute(ModelAttributeNames.FEHLERMELDUNG,
                    "Backup fehlgeschlagen: " + e.getMessage());
        }
        return "redirect:/admin/backups";
    }

    private String formatGroesse(Path pfad) {
        try {
            long bytes = java.nio.file.Files.size(pfad);
            if (bytes < 1024) return bytes + " B";
            if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
            return String.format("%.1f MB", bytes / (1024.0 * 1024));
        } catch (IOException e) {
            return "?";
        }
    }

    public record BackupInfo(String dateiname, String groesse) {}
}

