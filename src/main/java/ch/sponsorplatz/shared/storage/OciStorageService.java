package ch.sponsorplatz.shared.storage;

import com.oracle.bmc.model.BmcException;
import com.oracle.bmc.objectstorage.ObjectStorage;
import com.oracle.bmc.objectstorage.requests.DeleteObjectRequest;
import com.oracle.bmc.objectstorage.requests.GetObjectRequest;
import com.oracle.bmc.objectstorage.requests.PutObjectRequest;
import com.oracle.bmc.objectstorage.responses.GetObjectResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

/**
 * OCI-Object-Storage-Implementierung von {@link StorageService}.
 *
 * <p>Aktiv, wenn {@code sponsorplatz.storage.provider=oci}. Auth + Client-Wiring
 * liegt in {@link OciStorageConfig}.
 *
 * <p>Object-Keys entsprechen 1:1 dem {@code zielpfad}, den {@link MedienAssetService}
 * vergibt — die Hierarchie ({@code organisation/{id}/{uuid}-{name}.png}) wird in
 * Object-Storage als String-Präfix gespeichert (Buckets sind flach).
 */
@Service
@ConditionalOnProperty(name = "sponsorplatz.storage.provider", havingValue = "oci")
public class OciStorageService implements StorageService {

    private final ObjectStorage client;
    private final String namespace;
    private final String bucketName;

    public OciStorageService(
            ObjectStorage client,
            @Value("${sponsorplatz.storage.oci.namespace:}") String namespace,
            @Value("${sponsorplatz.storage.oci.bucket-uploads:sponsorplatz-uploads}") String bucketName) {
        if (namespace == null || namespace.isBlank()) {
            throw new IllegalStateException(
                    "sponsorplatz.storage.oci.namespace ist nicht gesetzt — siehe `oci os ns get`");
        }
        this.client = client;
        this.namespace = namespace;
        this.bucketName = bucketName;
    }

    @Override
    public String speichere(MultipartFile datei, String zielpfad) {
        validierePfad(zielpfad);
        try {
            client.putObject(PutObjectRequest.builder()
                    .namespaceName(namespace)
                    .bucketName(bucketName)
                    .objectName(zielpfad)
                    .contentLength(datei.getSize())
                    .contentType(datei.getContentType())
                    .putObjectBody(datei.getInputStream())
                    .build());
            return zielpfad;
        } catch (BmcException e) {
            throw new RuntimeException(
                    "OCI putObject fehlgeschlagen (" + zielpfad + "): "
                            + e.getStatusCode() + " " + e.getServiceCode() + " — " + e.getMessage(), e);
        } catch (IOException e) {
            throw new RuntimeException("Datei konnte nicht gelesen werden: " + zielpfad, e);
        }
    }

    @Override
    public void loesche(String storagePfad) {
        validierePfad(storagePfad);
        try {
            client.deleteObject(DeleteObjectRequest.builder()
                    .namespaceName(namespace)
                    .bucketName(bucketName)
                    .objectName(storagePfad)
                    .build());
        } catch (BmcException e) {
            if (e.getStatusCode() == 404) {
                return; // idempotent — schon weg
            }
            throw new RuntimeException(
                    "OCI deleteObject fehlgeschlagen (" + storagePfad + "): "
                            + e.getStatusCode() + " — " + e.getMessage(), e);
        }
    }

    @Override
    public Resource ladeAlsResource(String storagePfad) {
        validierePfad(storagePfad);
        try {
            GetObjectResponse response = client.getObject(GetObjectRequest.builder()
                    .namespaceName(namespace)
                    .bucketName(bucketName)
                    .objectName(storagePfad)
                    .build());
            return new InputStreamResource(response.getInputStream());
        } catch (BmcException e) {
            if (e.getStatusCode() == 404) {
                throw new RuntimeException("Datei nicht gefunden: " + storagePfad, e);
            }
            throw new RuntimeException(
                    "OCI getObject fehlgeschlagen (" + storagePfad + "): "
                            + e.getStatusCode() + " — " + e.getMessage(), e);
        }
    }

    private void validierePfad(String pfad) {
        if (pfad == null || pfad.isBlank()) {
            throw new IllegalArgumentException("Storage-Pfad darf nicht leer sein");
        }
        if (pfad.contains("..")) {
            throw new IllegalArgumentException("Storage-Pfad darf '..' nicht enthalten: " + pfad);
        }
    }
}
