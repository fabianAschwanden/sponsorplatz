package ch.sponsorplatz.shared.storage;

import com.azure.identity.DefaultAzureCredentialBuilder;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.BlobServiceClientBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Wiring für die Azure-Blob-Clients — nur aktiv, wenn
 * {@code sponsorplatz.storage.provider=azure}.
 *
 * <p>Auth-Modi (Property {@code sponsorplatz.storage.azure.auth-mode}):
 * <ul>
 *   <li>{@code managed-identity} (Default) — User-Assigned Managed Identity der
 *       VM, kein Secret nötig. Empfohlen für Prod auf Azure-VM.</li>
 *   <li>{@code connection-string} — liest
 *       {@code sponsorplatz.storage.azure.connection-string} (z.B. für lokales
 *       Testen gegen Azurite oder Storage-Account-Key).</li>
 * </ul>
 *
 * <p>Stellt zwei {@link BlobContainerClient}-Beans bereit:
 * <ul>
 *   <li>{@code azureUploadsContainer} — Medien-Uploads
 *       ({@link AzureBlobStorageService})</li>
 *   <li>{@code azureBackupsContainer} — DB-Dumps
 *       ({@code AzureBackupCloudUploader})</li>
 * </ul>
 */
@Configuration
@ConditionalOnProperty(name = "sponsorplatz.storage.provider", havingValue = "azure")
public class AzureStorageConfig {

    private static final Logger log = LoggerFactory.getLogger(AzureStorageConfig.class);

    @Bean
    BlobServiceClient blobServiceClient(
            @Value("${sponsorplatz.storage.azure.auth-mode:managed-identity}") String authMode,
            @Value("${sponsorplatz.storage.azure.account-url:}") String accountUrl,
            @Value("${sponsorplatz.storage.azure.connection-string:}") String connectionString) {
        return switch (authMode.toLowerCase()) {
            case "managed-identity" -> {
                if (accountUrl == null || accountUrl.isBlank()) {
                    throw new IllegalStateException(
                            "sponsorplatz.storage.azure.account-url ist nicht gesetzt "
                                    + "(z.B. https://<storage-account>.blob.core.windows.net)");
                }
                log.info("Azure Auth: Managed Identity (account-url={})", accountUrl);
                yield new BlobServiceClientBuilder()
                        .endpoint(accountUrl)
                        .credential(new DefaultAzureCredentialBuilder().build())
                        .buildClient();
            }
            case "connection-string" -> {
                if (connectionString == null || connectionString.isBlank()) {
                    throw new IllegalStateException(
                            "sponsorplatz.storage.azure.connection-string ist nicht gesetzt");
                }
                log.info("Azure Auth: Connection-String");
                yield new BlobServiceClientBuilder()
                        .connectionString(connectionString)
                        .buildClient();
            }
            default -> throw new IllegalArgumentException(
                    "Unbekannter sponsorplatz.storage.azure.auth-mode: " + authMode
                            + " (erwartet: managed-identity|connection-string)");
        };
    }

    @Bean(name = "azureUploadsOperations")
    AzureBlobOperations azureUploadsOperations(
            BlobServiceClient serviceClient,
            @Value("${sponsorplatz.storage.azure.container-uploads:sponsorplatz-uploads}") String name) {
        return new SdkAzureBlobOperations(containerFor(serviceClient, name));
    }

    @Bean(name = "azureBackupsOperations")
    AzureBlobOperations azureBackupsOperations(
            BlobServiceClient serviceClient,
            @Value("${sponsorplatz.storage.azure.container-backups:sponsorplatz-backups}") String name) {
        return new SdkAzureBlobOperations(containerFor(serviceClient, name));
    }

    private static BlobContainerClient containerFor(BlobServiceClient serviceClient, String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalStateException("Azure-Container-Name darf nicht leer sein");
        }
        return serviceClient.getBlobContainerClient(name);
    }
}
