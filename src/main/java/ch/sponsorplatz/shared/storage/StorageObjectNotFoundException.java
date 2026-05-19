package ch.sponsorplatz.shared.storage;

/**
 * Wird von {@link StorageService#ladeAlsResource(String)} geworfen, wenn das
 * referenzierte Objekt im Storage-Backend nicht (mehr) existiert.
 *
 * <p>Provider-agnostisch — kapselt 404 von OCI Object Storage, 404 von Azure
 * Blob Storage und {@code FileNotFoundException} vom lokalen Filesystem unter
 * einem gemeinsamen Typ. Aufrufer können so semantisch reagieren
 * (z.B. {@link ch.sponsorplatz.projekt.MedienController#ausliefern} liefert
 * HTTP 404 statt 500 bei orphaned MedienAsset-Records).
 */
public class StorageObjectNotFoundException extends RuntimeException {

    public StorageObjectNotFoundException(String storagePfad) {
        super("Datei nicht gefunden: " + storagePfad);
    }

    public StorageObjectNotFoundException(String storagePfad, Throwable cause) {
        super("Datei nicht gefunden: " + storagePfad, cause);
    }
}
