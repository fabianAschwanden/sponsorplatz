package ch.sponsorplatz.shared.storage;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.core.io.Resource;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

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
import static org.mockito.Mockito.when;

/**
 * Unit-Tests für {@link AzureBlobStorageService} — mockt das {@link AzureBlobOperations}-
 * Seam-Interface (die finalen Azure-SDK-Klassen, inkl. {@code BlobStorageException},
 * lassen sich mit dem projektweiten subclass-MockMaker nicht stubben).
 *
 * Test-IDs siehe {@code specs/TESTSTRATEGIE.md} (CLOUD-STO-AZ-01..06).
 */
class AzureBlobStorageServiceTest {

    private AzureBlobOperations operations;
    private AzureBlobStorageService storage;

    @BeforeEach
    void setUp() {
        operations = mock(AzureBlobOperations.class);
        storage = new AzureBlobStorageService(operations);
    }

    @Test
    @DisplayName("CLOUD-STO-AZ-01: validierePfad lehnt leer/null/'..' ab")
    void validierePfadLehntUngueltigesAb() {
        MultipartFile datei = new MockMultipartFile("file", "x.png", "image/png", new byte[]{1});

        assertThatThrownBy(() -> storage.speichere(datei, ""))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("leer");
        assertThatThrownBy(() -> storage.speichere(datei, null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> storage.speichere(datei, "../etc/passwd"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("..");
    }

    @Test
    @DisplayName("CLOUD-STO-AZ-02: speichere ruft upload mit Key, Stream, Länge, overwrite=true")
    void speichereSetztUploadRequestKorrekt() {
        MultipartFile datei = new MockMultipartFile("file", "logo.png", "image/png", new byte[]{1, 2, 3});

        String key = storage.speichere(datei, "organisation/abc/logo.png");

        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
        verify(operations).upload(keyCaptor.capture(), any(InputStream.class), eq(3L), eq(true));

        assertThat(key).isEqualTo("organisation/abc/logo.png");
        assertThat(keyCaptor.getValue()).isEqualTo("organisation/abc/logo.png");
    }

    @Test
    @DisplayName("CLOUD-STO-AZ-03: AzureBlobOperationException wird in RuntimeException gewrappt")
    void speichereMappedOperationException() {
        MultipartFile datei = new MockMultipartFile("file", "x.png", "image/png", new byte[]{1});
        doThrow(new AzureBlobOperationException("Blob-Upload", "k", 500, "InternalError", "boom", null))
                .when(operations).upload(anyString(), any(InputStream.class), anyLong(), anyBoolean());

        assertThatThrownBy(() -> storage.speichere(datei, "k"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Azure Blob-Upload");
    }

    @Test
    @DisplayName("CLOUD-STO-AZ-04: loesche ruft deleteIfExists — idempotent bei 404")
    void loescheIdempotent() {
        when(operations.deleteIfExists("organisation/abc/logo.png")).thenReturn(true);
        storage.loesche("organisation/abc/logo.png");
        verify(operations).deleteIfExists("organisation/abc/logo.png");

        // Zweiter Aufruf: Blob existiert nicht mehr → false, kein Throw
        when(operations.deleteIfExists("nicht-da")).thenReturn(false);
        storage.loesche("nicht-da");
        verify(operations).deleteIfExists("nicht-da");
    }

    @Test
    @DisplayName("CLOUD-STO-AZ-05: ladeAlsResource liefert Resource mit Blob-Content")
    void ladeAlsResourceLiefertContent() throws Exception {
        when(operations.openInputStream("organisation/abc/logo.png"))
                .thenReturn(new ByteArrayInputStream(new byte[]{42, 43, 44}));

        Resource resource = storage.ladeAlsResource("organisation/abc/logo.png");

        assertThat(resource.exists()).isTrue();
        assertThat(resource.getInputStream().readAllBytes()).containsExactly(42, 43, 44);
    }

    @Test
    @DisplayName("CLOUD-STO-AZ-06: ladeAlsResource wirft 'nicht gefunden' bei AzureBlobNotFoundException")
    void ladeAlsResourceWirft404() {
        when(operations.openInputStream("k"))
                .thenThrow(new AzureBlobNotFoundException("k", null));

        assertThatThrownBy(() -> storage.ladeAlsResource("k"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("nicht gefunden");
    }

    @Test
    @DisplayName("CLOUD-STO-AZ-07: speichereBytes ruft upload mit Key, Stream, Länge, overwrite=true")
    void speichereBytesSetztUploadKorrekt() {
        byte[] inhalt = {1, 2, 3, 4, 5};

        String key = storage.speichereBytes(inhalt, "image/png", "user/abc/restore.png");

        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
        verify(operations).upload(keyCaptor.capture(), any(InputStream.class), eq(5L), eq(true));

        assertThat(key).isEqualTo("user/abc/restore.png");
        assertThat(keyCaptor.getValue()).isEqualTo("user/abc/restore.png");
    }
}
