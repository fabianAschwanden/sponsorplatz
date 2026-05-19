package ch.sponsorplatz.admin;

import ch.sponsorplatz.shared.config.ModelAttributeNames;
import ch.sponsorplatz.audit.AuditService;
import ch.sponsorplatz.backup.BackupRestoreService;
import ch.sponsorplatz.backup.BackupService;
import ch.sponsorplatz.backup.DateiBackupRestoreService;
import ch.sponsorplatz.backup.DateiBackupService;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
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
    private final BackupRestoreService restoreService;
    private final DateiBackupService dateiBackupService;
    private final DateiBackupRestoreService dateiBackupRestoreService;

    public AdminAuditController(AuditService auditService,
                                BackupService backupService,
                                BackupRestoreService restoreService,
                                DateiBackupService dateiBackupService,
                                DateiBackupRestoreService dateiBackupRestoreService) {
        this.auditService = auditService;
        this.backupService = backupService;
        this.restoreService = restoreService;
        this.dateiBackupService = dateiBackupService;
        this.dateiBackupRestoreService = dateiBackupRestoreService;
    }

    // --- Audit-Log ---

    @GetMapping("/audit")
    public String auditLog(Model model) {
        model.addAttribute(ModelAttributeNames.AKTIVE_SEITE, "admin");
        model.addAttribute("auditLogs", auditService.letzteEintraegeViews());
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

    @GetMapping("/backups/{dateiname}/download")
    public ResponseEntity<ByteArrayResource> backupDownload(@PathVariable String dateiname) throws IOException {
        byte[] bytes = backupService.leseBackup(dateiname);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("application/sql"))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + dateiname + "\"")
                .contentLength(bytes.length)
                .body(new ByteArrayResource(bytes));
    }

    @PostMapping("/backups/{dateiname}/loeschen")
    public String backupLoeschen(@PathVariable String dateiname, RedirectAttributes redirect) {
        try {
            backupService.loescheBackup(dateiname);
            redirect.addFlashAttribute(ModelAttributeNames.ERFOLGS_MELDUNG,
                    "Backup gelöscht: " + dateiname);
        } catch (Exception e) {
            redirect.addFlashAttribute(ModelAttributeNames.FEHLERMELDUNG,
                    "Löschen fehlgeschlagen: " + e.getMessage());
        }
        return "redirect:/admin/backups";
    }

    /**
     * Restore aus hochgeladenem SQL-Dump. Verlangt explizite Bestätigung
     * via getipptem "RESTORE" — destruktive Operation, alle aktuellen
     * Daten werden ersetzt.
     */
    @PostMapping("/backups/restore")
    public String backupRestore(@RequestParam("datei") MultipartFile datei,
                                @RequestParam("bestaetigung") String bestaetigung,
                                Authentication auth,
                                RedirectAttributes redirect) {
        if (!"RESTORE".equals(bestaetigung)) {
            redirect.addFlashAttribute(ModelAttributeNames.FEHLERMELDUNG,
                    "Restore abgebrochen — Bestätigungs-Text muss exakt RESTORE sein "
                            + "(als Schutz gegen versehentlichen Klick).");
            return "redirect:/admin/backups";
        }
        if (datei == null || datei.isEmpty()) {
            redirect.addFlashAttribute(ModelAttributeNames.FEHLERMELDUNG,
                    "Keine SQL-Datei gewählt.");
            return "redirect:/admin/backups";
        }
        try {
            String name = auth != null ? auth.getName() : "system";
            restoreService.restore(datei.getBytes(), name);
            redirect.addFlashAttribute(ModelAttributeNames.ERFOLGS_MELDUNG,
                    "Restore erfolgreich — " + datei.getOriginalFilename()
                            + " (" + datei.getSize() / 1024 + " KB) wurde eingespielt. "
                            + "Bitte ausloggen und erneut anmelden.");
        } catch (Exception e) {
            redirect.addFlashAttribute(ModelAttributeNames.FEHLERMELDUNG,
                    "Restore fehlgeschlagen: " + e.getMessage());
        }
        return "redirect:/admin/backups";
    }

    // --- Datei-Backup (Medien-Uploads als ZIP) ---

    @GetMapping("/datei-backups")
    public String dateiBackupListe(Model model) {
        try {
            List<Path> backups = dateiBackupService.listeDateiBackups();
            model.addAttribute(ModelAttributeNames.AKTIVE_SEITE, "admin");
            model.addAttribute("dateiBackups", backups.stream()
                    .map(p -> new BackupInfo(p.getFileName().toString(), formatGroesse(p)))
                    .toList());
        } catch (IOException e) {
            model.addAttribute(ModelAttributeNames.FEHLERMELDUNG,
                    "Datei-Backup-Verzeichnis nicht lesbar: " + e.getMessage());
            model.addAttribute("dateiBackups", List.of());
        }
        return "admin/datei-backups";
    }

    @PostMapping("/datei-backups/erstellen")
    public String dateiBackupErstellen(RedirectAttributes redirect) {
        try {
            Path pfad = dateiBackupService.erstelleDateiBackup();
            redirect.addFlashAttribute(ModelAttributeNames.ERFOLGS_MELDUNG,
                    "Datei-Backup erstellt: " + pfad.getFileName());
        } catch (Exception e) {
            redirect.addFlashAttribute(ModelAttributeNames.FEHLERMELDUNG,
                    "Datei-Backup fehlgeschlagen: " + e.getMessage());
        }
        return "redirect:/admin/datei-backups";
    }

    @GetMapping("/datei-backups/{dateiname}/download")
    public ResponseEntity<ByteArrayResource> dateiBackupDownload(@PathVariable String dateiname) throws IOException {
        byte[] bytes = dateiBackupService.leseDateiBackup(dateiname);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("application/zip"))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + dateiname + "\"")
                .contentLength(bytes.length)
                .body(new ByteArrayResource(bytes));
    }

    @PostMapping("/datei-backups/{dateiname}/loeschen")
    public String dateiBackupLoeschen(@PathVariable String dateiname, RedirectAttributes redirect) {
        try {
            dateiBackupService.loescheDateiBackup(dateiname);
            redirect.addFlashAttribute(ModelAttributeNames.ERFOLGS_MELDUNG,
                    "Datei-Backup gelöscht: " + dateiname);
        } catch (Exception e) {
            redirect.addFlashAttribute(ModelAttributeNames.FEHLERMELDUNG,
                    "Löschen fehlgeschlagen: " + e.getMessage());
        }
        return "redirect:/admin/datei-backups";
    }

    /**
     * Restore aus hochgeladenem ZIP. Wie beim DB-Restore verlangt eine
     * explizite Bestätigung — vorhandene Storage-Objekte werden überschrieben.
     */
    @PostMapping("/datei-backups/restore")
    public String dateiBackupRestore(@RequestParam("datei") MultipartFile datei,
                                     @RequestParam("bestaetigung") String bestaetigung,
                                     Authentication auth,
                                     RedirectAttributes redirect) {
        if (!"RESTORE".equals(bestaetigung)) {
            redirect.addFlashAttribute(ModelAttributeNames.FEHLERMELDUNG,
                    "Restore abgebrochen — Bestätigungs-Text muss exakt RESTORE sein.");
            return "redirect:/admin/datei-backups";
        }
        if (datei == null || datei.isEmpty()) {
            redirect.addFlashAttribute(ModelAttributeNames.FEHLERMELDUNG,
                    "Keine ZIP-Datei gewählt.");
            return "redirect:/admin/datei-backups";
        }
        try {
            String name = auth != null ? auth.getName() : "system";
            DateiBackupRestoreService.RestoreReport report =
                    dateiBackupRestoreService.restore(datei.getBytes(), name);
            redirect.addFlashAttribute(ModelAttributeNames.ERFOLGS_MELDUNG,
                    "Datei-Restore erfolgreich — " + report.restored() + " Dateien wiederhergestellt"
                            + (report.skipped() > 0 ? ", " + report.skipped() + " skipped" : "") + ".");
        } catch (Exception e) {
            redirect.addFlashAttribute(ModelAttributeNames.FEHLERMELDUNG,
                    "Datei-Restore fehlgeschlagen: " + e.getMessage());
        }
        return "redirect:/admin/datei-backups";
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

