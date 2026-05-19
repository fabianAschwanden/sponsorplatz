package ch.sponsorplatz.shared.storage;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

/**
 * Azure-Blob-Storage-Implementierung von {@link StorageService}.
 *
 * <p>Aktiv, wenn {@code sponsorplatz.storage.provider=azure}. Client-Wiring
 * + Auth (Managed Identity oder Connection-String) liegt in
 * {@link AzureStorageConfig}. SDK-Calls laufen über
 * {@link AzureBlobOperations} (Test-Seam für die finalen SDK-Klassen).
 *
 * <p>Object-Keys (= Blob-Namen) entsprechen 1:1 dem {@code zielpfad}, den
 * {@code MedienAssetService} vergibt — die Hierarchie
 * ({@code organisation/{id}/{uuid}-{name}.png}) wird in Azure Blob als
 * String-Präfix gespeichert (Container sind flach, "Ordner" sind reine
 * Namens-Konvention).
 */
@Service
@ConditionalOnProperty(name = "sponsorplatz.storage.provider", havingValue = "azure")
public class AzureBlobStorageService implements StorageService {

    private final AzureBlobOperations uploads;

    public AzureBlobStorageService(
            @Qualifier("azureUploadsOperations") AzureBlobOperations uploads) {
        this.uploads = uploads;
    }

    @Override
    public String speichere(MultipartFile datei, String zielpfad) {
        validierePfad(zielpfad);
        try {
            uploads.upload(zielpfad, datei.getInputStream(), datei.getSize(), true);
            return zielpfad;
        } catch (AzureBlobOperationException e) {
            throw new RuntimeException(
                    "Azure Blob-Upload fehlgeschlagen (" + zielpfad + "): " + e.getMessage(), e);
        } catch (IOException e) {
            throw new RuntimeException("Datei konnte nicht gelesen werden: " + zielpfad, e);
        }
    }

    @Override
    public String speichereBytes(byte[] inhalt, String contentType, String zielpfad) {
        validierePfad(zielpfad);
        try {
            uploads.upload(zielpfad, new java.io.ByteArrayInputStream(inhalt), inhalt.length, true);
            return zielpfad;
        } catch (AzureBlobOperationException e) {
            throw new RuntimeException(
                    "Azure Blob-Upload (Bytes) fehlgeschlagen (" + zielpfad + "): " + e.getMessage(), e);
        }
    }

    @Override
    public void loesche(String storagePfad) {
        validierePfad(storagePfad);
        // deleteIfExists ist nativ idempotent — kein 404-Handling nötig.
        uploads.deleteIfExists(storagePfad);
    }

    @Override
    public Resource ladeAlsResource(String storagePfad) {
        validierePfad(storagePfad);
        try {
            return new InputStreamResource(uploads.openInputStream(storagePfad));
        } catch (AzureBlobNotFoundException e) {
            throw new StorageObjectNotFoundException(storagePfad, e);
        } catch (AzureBlobOperationException e) {
            throw new RuntimeException(
                    "Azure getBlob fehlgeschlagen (" + storagePfad + "): " + e.getMessage(), e);
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
