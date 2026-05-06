package ch.sponsorplatz.service;

import com.oracle.bmc.model.BmcException;
import com.oracle.bmc.objectstorage.ObjectStorage;
import com.oracle.bmc.objectstorage.requests.DeleteObjectRequest;
import com.oracle.bmc.objectstorage.requests.GetObjectRequest;
import com.oracle.bmc.objectstorage.requests.PutObjectRequest;
import com.oracle.bmc.objectstorage.responses.GetObjectResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.core.io.Resource;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit-Tests für {@link OciStorageService} — mockt den ObjectStorage-Client,
 * prüft Request-Konstruktion und Fehler-Mapping.
 *
 * Test-IDs siehe {@code specs/TESTSTRATEGIE.md} (CLOUD-STO-01..06).
 */
class OciStorageServiceTest {

    private ObjectStorage client;
    private OciStorageService storage;

    @BeforeEach
    void setUp() {
        client = mock(ObjectStorage.class);
        storage = new OciStorageService(client, "demo-namespace", "uploads-bucket");
    }

    @Test
    @DisplayName("CLOUD-STO-01: Konstruktor wirft, wenn namespace leer")
    void konstruktorPruefftNamespace() {
        assertThatThrownBy(() -> new OciStorageService(client, "", "bucket"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("namespace");
        assertThatThrownBy(() -> new OciStorageService(client, null, "bucket"))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("CLOUD-STO-02: speichere ruft putObject mit Namespace+Bucket+Key+Inhalt")
    void speichereSetztRequestKorrekt() {
        MultipartFile datei = new MockMultipartFile("file", "logo.png", "image/png", new byte[]{1, 2, 3});

        String key = storage.speichere(datei, "organisation/abc/logo.png");

        ArgumentCaptor<PutObjectRequest> captor = ArgumentCaptor.forClass(PutObjectRequest.class);
        verify(client).putObject(captor.capture());
        PutObjectRequest req = captor.getValue();

        assertThat(key).isEqualTo("organisation/abc/logo.png");
        assertThat(req.getNamespaceName()).isEqualTo("demo-namespace");
        assertThat(req.getBucketName()).isEqualTo("uploads-bucket");
        assertThat(req.getObjectName()).isEqualTo("organisation/abc/logo.png");
        assertThat(req.getContentLength()).isEqualTo(3L);
    }

    @Test
    @DisplayName("CLOUD-STO-03: speichere wrappt BmcException in RuntimeException")
    void speichereMappedBmcException() {
        MultipartFile datei = new MockMultipartFile("file", "x.png", "image/png", new byte[]{1});
        when(client.putObject(any(PutObjectRequest.class)))
                .thenThrow(new BmcException(500, "InternalError", "boom", "rid"));

        assertThatThrownBy(() -> storage.speichere(datei, "k"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("OCI putObject");
    }

    @Test
    @DisplayName("CLOUD-STO-04: loesche ruft deleteObject; 404 ist idempotent")
    void loescheIdempotent() {
        // Normaler Delete
        storage.loesche("organisation/abc/logo.png");
        ArgumentCaptor<DeleteObjectRequest> captor = ArgumentCaptor.forClass(DeleteObjectRequest.class);
        verify(client).deleteObject(captor.capture());
        assertThat(captor.getValue().getObjectName()).isEqualTo("organisation/abc/logo.png");

        // 404 wirft NICHT
        when(client.deleteObject(any(DeleteObjectRequest.class)))
                .thenThrow(new BmcException(404, "NotFound", "weg", "rid"));
        storage.loesche("nicht-da");
    }

    @Test
    @DisplayName("CLOUD-STO-05: ladeAlsResource liest Object und liefert Resource mit Content")
    void ladeAlsResourceLiefertContent() throws Exception {
        GetObjectResponse response = GetObjectResponse.builder()
                .inputStream(new ByteArrayInputStream(new byte[]{42, 43, 44}))
                .build();
        when(client.getObject(any(GetObjectRequest.class))).thenReturn(response);

        Resource resource = storage.ladeAlsResource("organisation/abc/logo.png");

        assertThat(resource.exists()).isTrue();
        assertThat(resource.getInputStream().readAllBytes()).containsExactly(42, 43, 44);
    }

    @Test
    @DisplayName("CLOUD-STO-06: ladeAlsResource wirft, wenn Object nicht existiert (404)")
    void ladeAlsResourceWirft404() {
        when(client.getObject(any(GetObjectRequest.class)))
                .thenThrow(new BmcException(404, "NotFound", "fehlt", "rid"));

        assertThatThrownBy(() -> storage.ladeAlsResource("k"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("nicht gefunden");
    }
}
