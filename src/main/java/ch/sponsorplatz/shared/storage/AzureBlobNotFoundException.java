package ch.sponsorplatz.shared.storage;

/**
 * Adapter-eigene 404-Exception — {@link SdkAzureBlobOperations} wirft sie,
 * wenn Azure mit Status 404 / {@code BlobErrorCode.BLOB_NOT_FOUND} antwortet.
 */
public class AzureBlobNotFoundException extends RuntimeException {

    public AzureBlobNotFoundException(String blobName, Throwable cause) {
        super("Azure-Blob nicht gefunden: " + blobName, cause);
    }
}
