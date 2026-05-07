package ch.sponsorplatz.shared.storage;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

/**
 * Lokale Datei-Speicherung — Default-Implementierung für dev/test/prod-onprem.
 * Speichert unter dem konfigurierten Basis-Pfad (default: ./uploads/).
 *
 * <p>Aktiv, wenn {@code sponsorplatz.storage.provider} fehlt oder {@code lokal} ist.
 * Für OCI-Deployments gilt {@code sponsorplatz.storage.provider=oci} → siehe
 * {@link OciStorageService}.
 */
@Service
@ConditionalOnProperty(name = "sponsorplatz.storage.provider", havingValue = "lokal", matchIfMissing = true)
public class LokalerStorageService implements StorageService {

    private final Path basisPfad;

    public LokalerStorageService(
            @Value("${sponsorplatz.storage.lokal.basis-pfad:./uploads}") String basisPfad) {
        this.basisPfad = Paths.get(basisPfad).toAbsolutePath().normalize();
        erstelleVerzeichnis(this.basisPfad);
    }

    @Override
    public String speichere(MultipartFile datei, String zielpfad) {
        try {
            Path ziel = basisPfad.resolve(zielpfad).normalize();
            erstelleVerzeichnis(ziel.getParent());
            Files.copy(datei.getInputStream(), ziel, StandardCopyOption.REPLACE_EXISTING);
            return zielpfad;
        } catch (IOException e) {
            throw new RuntimeException("Datei konnte nicht gespeichert werden: " + zielpfad, e);
        }
    }

    @Override
    public void loesche(String storagePfad) {
        try {
            Path pfad = basisPfad.resolve(storagePfad).normalize();
            Files.deleteIfExists(pfad);
        } catch (IOException e) {
            throw new RuntimeException("Datei konnte nicht gelöscht werden: " + storagePfad, e);
        }
    }

    @Override
    public Resource ladeAlsResource(String storagePfad) {
        try {
            Path pfad = basisPfad.resolve(storagePfad).normalize();
            Resource resource = new UrlResource(pfad.toUri());
            if (resource.exists() && resource.isReadable()) {
                return resource;
            }
            throw new RuntimeException("Datei nicht gefunden: " + storagePfad);
        } catch (MalformedURLException e) {
            throw new RuntimeException("Ungültiger Pfad: " + storagePfad, e);
        }
    }

    private void erstelleVerzeichnis(Path pfad) {
        try {
            Files.createDirectories(pfad);
        } catch (IOException e) {
            throw new RuntimeException("Verzeichnis konnte nicht erstellt werden: " + pfad, e);
        }
    }
}

