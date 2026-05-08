package ch.sponsorplatz.projekt;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "sponsoring_paket")
public class SponsoringPaket {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "projekt_id", nullable = false)
    private Projekt projekt;

    @Column(name = "name", nullable = false, length = 255)
    private String name;

    @Column(name = "beschreibung", columnDefinition = "TEXT")
    private String beschreibung;

    @Column(name = "preis_chf", precision = 10, scale = 2)
    private BigDecimal preisChf;

    @Column(name = "gegenleistungen", columnDefinition = "TEXT")
    private String gegenleistungen;

    @Column(name = "sortierung", nullable = false)
    private int sortierung = 0;

    @Column(name = "aktiv", nullable = false)
    private boolean aktiv = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    void onCreate() {
        Instant jetzt = Instant.now();
        createdAt = jetzt;
        updatedAt = jetzt;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }

    // --- Getter / Setter ---

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public Projekt getProjekt() { return projekt; }
    public void setProjekt(Projekt projekt) { this.projekt = projekt; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getBeschreibung() { return beschreibung; }
    public void setBeschreibung(String beschreibung) { this.beschreibung = beschreibung; }

    public BigDecimal getPreisChf() { return preisChf; }
    public void setPreisChf(BigDecimal preisChf) { this.preisChf = preisChf; }

    public String getGegenleistungen() { return gegenleistungen; }
    public void setGegenleistungen(String gegenleistungen) { this.gegenleistungen = gegenleistungen; }

    public int getSortierung() { return sortierung; }
    public void setSortierung(int sortierung) { this.sortierung = sortierung; }

    public boolean isAktiv() { return aktiv; }
    public void setAktiv(boolean aktiv) { this.aktiv = aktiv; }

    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SponsoringPaket that)) return false;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() { return Objects.hash(id); }
}

