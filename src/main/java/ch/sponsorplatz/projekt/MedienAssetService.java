package ch.sponsorplatz.projekt;
import ch.sponsorplatz.shared.storage.StorageService;

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
    private static final long MAX_DOKUMENT_BYTES = 20 * 1024 * 1024; // 20 MB
    private static final int MAX_ASSETS_PRO_ENTITY = 10;
    private static final Set<String> ERLAUBTE_BILD_TYPES = Set.of(
            "image/jpeg", "image/png", "image/webp"
    );
    private static final Set<String> ERLAUBTE_DOKUMENT_TYPES = Set.of(
            "application/pdf",
            "application/vnd.openxmlformats-officedocument.presentationml.presentation", // pptx
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",    // docx
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",          // xlsx
            "application/vnd.ms-powerpoint",  // ppt
            "application/msword",             // doc
            "application/vnd.ms-excel"        // xls
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
    /** Wrapper für Controller — gibt nur die neue Asset-ID zurück (ARCH-02). */
    public UUID speichereUndGibId(MultipartFile datei, EntityTyp entityTyp, UUID entityId, AssetTyp assetTyp) {
        return speichere(datei, entityTyp, entityId, assetTyp).getId();
    }

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

    /**
     * Splittet die Assets einer Entity in Bilder (Cover/Galerie/Profilbild/…)
     * und Anhänge (PDFs/Office-Dokumente) — Controller braucht keine
     * Entity-Liste mehr durchzustreamen (ARCH-02).
     */
    @Transactional(readOnly = true)
    public BilderUndAnhaenge findeBilderUndAnhaengeViews(EntityTyp entityTyp, UUID entityId) {
        List<MedienAsset> alle = findeNachEntity(entityTyp, entityId);
        List<MedienAssetView> bilder = MedienAssetView.von(alle.stream()
                .filter(m -> m.getAssetTyp() != AssetTyp.ANHANG).toList());
        List<MedienAssetView> anhaenge = MedienAssetView.von(alle.stream()
                .filter(m -> m.getAssetTyp() == AssetTyp.ANHANG).toList());
        return new BilderUndAnhaenge(bilder, anhaenge);
    }

    /** Tuple aus den beiden Asset-Typ-Listen, die der ProjektController braucht. */
    public record BilderUndAnhaenge(List<MedienAssetView> bilder, List<MedienAssetView> anhaenge) {}

    /** Asset-Daten für die HTTP-Auslieferung — Controller braucht keine Entity (ARCH-02). */
    @Transactional(readOnly = true)
    public AuslieferungsSnapshot findeAuslieferungsSnapshot(UUID id) {
        return repository.findById(id)
                .map(a -> new AuslieferungsSnapshot(a.getStoragePfad(), a.getContentType(), a.getDateiname()))
                .orElseThrow(() -> new ch.sponsorplatz.shared.exception.NotFoundException(
                        "Medien-Asset nicht gefunden: " + id));
    }

    public record AuslieferungsSnapshot(String storagePfad, String contentType, String dateiname) {}

    /** Berechtigungs-Snapshot für den Lösch-Endpoint (ARCH-02). */
    @Transactional(readOnly = true)
    public BerechtigungsSnapshot findeBerechtigungsSnapshot(UUID id) {
        return repository.findById(id)
                .map(a -> new BerechtigungsSnapshot(a.getEntityTyp(), a.getEntityId()))
                .orElseThrow(() -> new ch.sponsorplatz.shared.exception.NotFoundException(
                        "Medien-Asset nicht gefunden: " + id));
    }

    public record BerechtigungsSnapshot(EntityTyp entityTyp, UUID entityId) {}

    @Transactional(readOnly = true)
    public Optional<MedienAsset> findeCover(EntityTyp entityTyp, UUID entityId) {
        return repository.findFirstByEntityTypAndEntityIdAndAssetTypOrderBySortierungAsc(
                entityTyp, entityId, AssetTyp.COVER);
    }

    /**
     * Liefert den Auslieferungs-Pfad des Cover-Assets — Controller braucht
     * keinen Entity-Zugriff (ARCH-02). Returns {@code Optional.empty()} wenn
     * keine Cover-Datei vorhanden.
     */
    @Transactional(readOnly = true)
    public Optional<String> findeCoverUrl(EntityTyp entityTyp, UUID entityId) {
        return findeCover(entityTyp, entityId).map(a -> "/medien/" + a.getId());
    }

    /** View-Variante von {@link #findeAnhaenge} (ARCH-02). */
    @Transactional(readOnly = true)
    public List<MedienAssetView> findeAnhaengeViews(EntityTyp entityTyp, UUID entityId) {
        return MedienAssetView.von(findeAnhaenge(entityTyp, entityId));
    }

    /** View-Variante von {@link #findeGalerie} (ARCH-02). */
    @Transactional(readOnly = true)
    public List<MedienAssetView> findeGalerieViews(EntityTyp entityTyp, UUID entityId) {
        return MedienAssetView.von(findeGalerie(entityTyp, entityId));
    }

    @Transactional(readOnly = true)
    public Optional<MedienAsset> findeNachId(UUID id) {
        return repository.findById(id);
    }

    /** Findet alle Anhänge (Dokumente) eines Projekts — für die öffentliche Ansicht. */
    @Transactional(readOnly = true)
    public List<MedienAsset> findeAnhaenge(EntityTyp entityTyp, UUID entityId) {
        return repository.findByEntityTypAndEntityIdAndAssetTypOrderBySortierungAsc(
                entityTyp, entityId, AssetTyp.ANHANG);
    }

    /** Findet alle Galerie-Bilder eines Projekts — für die öffentliche Bildanzeige. */
    @Transactional(readOnly = true)
    public List<MedienAsset> findeGalerie(EntityTyp entityTyp, UUID entityId) {
        return repository.findByEntityTypAndEntityIdAndAssetTypOrderBySortierungAsc(
                entityTyp, entityId, AssetTyp.GALERIE);
    }

    private void validiereUpload(MultipartFile datei, EntityTyp entityTyp, UUID entityId) {
        if (datei == null || datei.isEmpty()) {
            throw new IllegalArgumentException("Keine Datei hochgeladen");
        }
        String contentType = datei.getContentType();
        boolean istBild = ERLAUBTE_BILD_TYPES.contains(contentType);
        boolean istDokument = ERLAUBTE_DOKUMENT_TYPES.contains(contentType);
        if (!istBild && !istDokument) {
            throw new IllegalArgumentException(
                    "Ungültiger Dateityp: " + contentType
                    + ". Erlaubt: Bilder (JPEG, PNG, WebP) oder Dokumente (PDF, PPTX, DOCX, XLSX)");
        }
        long maxBytes = istDokument ? MAX_DOKUMENT_BYTES : MAX_GROESSE_BYTES;
        if (datei.getSize() > maxBytes) {
            throw new IllegalArgumentException(
                    "Datei zu gross (max. " + (maxBytes / 1024 / 1024) + " MB)");
        }
        long anzahl = repository.countByEntityTypAndEntityId(entityTyp, entityId);
        if (anzahl >= MAX_ASSETS_PRO_ENTITY) {
            throw new IllegalArgumentException("Maximum von " + MAX_ASSETS_PRO_ENTITY + " Medien pro Entity erreicht");
        }
    }
}

