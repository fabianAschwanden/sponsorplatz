package ch.sponsorplatz.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Organisation — Wurzel-Entität für Vereine, Unternehmen, Stiftungen.
 *
 * Wird im kollaborativen Modell als Edit-Marker für nachgelagerte Daten verwendet
 * (siehe ROLLENKONZEPT.md). Lese-Sichtbarkeit ist offen.
 */
@Entity
@Table(name = "organisation")
public class Organisation {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(name = "typ", nullable = false, length = 20)
    private OrgTyp typ;

    @Column(name = "name", nullable = false, length = 255)
    private String name;

    @Column(name = "slug", nullable = false, length = 120, unique = true)
    private String slug;

    @Column(name = "rechtsform", length = 50)
    private String rechtsform;

    @Enumerated(EnumType.STRING)
    @Column(name = "branche", nullable = false, length = 50)
    private Branche branche;

    @Column(name = "beschreibung", columnDefinition = "TEXT")
    private String beschreibung;

    @Column(name = "website_url", length = 500)
    private String websiteUrl;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private OrgStatus status = OrgStatus.PENDING;

    @Column(name = "verifiziert_am")
    private Instant verifiziertAm;

    @Column(name = "zefix_uid", length = 20)
    private String zefixUid;

    @Column(name = "iban", length = 34)
    private String iban;

    @Column(name = "strasse", length = 70)
    private String strasse;

    @Column(name = "postleitzahl", length = 16)
    private String postleitzahl;

    @Column(name = "ort", length = 70)
    private String ort;

    @Column(name = "registriert_am", nullable = false, updatable = false)
    private Instant registriertAm;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    void onCreate() {
        Instant jetzt = Instant.now();
        if (registriertAm == null) {
            registriertAm = jetzt;
        }
        createdAt = jetzt;
        updatedAt = jetzt;
        if (status == null) {
            status = OrgStatus.PENDING;
        }
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }

    // --- Getter / Setter ---

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public OrgTyp getTyp() {
        return typ;
    }

    public void setTyp(OrgTyp typ) {
        this.typ = typ;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSlug() {
        return slug;
    }

    public void setSlug(String slug) {
        this.slug = slug;
    }

    public String getRechtsform() {
        return rechtsform;
    }

    public void setRechtsform(String rechtsform) {
        this.rechtsform = rechtsform;
    }

    public Branche getBranche() {
        return branche;
    }

    public void setBranche(Branche branche) {
        this.branche = branche;
    }

    public String getBeschreibung() {
        return beschreibung;
    }

    public void setBeschreibung(String beschreibung) {
        this.beschreibung = beschreibung;
    }

    public String getWebsiteUrl() {
        return websiteUrl;
    }

    public void setWebsiteUrl(String websiteUrl) {
        this.websiteUrl = websiteUrl;
    }

    public OrgStatus getStatus() {
        return status;
    }

    public void setStatus(OrgStatus status) {
        this.status = status;
    }

    public Instant getVerifiziertAm() {
        return verifiziertAm;
    }

    public void setVerifiziertAm(Instant verifiziertAm) {
        this.verifiziertAm = verifiziertAm;
    }

    public String getZefixUid() {
        return zefixUid;
    }

    public void setZefixUid(String zefixUid) {
        this.zefixUid = zefixUid;
    }

    public String getIban() {
        return iban;
    }

    public void setIban(String iban) {
        this.iban = iban;
    }

    public String getStrasse() { return strasse; }
    public void setStrasse(String strasse) { this.strasse = strasse; }

    public String getPostleitzahl() { return postleitzahl; }
    public void setPostleitzahl(String postleitzahl) { this.postleitzahl = postleitzahl; }

    public String getOrt() { return ort; }
    public void setOrt(String ort) { this.ort = ort; }

    public Instant getRegistriertAm() {
        return registriertAm;
    }

    public void setRegistriertAm(Instant registriertAm) {
        this.registriertAm = registriertAm;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Organisation that)) return false;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "Organisation{id=" + id + ", slug='" + slug + "', typ=" + typ + "}";
    }
}
