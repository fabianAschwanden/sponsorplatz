package ch.sponsorplatz.shared.storage;

import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.models.BlobErrorCode;
import com.azure.storage.blob.models.BlobStorageException;

import java.io.InputStream;

/**
 * Produktive Implementierung von {@link AzureBlobOperations} — delegiert an
 * {@link BlobContainerClient} und übersetzt {@link BlobStorageException} in
 * die package-eigenen {@link AzureBlobNotFoundException} /
 * {@link AzureBlobOperationException}. Das ist die einzige Stelle, an der
 * Azure-SDK-Typen ausserhalb von {@code AzureStorageConfig} auftauchen.
 */
final class SdkAzureBlobOperations implements AzureBlobOperations {

    private final BlobContainerClient container;

    SdkAzureBlobOperations(BlobContainerClient container) {
        this.container = container;
    }

    @Override
    public void upload(String blobName, InputStream data, long length, boolean overwrite) {
        try {
            container.getBlobClient(blobName).upload(data, length, overwrite);
        } catch (BlobStorageException e) {
            throw uebersetze("Blob-Upload", blobName, e);
        }
    }

    @Override
    public boolean deleteIfExists(String blobName) {
        try {
            return container.getBlobClient(blobName).deleteIfExists();
        } catch (BlobStorageException e) {
            throw uebersetze("Blob-Delete", blobName, e);
        }
    }

    @Override
    public InputStream openInputStream(String blobName) {
        try {
            return container.getBlobClient(blobName).openInputStream();
        } catch (BlobStorageException e) {
            throw uebersetze("Blob-Read", blobName, e);
        }
    }

    private static RuntimeException uebersetze(String operation, String blobName, BlobStorageException e) {
        if (e.getStatusCode() == 404 || BlobErrorCode.BLOB_NOT_FOUND.equals(e.getErrorCode())) {
            return new AzureBlobNotFoundException(blobName, e);
        }
        return new AzureBlobOperationException(
                operation,
                blobName,
                e.getStatusCode(),
                String.valueOf(e.getErrorCode()),
                e.getMessage(),
                e);
    }
}
