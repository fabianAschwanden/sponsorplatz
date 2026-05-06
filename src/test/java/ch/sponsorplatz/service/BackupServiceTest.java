package ch.sponsorplatz.service;

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
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BackupServiceTest {

    @Mock private DataSource dataSource;
    @Mock private AuditService auditService;
    @Mock private BackupCloudUploader cloudUploader;

    private BackupService service;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        service = new BackupService(dataSource, auditService, Optional.empty());
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

    @Test
    @DisplayName("BACKUP-04: Backup wird in Cloud hochgeladen, wenn Uploader registriert")
    void backupWirdInCloudHochgeladen() throws Exception {
        service = new BackupService(dataSource, auditService, Optional.of(cloudUploader));
        ReflectionTestUtils.setField(service, "backupVerzeichnis", tempDir.toString());
        ReflectionTestUtils.setField(service, "aufbewahrungTage", 30);
        ReflectionTestUtils.setField(service, "datasourceUrl", "jdbc:h2:file:./data/test");
        Connection conn = mock(Connection.class);
        Statement stmt = mock(Statement.class);
        when(dataSource.getConnection()).thenReturn(conn);
        when(conn.createStatement()).thenReturn(stmt);
        when(cloudUploader.lade(any(Path.class))).thenReturn("backups/x.sql");

        Path result = service.erstelleBackup();

        verify(cloudUploader).lade(result);
    }

    @Test
    @DisplayName("BACKUP-05: Cloud-Upload-Fehler bricht Backup nicht ab")
    void cloudUploadFehlerSchluckt() throws Exception {
        service = new BackupService(dataSource, auditService, Optional.of(cloudUploader));
        ReflectionTestUtils.setField(service, "backupVerzeichnis", tempDir.toString());
        ReflectionTestUtils.setField(service, "aufbewahrungTage", 30);
        ReflectionTestUtils.setField(service, "datasourceUrl", "jdbc:h2:file:./data/test");
        Connection conn = mock(Connection.class);
        Statement stmt = mock(Statement.class);
        when(dataSource.getConnection()).thenReturn(conn);
        when(conn.createStatement()).thenReturn(stmt);
        when(cloudUploader.lade(any(Path.class)))
                .thenThrow(new RuntimeException("OCI down"));

        Path result = service.erstelleBackup();

        assertThat(result).isNotNull();
    }

    @Test
    @DisplayName("BACKUP-06: Ohne Uploader wird kein Cloud-Call gemacht")
    void ohneUploaderKeinCall() throws Exception {
        Connection conn = mock(Connection.class);
        Statement stmt = mock(Statement.class);
        when(dataSource.getConnection()).thenReturn(conn);
        when(conn.createStatement()).thenReturn(stmt);

        service.erstelleBackup();

        verify(cloudUploader, never()).lade(any(Path.class));
    }
}

