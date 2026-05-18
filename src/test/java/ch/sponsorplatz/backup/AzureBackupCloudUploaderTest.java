package ch.sponsorplatz.backup;

import ch.sponsorplatz.shared.storage.AzureBlobOperationException;
import ch.sponsorplatz.shared.storage.AzureBlobOperations;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * Unit-Tests für {@link AzureBackupCloudUploader} — mockt das
 * {@link AzureBlobOperations}-Seam-Interface.
 *
 * Test-IDs siehe {@code specs/TESTSTRATEGIE.md} (CLOUD-BKP-AZ-01..04).
 */
class AzureBackupCloudUploaderTest {

    private AzureBlobOperations operations;
    private AzureBackupCloudUploader uploader;

    @BeforeEach
    void setUp() {
        operations = mock(AzureBlobOperations.class);
        uploader = new AzureBackupCloudUploader(operations);
    }

    @Test
    @DisplayName("CLOUD-BKP-AZ-01: lade nutzt Key 'backups/<dateiname>' + overwrite=true")
    void ladeSetztRequestKorrekt(@TempDir Path tmp) throws Exception {
        Path datei = tmp.resolve("sponsorplatz_backup_20260518_020000.sql");
        Files.writeString(datei, "-- dump\n");

        String key = uploader.lade(datei);

        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Long> lengthCaptor = ArgumentCaptor.forClass(Long.class);
        verify(operations).upload(keyCaptor.capture(), any(InputStream.class),
                lengthCaptor.capture(), eq(true));

        assertThat(key).isEqualTo("backups/sponsorplatz_backup_20260518_020000.sql");
        assertThat(keyCaptor.getValue()).isEqualTo("backups/sponsorplatz_backup_20260518_020000.sql");
        assertThat(lengthCaptor.getValue()).isEqualTo(Files.size(datei));
    }

    @Test
    @DisplayName("CLOUD-BKP-AZ-02: lade wrappt AzureBlobOperationException in RuntimeException")
    void ladeMappedOperationException(@TempDir Path tmp) throws Exception {
        Path datei = tmp.resolve("dump.sql");
        Files.writeString(datei, "-- dump\n");
        doThrow(new AzureBlobOperationException("Blob-Upload", "backups/dump.sql", 503,
                "ServerBusy", "throttled", null))
                .when(operations).upload(anyString(), any(InputStream.class), anyLong(), anyBoolean());

        assertThatThrownBy(() -> uploader.lade(datei))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Azure Backup-Upload");
    }

    @Test
    @DisplayName("CLOUD-BKP-AZ-03: lade wirft RuntimeException, wenn Datei nicht lesbar")
    void ladeMeldetFehlendeDatei(@TempDir Path tmp) {
        Path datei = tmp.resolve("gibt-es-nicht.sql");

        assertThatThrownBy(() -> uploader.lade(datei))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Backup-Datei");
    }

    @Test
    @DisplayName("CLOUD-BKP-AZ-04: Konstruktor lehnt null-Operations ab")
    void konstruktorLehntNullAb() {
        assertThatThrownBy(() -> new AzureBackupCloudUploader(null))
                .isInstanceOf(NullPointerException.class);
    }
}
