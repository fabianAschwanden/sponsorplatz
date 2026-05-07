package ch.sponsorplatz.service;
import ch.sponsorplatz.shared.storage.StorageService;

import ch.sponsorplatz.model.AssetTyp;
import ch.sponsorplatz.model.EntityTyp;
import ch.sponsorplatz.model.MedienAsset;
import ch.sponsorplatz.repository.MedienAssetRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Service für Medien-Assets (Upload, Löschen, Abfragen).
 */
@Service
@Transactional
public class MedienAssetService {

    private static final long MAX_GROESSE_BYTES = 5 * 1024 * 1024; // 5 MB
    private static final int MAX_ASSETS_PRO_ENTITY = 10;
    private static final Set<String> ERLAUBTE_CONTENT_TYPES = Set.of(
            "image/jpeg", "image/png", "image/webp"
    );

    private final MedienAssetRepository repository;
    private final StorageService storageService;

    public MedienAssetService(MedienAssetRepository repository, StorageService storageService) {
        this.repository = repository;
        this.storageService = storageService;
    }

    /**
     * Speichert ein hochgeladenes Medien-Asset.
     *
     * @throws IllegalArgumentException bei ungültigem Content-Type, zu grosser Datei, oder Limit erreicht
     */
    public MedienAsset speichere(MultipartFile datei, EntityTyp entityTyp, UUID entityId, AssetTyp assetTyp) {
        validiereUpload(datei, entityTyp, entityId);

        String dateiname = datei.getOriginalFilename() != null
                ? datei.getOriginalFilename()
                : "upload-" + UUID.randomUUID();

        String pfad = entityTyp.name().toLowerCase() + "/" + entityId + "/" + UUID.randomUUID() + "-" + dateiname;
        String gespeicherterPfad = storageService.speichere(datei, pfad);

        MedienAsset asset = new MedienAsset();
        asset.setDateiname(dateiname);
        asset.setContentType(datei.getContentType());
        asset.setGroesseBytes(datei.getSize());
        asset.setStoragePfad(gespeicherterPfad);
        asset.setEntityTyp(entityTyp);
        asset.setEntityId(entityId);
        asset.setAssetTyp(assetTyp);

        return repository.save(asset);
    }

    /**
     * Löscht ein Asset aus DB und Storage.
     */
    public void loesche(UUID assetId) {
        MedienAsset asset = repository.findById(assetId)
                .orElseThrow(() -> new IllegalArgumentException("Asset nicht gefunden: " + assetId));
        storageService.loesche(asset.getStoragePfad());
        repository.delete(asset);
    }

    @Transactional(readOnly = true)
    public List<MedienAsset> findeNachEntity(EntityTyp entityTyp, UUID entityId) {
        return repository.findByEntityTypAndEntityIdOrderBySortierungAsc(entityTyp, entityId);
    }

    @Transactional(readOnly = true)
    public Optional<MedienAsset> findeCover(EntityTyp entityTyp, UUID entityId) {
        return repository.findFirstByEntityTypAndEntityIdAndAssetTypOrderBySortierungAsc(
                entityTyp, entityId, AssetTyp.COVER);
    }

    @Transactional(readOnly = true)
    public Optional<MedienAsset> findeNachId(UUID id) {
        return repository.findById(id);
    }

    private void validiereUpload(MultipartFile datei, EntityTyp entityTyp, UUID entityId) {
        if (datei == null || datei.isEmpty()) {
            throw new IllegalArgumentException("Keine Datei hochgeladen");
        }
        if (!ERLAUBTE_CONTENT_TYPES.contains(datei.getContentType())) {
            throw new IllegalArgumentException(
                    "Ungültiger Dateityp: " + datei.getContentType() + ". Erlaubt: JPEG, PNG, WebP");
        }
        if (datei.getSize() > MAX_GROESSE_BYTES) {
            throw new IllegalArgumentException("Datei zu gross (max. 5 MB)");
        }
        long anzahl = repository.countByEntityTypAndEntityId(entityTyp, entityId);
        if (anzahl >= MAX_ASSETS_PRO_ENTITY) {
            throw new IllegalArgumentException("Maximum von " + MAX_ASSETS_PRO_ENTITY + " Medien pro Entity erreicht");
        }
    }
}

