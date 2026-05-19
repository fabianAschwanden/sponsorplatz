package ch.sponsorplatz.backup;

import ch.sponsorplatz.audit.AuditAktion;
import ch.sponsorplatz.audit.AuditService;
import ch.sponsorplatz.shared.storage.StorageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * Unit-Tests für {@link DateiBackupRestoreService}.
 *
 * Test-IDs: DATEI-RESTORE-01..04 in {@code specs/TESTSTRATEGIE.md}.
 */
@ExtendWith(MockitoExtension.class)
class DateiBackupRestoreServiceTest {

    @Mock private StorageService storageService;
    @Mock private AuditService auditService;

    private DateiBackupRestoreService service;

    @BeforeEach
    void setUp() {
        service = new DateiBackupRestoreService(storageService, auditService);
    }

    @Test
    @DisplayName("DATEI-RESTORE-01: leerer/null Input wird abgelehnt")
    void leererInputWirdAbgelehnt() {
        assertThatThrownBy(() -> service.restore(new byte[0], "admin"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("leer");
        assertThatThrownBy(() -> service.restore(null, "admin"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("DATEI-RESTORE-02: jeder ZIP-Entry landet via speichereBytes im Storage mit korrektem Content-Type")
    void restoreSchiebtJedenEintragWeiter() throws IOException {
        byte[] zipBytes = baueZip(
                eintrag("user/abc/logo.png", new byte[]{1, 2, 3}),
                eintrag("organisation/xyz/cover.jpg", new byte[]{4, 5}),
                eintrag("user/abc/notes.pdf", new byte[]{6}));

        DateiBackupRestoreService.RestoreReport report = service.restore(zipBytes, "admin@example.ch");

        assertThat(report.restored()).isEqualTo(3);
        assertThat(report.skipped()).isZero();

        ArgumentCaptor<String> contentTypeCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> pfadCaptor = ArgumentCaptor.forClass(String.class);
        verify(storageService, org.mockito.Mockito.times(3))
                .speichereBytes(any(byte[].class), contentTypeCaptor.capture(), pfadCaptor.capture());

        assertThat(pfadCaptor.getAllValues()).containsExactly(
                "user/abc/logo.png", "organisation/xyz/cover.jpg", "user/abc/notes.pdf");
        // Content-Type-Heuristik via URLConnection.guessContentTypeFromName
        assertThat(contentTypeCaptor.getAllValues().get(0)).isEqualTo("image/png");
        assertThat(contentTypeCaptor.getAllValues().get(1)).isEqualTo("image/jpeg");
        assertThat(contentTypeCaptor.getAllValues().get(2)).isEqualTo("application/pdf");
    }

    @Test
    @DisplayName("DATEI-RESTORE-03: Path-Traversal-Entry ('..') wird übersprungen, nicht im Storage abgelegt")
    void pathTraversalEintragWirdGeskippt() throws IOException {
        byte[] zipBytes = baueZip(
                eintrag("../etc/passwd", new byte[]{99}),
                eintrag("user/abc/sauber.png", new byte[]{1}));

        DateiBackupRestoreService.RestoreReport report = service.restore(zipBytes, "admin");

        assertThat(report.restored()).isEqualTo(1);
        assertThat(report.skipped()).isEqualTo(1);
        verify(storageService).speichereBytes(any(byte[].class), any(String.class), eq("user/abc/sauber.png"));
        verify(storageService, never()).speichereBytes(any(byte[].class), any(String.class), eq("../etc/passwd"));
    }

    @Test
    @DisplayName("DATEI-RESTORE-04: Audit-Log enthält 'DATEI_BACKUP_RESTORED' mit ausgefuehrtVon")
    void protokolliertAudit() throws IOException {
        byte[] zipBytes = baueZip(eintrag("user/abc/x.png", new byte[]{1}));

        service.restore(zipBytes, "admin@example.ch");

        verify(auditService).protokolliere(
                eq(AuditAktion.DATEI_BACKUP_RESTORED),
                eq("admin@example.ch"),
                isNull(),
                eq("DATEI_BACKUP"),
                any(String.class));
    }

    // ── Helfer ────────────────────────────────────────────────────────────────

    private record Eintrag(String name, byte[] inhalt) {}

    private Eintrag eintrag(String name, byte[] inhalt) {
        return new Eintrag(name, inhalt);
    }

    private byte[] baueZip(Eintrag... eintraege) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (OutputStream o = out; ZipOutputStream zip = new ZipOutputStream(o)) {
            for (Eintrag e : eintraege) {
                zip.putNextEntry(new ZipEntry(e.name()));
                zip.write(e.inhalt());
                zip.closeEntry();
            }
        }
        return out.toByteArray();
    }
}
