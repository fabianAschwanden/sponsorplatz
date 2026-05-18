package ch.sponsorplatz.shared.storage;

import java.io.InputStream;

/**
 * Schmaler Adapter um die finalen Azure-SDK-Klassen
 * ({@code BlobContainerClient}, {@code BlobClient}, {@code BlobStorageException})
 * mockbar zu machen — Mockito mit dem projektweiten subclass-MockMaker (siehe
 * {@code src/test/resources/mockito-extensions}) kann sie sonst nicht stubben.
 *
 * <p>Produktive Implementierung: {@link SdkAzureBlobOperations} — delegiert
 * an {@code BlobContainerClient} und übersetzt {@code BlobStorageException}
 * in die im selben Package definierten {@link AzureBlobNotFoundException} /
 * {@link AzureBlobOperationException}. Damit kennt {@link AzureBlobStorageService}
 * keinen Azure-SDK-Typ mehr.
 */
public interface AzureBlobOperations {

    /**
     * @throws AzureBlobOperationException bei jedem Azure-Fehler ausser 404
     */
    void upload(String blobName, InputStream data, long length, boolean overwrite);

    /**
     * Idempotent — wenn das Blob nicht existiert, false zurück, kein Throw.
     *
     * @throws AzureBlobOperationException bei anderen Azure-Fehlern
     */
    boolean deleteIfExists(String blobName);

    /**
     * @throws AzureBlobNotFoundException wenn das Blob nicht existiert
     * @throws AzureBlobOperationException bei anderen Azure-Fehlern
     */
    InputStream openInputStream(String blobName);
}
