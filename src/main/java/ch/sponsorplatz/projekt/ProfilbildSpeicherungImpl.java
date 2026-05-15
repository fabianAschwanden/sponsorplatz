package ch.sponsorplatz.projekt;

import ch.sponsorplatz.benutzer.ProfilbildSpeicherung;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

/**
 * Adapter: implementiert den {@link ProfilbildSpeicherung}-Port aus
 * {@code benutzer} mit dem konkreten {@link MedienAssetService}-Stack
 * dieses Pakets — kapselt die {@link EntityTyp}/{@link AssetTyp}-Konstanten
 * vor dem Aufrufer, damit {@code benutzer} nicht auf {@code projekt}
 * zugreifen muss (ARCH-06).
 */
@Component
public class ProfilbildSpeicherungImpl implements ProfilbildSpeicherung {

    private final MedienAssetService medienAssetService;

    public ProfilbildSpeicherungImpl(MedienAssetService medienAssetService) {
        this.medienAssetService = medienAssetService;
    }

    @Override
    public UUID speichereProfilbild(MultipartFile datei, UUID userId) {
        return medienAssetService.speichereUndGibId(datei, EntityTyp.USER, userId, AssetTyp.PROFILBILD);
    }
}
