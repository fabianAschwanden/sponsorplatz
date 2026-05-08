package ch.sponsorplatz.projekt;

import ch.sponsorplatz.organisation.Organisation;
import jakarta.persistence.*;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

/**
 * Ein Vereins-Event (z.B. Turnier, Aktionstag, Generalversammlung).
 * Gehoert zu einer Organisation und wird auf dem Dashboard und
 * im Marktplatz angezeigt.
 */
@Entity
@Table(name = "event")
public class Event {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "org_id", nullable = false)
    private Organisation org;

    @Column(name = "name", nullable = false, length = 255)
    private String name;

    @Column(name = "slug", nullable = false, unique = true, length = 255)
    private String slug;

    @Column(name = "beschreibung", columnDefinition = "TEXT")
    private String beschreibung;

    @Column(name = "ort", length = 200)
    private String ort;

    @Column(name = "datum", nullable = false)
    private LocalDate datum;

    @Column(name = "datum_ende")
    private LocalDate datumEnde;

    @Column(name = "kapazitaet")
    private Integer kapazitaet;

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

    public Organisation getOrg() { return org; }
    public void setOrg(Organisation org) { this.org = org; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getSlug() { return slug; }
    public void setSlug(String slug) { this.slug = slug; }

    public String getBeschreibung() { return beschreibung; }
    public void setBeschreibung(String beschreibung) { this.beschreibung = beschreibung; }

    public String getOrt() { return ort; }
    public void setOrt(String ort) { this.ort = ort; }

    public LocalDate getDatum() { return datum; }
    public void setDatum(LocalDate datum) { this.datum = datum; }

    public LocalDate getDatumEnde() { return datumEnde; }
    public void setDatumEnde(LocalDate datumEnde) { this.datumEnde = datumEnde; }

    public Integer getKapazitaet() { return kapazitaet; }
    public void setKapazitaet(Integer kapazitaet) { this.kapazitaet = kapazitaet; }

    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Event that)) return false;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() { return Objects.hash(id); }
}

