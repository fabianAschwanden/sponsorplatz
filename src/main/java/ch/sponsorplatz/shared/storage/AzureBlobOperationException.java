package ch.sponsorplatz.shared.storage;

/**
 * Adapter-eigene Wrapper-Exception für jeden Nicht-404-Fehler vom Azure-SDK.
 * Trägt den HTTP-Status-Code und den ErrorCode-String für Logs/Diagnose.
 */
public class AzureBlobOperationException extends RuntimeException {

    private final int statusCode;
    private final String errorCode;

    public AzureBlobOperationException(String operation, String blobName, int statusCode,
                                       String errorCode, String message, Throwable cause) {
        super("Azure " + operation + " fehlgeschlagen (" + blobName + "): "
                + statusCode + " " + errorCode + " — " + message, cause);
        this.statusCode = statusCode;
        this.errorCode = errorCode;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public String getErrorCode() {
        return errorCode;
    }
}
