package ch.sponsorplatz.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/**
 * Medien-Asset — Bilder, Dokumente, Logos für Projekte und Organisationen.
 */
@Entity
@Table(name = "medien_asset")
public class MedienAsset {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "dateiname", nullable = false, length = 255)
    private String dateiname;

    @Column(name = "content_type", nullable = false, length = 100)
    private String contentType;

    @Column(name = "groesse_bytes", nullable = false)
    private long groesseBytes;

    @Column(name = "storage_pfad", nullable = false, length = 500)
    private String storagePfad;

    @Enumerated(EnumType.STRING)
    @Column(name = "entity_typ", nullable = false, length = 20)
    private EntityTyp entityTyp;

    @Column(name = "entity_id", nullable = false)
    private UUID entityId;

    @Enumerated(EnumType.STRING)
    @Column(name = "asset_typ", nullable = false, length = 20)
    private AssetTyp assetTyp;

    @Column(name = "sortierung", nullable = false)
    private int sortierung = 0;

    @Column(name = "erstellt_am", nullable = false, updatable = false)
    private Instant erstelltAm;

    @PrePersist
    void onCreate() {
        if (erstelltAm == null) {
            erstelltAm = Instant.now();
        }
    }

    // --- Getter / Setter ---

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getDateiname() { return dateiname; }
    public void setDateiname(String dateiname) { this.dateiname = dateiname; }

    public String getContentType() { return contentType; }
    public void setContentType(String contentType) { this.contentType = contentType; }

    public long getGroesseBytes() { return groesseBytes; }
    public void setGroesseBytes(long groesseBytes) { this.groesseBytes = groesseBytes; }

    public String getStoragePfad() { return storagePfad; }
    public void setStoragePfad(String storagePfad) { this.storagePfad = storagePfad; }

    public EntityTyp getEntityTyp() { return entityTyp; }
    public void setEntityTyp(EntityTyp entityTyp) { this.entityTyp = entityTyp; }

    public UUID getEntityId() { return entityId; }
    public void setEntityId(UUID entityId) { this.entityId = entityId; }

    public AssetTyp getAssetTyp() { return assetTyp; }
    public void setAssetTyp(AssetTyp assetTyp) { this.assetTyp = assetTyp; }

    public int getSortierung() { return sortierung; }
    public void setSortierung(int sortierung) { this.sortierung = sortierung; }

    public Instant getErstelltAm() { return erstelltAm; }
}

