package ch.sponsorplatz.service;

import com.oracle.bmc.model.BmcException;
import com.oracle.bmc.objectstorage.ObjectStorage;
import com.oracle.bmc.objectstorage.requests.PutObjectRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Lädt Backup-Dumps in einen OCI-Object-Storage-Bucket. Aktiv, wenn
 * {@code sponsorplatz.storage.provider=oci}.
 *
 * <p>Der Bucket ist standardmässig {@code sponsorplatz-backups}. Versionierung +
 * Lifecycle-Rules werden im Bucket selbst konfiguriert (Terraform), nicht hier.
 */
@Component
@ConditionalOnProperty(name = "sponsorplatz.storage.provider", havingValue = "oci")
public class OciBackupCloudUploader implements BackupCloudUploader {

    private final ObjectStorage client;
    private final String namespace;
    private final String bucketName;

    public OciBackupCloudUploader(
            ObjectStorage client,
            @Value("${sponsorplatz.storage.oci.namespace:}") String namespace,
            @Value("${sponsorplatz.storage.oci.bucket-backups:sponsorplatz-backups}") String bucketName) {
        if (namespace == null || namespace.isBlank()) {
            throw new IllegalStateException(
                    "sponsorplatz.storage.oci.namespace ist nicht gesetzt — siehe `oci os ns get`");
        }
        this.client = client;
        this.namespace = namespace;
        this.bucketName = bucketName;
    }

    @Override
    public String lade(Path datei) {
        String objectKey = "backups/" + datei.getFileName().toString();
        try (InputStream in = Files.newInputStream(datei)) {
            client.putObject(PutObjectRequest.builder()
                    .namespaceName(namespace)
                    .bucketName(bucketName)
                    .objectName(objectKey)
                    .contentLength(Files.size(datei))
                    .contentType("application/sql")
                    .putObjectBody(in)
                    .build());
            return objectKey;
        } catch (BmcException e) {
            throw new RuntimeException(
                    "OCI Backup-Upload fehlgeschlagen (" + objectKey + "): "
                            + e.getStatusCode() + " — " + e.getMessage(), e);
        } catch (IOException e) {
            throw new RuntimeException("Backup-Datei konnte nicht gelesen werden: " + datei, e);
        }
    }
}
