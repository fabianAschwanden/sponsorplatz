package ch.sponsorplatz.admin;

import ch.sponsorplatz.audit.AuditService;
import ch.sponsorplatz.backup.BackupRestoreService;
import ch.sponsorplatz.backup.BackupService;
import ch.sponsorplatz.backup.DateiBackupRestoreService;
import ch.sponsorplatz.backup.DateiBackupService;
import ch.sponsorplatz.benutzer.SponsorplatzUserDetailsService;
import ch.sponsorplatz.shared.config.SecurityConfig;
import ch.sponsorplatz.shared.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.file.Path;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Controller-Tests für {@link AdminAuditController}.
 * Test-IDs: AAUDIT-01..08
 */
@WebMvcTest(controllers = AdminAuditController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class})
@ActiveProfiles("dev")
class AdminAuditControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuditService auditService;

    @MockitoBean
    private BackupService backupService;

    @MockitoBean
    private BackupRestoreService restoreService;

    @MockitoBean
    private DateiBackupService dateiBackupService;

    @MockitoBean
    private DateiBackupRestoreService dateiBackupRestoreService;

    @MockitoBean
    private SponsorplatzUserDetailsService userDetailsService;

    @Test
    @WithMockUser(roles = "PLATFORM_ADMIN")
    @DisplayName("AAUDIT-01: GET /admin/audit zeigt Audit-Log")
    void auditLog() throws Exception {
        when(auditService.letzteEintraegeViews()).thenReturn(List.of());

        mockMvc.perform(get("/admin/audit"))
                .andExpect(status().isOk())
                .andExpect(model().attributeExists("auditLogs"))
                .andExpect(view().name("admin/audit"));
    }

    @Test
    @WithMockUser(roles = "PLATFORM_ADMIN")
    @DisplayName("AAUDIT-02: GET /admin/backups zeigt Backup-Liste")
    void backupListe() throws Exception {
        when(backupService.listeBackups()).thenReturn(List.of());

        mockMvc.perform(get("/admin/backups"))
                .andExpect(status().isOk())
                .andExpect(model().attributeExists("backups"))
                .andExpect(view().name("admin/backups"));
    }

    @Test
    @WithMockUser(roles = "PLATFORM_ADMIN")
    @DisplayName("AAUDIT-03: POST /admin/backups/erstellen erstellt Backup")
    void backupErstellen() throws Exception {
        when(backupService.erstelleBackup()).thenReturn(Path.of("/tmp/backup.sql"));

        mockMvc.perform(post("/admin/backups/erstellen").with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attributeExists("erfolgsMeldung"));

        verify(backupService).erstelleBackup();
    }

    @Test
    @WithMockUser(roles = "PLATFORM_ADMIN")
    @DisplayName("AAUDIT-04: GET /admin/backups/{dateiname}/download liefert SQL-Datei")
    void backupDownload() throws Exception {
        when(backupService.leseBackup("backup-2026.sql")).thenReturn("-- SQL".getBytes());

        mockMvc.perform(get("/admin/backups/{dateiname}/download", "backup-2026.sql"))
                .andExpect(status().isOk())
                .andExpect(header().exists("Content-Disposition"));
    }

    @Test
    @WithMockUser(roles = "PLATFORM_ADMIN")
    @DisplayName("AAUDIT-05: POST /admin/backups/{dateiname}/loeschen entfernt Backup")
    void backupLoeschen() throws Exception {
        mockMvc.perform(post("/admin/backups/{dateiname}/loeschen", "old.sql").with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attributeExists("erfolgsMeldung"));

        verify(backupService).loescheBackup("old.sql");
    }

    @Test
    @WithMockUser(roles = "PLATFORM_ADMIN")
    @DisplayName("AAUDIT-06: POST /admin/backups/restore ohne RESTORE-Bestätigung → Fehler")
    void restoreOhneBestaetigung() throws Exception {
        MockMultipartFile datei = new MockMultipartFile("datei", "dump.sql",
                "application/sql", "-- SQL".getBytes());

        mockMvc.perform(multipart("/admin/backups/restore")
                        .file(datei)
                        .param("bestaetigung", "FALSCH")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attributeExists("fehlermeldung"));
    }

    @Test
    @WithMockUser(roles = "PLATFORM_ADMIN")
    @DisplayName("AAUDIT-07: POST /admin/backups/restore mit RESTORE → Erfolg")
    void restoreMitBestaetigung() throws Exception {
        MockMultipartFile datei = new MockMultipartFile("datei", "dump.sql",
                "application/sql", "-- SQL RESTORE".getBytes());

        mockMvc.perform(multipart("/admin/backups/restore")
                        .file(datei)
                        .param("bestaetigung", "RESTORE")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attributeExists("erfolgsMeldung"));

        verify(restoreService).restore(any(), any());
    }

    @Test
    @WithMockUser
    @DisplayName("AAUDIT-08: Nicht-Admin bekommt 403")
    void nichtAdminVerboten() throws Exception {
        mockMvc.perform(get("/admin/audit"))
                .andExpect(status().isForbidden());
    }
}

