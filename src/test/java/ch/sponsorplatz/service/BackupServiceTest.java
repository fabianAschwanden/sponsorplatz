package ch.sponsorplatz.service;

import ch.sponsorplatz.repository.AuditLogRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import javax.sql.DataSource;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.Statement;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BackupServiceTest {

    @Mock private DataSource dataSource;
    @Mock private AuditService auditService;

    private BackupService service;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        service = new BackupService(dataSource, auditService);
        ReflectionTestUtils.setField(service, "backupVerzeichnis", tempDir.toString());
        ReflectionTestUtils.setField(service, "aufbewahrungTage", 30);
        ReflectionTestUtils.setField(service, "datasourceUrl", "jdbc:h2:file:./data/test");
    }

    @Test
    @DisplayName("BACKUP-01: H2-Backup erstellt Datei im konfigurierten Verzeichnis")
    void erstelltH2Backup() throws Exception {
        Connection conn = mock(Connection.class);
        Statement stmt = mock(Statement.class);
        when(dataSource.getConnection()).thenReturn(conn);
        when(conn.createStatement()).thenReturn(stmt);

        Path result = service.erstelleBackup();

        assertThat(result).isNotNull();
        assertThat(result.getFileName().toString()).startsWith("sponsorplatz_backup_");
        assertThat(result.getFileName().toString()).endsWith(".sql");
    }

    @Test
    @DisplayName("BACKUP-02: listeBackups gibt leere Liste für leeres Verzeichnis")
    void listeBackupsLeer() throws IOException {
        List<Path> result = service.listeBackups();
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("BACKUP-03: listeBackups findet vorhandene Backups")
    void listeBackupsFindet() throws IOException {
        Files.createFile(tempDir.resolve("sponsorplatz_backup_20260501_020000.sql"));
        Files.createFile(tempDir.resolve("sponsorplatz_backup_20260502_020000.sql"));
        Files.createFile(tempDir.resolve("andere_datei.txt")); // soll ignoriert werden

        List<Path> result = service.listeBackups();
        assertThat(result).hasSize(2);
    }
}

