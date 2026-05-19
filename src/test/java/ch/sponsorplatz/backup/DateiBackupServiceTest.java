package ch.sponsorplatz.backup;

import ch.sponsorplatz.audit.AuditAktion;
import ch.sponsorplatz.audit.AuditService;
import ch.sponsorplatz.projekt.MedienAsset;
import ch.sponsorplatz.projekt.MedienAssetRepository;
import ch.sponsorplatz.shared.storage.StorageObjectNotFoundException;
import ch.sponsorplatz.shared.storage.StorageService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit-Tests für {@link DateiBackupService}.
 *
 * Test-IDs: DATEI-BACKUP-01..05 in {@code specs/TESTSTRATEGIE.md}.
 */
@ExtendWith(MockitoExtension.class)
class DateiBackupServiceTest {

    @Mock private MedienAssetRepository medienAssetRepository;
    @Mock private StorageService storageService;
    @Mock private AuditService auditService;

    private DateiBackupService service;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        service = new DateiBackupService(medienAssetRepository, storageService, auditService);
        ReflectionTestUtils.setField(service, "backupVerzeichnis", tempDir.toString());
    }

    @Test
    @DisplayName("DATEI-BACKUP-01: erstelleDateiBackup baut ZIP mit allen Asset-Bytes; Pfad = Storage-Key")
    void erstelltZipMitAllenAssets() throws IOException {
        MedienAsset a1 = asset("user/abc/logo.png", "image/png");
        MedienAsset a2 = asset("organisation/xyz/cover.jpg", "image/jpeg");
        when(medienAssetRepository.findAll()).thenReturn(List.of(a1, a2));
        when(storageService.ladeAlsResource("user/abc/logo.png"))
                .thenReturn(new ByteArrayResource(new byte[]{1, 2, 3}));
        when(storageService.ladeAlsResource("organisation/xyz/cover.jpg"))
                .thenReturn(new ByteArrayResource(new byte[]{4, 5, 6, 7}));

        Path zipPfad = service.erstelleDateiBackup();

        assertThat(zipPfad.getFileName().toString())
                .startsWith("sponsorplatz_uploads_").endsWith(".zip");
        Map<String, byte[]> entries = leseZipEintraege(zipPfad);
        assertThat(entries).hasSize(2);
        assertThat(entries.get("user/abc/logo.png")).containsExactly(1, 2, 3);
        assertThat(entries.get("organisation/xyz/cover.jpg")).containsExactly(4, 5, 6, 7);
    }

    @Test
    @DisplayName("DATEI-BACKUP-02: orphaned Asset (Storage-404) wird übersprungen, Backup läuft weiter")
    void orphanWirdUebersprungen() throws IOException {
        MedienAsset live = asset("user/abc/echte.png", "image/png");
        MedienAsset orphan = asset("user/abc/weg.png", "image/png");
        when(medienAssetRepository.findAll()).thenReturn(List.of(live, orphan));
        when(storageService.ladeAlsResource("user/abc/echte.png"))
                .thenReturn(new ByteArrayResource(new byte[]{1}));
        when(storageService.ladeAlsResource("user/abc/weg.png"))
                .thenThrow(new StorageObjectNotFoundException("user/abc/weg.png"));

        Path zipPfad = service.erstelleDateiBackup();

        Map<String, byte[]> entries = leseZipEintraege(zipPfad);
        assertThat(entries).hasSize(1);
        assertThat(entries).containsKey("user/abc/echte.png");
        assertThat(entries).doesNotContainKey("user/abc/weg.png");
    }

    @Test
    @DisplayName("DATEI-BACKUP-03: Audit-Log enthält 'DATEI_BACKUP_ERSTELLT' mit Skip-Count")
    void protokolliertAudit() throws IOException {
        when(medienAssetRepository.findAll()).thenReturn(List.of());

        Path zipPfad = service.erstelleDateiBackup();

        verify(auditService).protokolliere(
                eq(AuditAktion.DATEI_BACKUP_ERSTELLT),
                eq("SYSTEM"),
                isNull(),
                eq("DATEI_BACKUP"),
                any(String.class));
        assertThat(zipPfad).exists();
    }

    @Test
    @DisplayName("DATEI-BACKUP-04: listeDateiBackups gibt nur eigene ZIP-Dateien sortiert (neueste zuerst) zurück")
    void listeBackupsFiltertUndSortiert() throws IOException {
        Files.writeString(tempDir.resolve("sponsorplatz_uploads_20260518_120000.zip"), "x");
        Files.writeString(tempDir.resolve("sponsorplatz_uploads_20260519_120000.zip"), "y");
        Files.writeString(tempDir.resolve("sponsorplatz_backup_20260519_120000.sql"), "andere");
        Files.writeString(tempDir.resolve("notes.txt"), "noise");

        List<Path> liste = service.listeDateiBackups();

        assertThat(liste).hasSize(2);
        assertThat(liste.get(0).getFileName().toString()).isEqualTo("sponsorplatz_uploads_20260519_120000.zip");
        assertThat(liste.get(1).getFileName().toString()).isEqualTo("sponsorplatz_uploads_20260518_120000.zip");
    }

    @Test
    @DisplayName("DATEI-BACKUP-05: leseBackup + loescheBackup lehnen Path-Traversal + ungültige Namen ab")
    void leseLoescheValidiertDateinamen() {
        assertThatThrownBy(() -> service.leseDateiBackup("../etc/passwd"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.leseDateiBackup("ohne-praefix.zip"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.leseDateiBackup(""))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.loescheDateiBackup("sponsorplatz_uploads_x/../y.zip"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    // ── Helfer ────────────────────────────────────────────────────────────────

    private MedienAsset asset(String storagePfad, String contentType) {
        MedienAsset a = new MedienAsset();
        a.setStoragePfad(storagePfad);
        a.setContentType(contentType);
        a.setDateiname(storagePfad.substring(storagePfad.lastIndexOf('/') + 1));
        return a;
    }

    private Map<String, byte[]> leseZipEintraege(Path zip) throws IOException {
        Map<String, byte[]> map = new HashMap<>();
        try (ZipInputStream in = new ZipInputStream(Files.newInputStream(zip))) {
            ZipEntry entry;
            while ((entry = in.getNextEntry()) != null) {
                map.put(entry.getName(), in.readAllBytes());
                in.closeEntry();
            }
        }
        return map;
    }
}
