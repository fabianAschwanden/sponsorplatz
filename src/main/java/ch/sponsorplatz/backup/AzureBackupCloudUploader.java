package ch.sponsorplatz.backup;

import ch.sponsorplatz.shared.storage.AzureBlobOperationException;
import ch.sponsorplatz.shared.storage.AzureBlobOperations;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

/**
 * Lädt Backup-Dumps in einen Azure-Blob-Container. Aktiv, wenn
 * {@code sponsorplatz.storage.provider=azure}.
 *
 * <p>Der Container ist standardmässig {@code sponsorplatz-backups}. Versionierung +
 * Lifecycle-Rules werden im Storage-Account selbst konfiguriert (Terraform),
 * nicht hier. SDK-Calls laufen über {@link AzureBlobOperations} (siehe
 * {@code AzureBlobStorageService} für den Mock-Hintergrund).
 */
@Component
@ConditionalOnProperty(name = "sponsorplatz.storage.provider", havingValue = "azure")
public class AzureBackupCloudUploader implements BackupCloudUploader {

    private final AzureBlobOperations backups;

    public AzureBackupCloudUploader(
            @Qualifier("azureBackupsOperations") AzureBlobOperations backups) {
        this.backups = Objects.requireNonNull(backups, "azureBackupsOperations");
    }

    @Override
    public String lade(Path datei) {
        String objectKey = "backups/" + datei.getFileName().toString();
        try (InputStream in = Files.newInputStream(datei)) {
            backups.upload(objectKey, in, Files.size(datei), true);
            return objectKey;
        } catch (AzureBlobOperationException e) {
            throw new RuntimeException(
                    "Azure Backup-Upload fehlgeschlagen (" + objectKey + "): " + e.getMessage(), e);
        } catch (IOException e) {
            throw new RuntimeException("Backup-Datei konnte nicht gelesen werden: " + datei, e);
        }
    }
}
