package ch.sponsorplatz.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "projekt")
public class Projekt {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "org_id", nullable = false)
    private Organisation org;

    @Column(name = "name", nullable = false, length = 255)
    private String name;

    @Column(name = "slug", nullable = false, length = 120, unique = true)
    private String slug;

    @Column(name = "beschreibung", columnDefinition = "TEXT")
    private String beschreibung;

    @Enumerated(EnumType.STRING)
    @Column(name = "sichtbarkeit", nullable = false, length = 20)
    private Sichtbarkeit sichtbarkeit = Sichtbarkeit.ENTWURF;

    @Column(name = "kategorie", length = 50)
    private String kategorie;

    @Column(name = "ort", length = 100)
    private String ort;

    @Column(name = "start_datum")
    private LocalDate startDatum;

    @Column(name = "end_datum")
    private LocalDate endDatum;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "veroeffentlicht_am")
    private Instant veroeffentlichtAm;

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

    public Sichtbarkeit getSichtbarkeit() { return sichtbarkeit; }
    public void setSichtbarkeit(Sichtbarkeit sichtbarkeit) { this.sichtbarkeit = sichtbarkeit; }

    public String getKategorie() { return kategorie; }
    public void setKategorie(String kategorie) { this.kategorie = kategorie; }

    public String getOrt() { return ort; }
    public void setOrt(String ort) { this.ort = ort; }

    public LocalDate getStartDatum() { return startDatum; }
    public void setStartDatum(LocalDate startDatum) { this.startDatum = startDatum; }

    public LocalDate getEndDatum() { return endDatum; }
    public void setEndDatum(LocalDate endDatum) { this.endDatum = endDatum; }

    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }

    public Instant getVeroeffentlichtAm() { return veroeffentlichtAm; }
    public void setVeroeffentlichtAm(Instant veroeffentlichtAm) { this.veroeffentlichtAm = veroeffentlichtAm; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Projekt that)) return false;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() { return Objects.hash(id); }
}

