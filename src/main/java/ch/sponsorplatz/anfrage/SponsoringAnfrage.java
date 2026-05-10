package ch.sponsorplatz.anfrage;
import ch.sponsorplatz.projekt.SponsoringPaket;
import ch.sponsorplatz.organisation.Organisation;

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
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "sponsoring_anfrage")
public class SponsoringAnfrage {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    /**
     * Paket-Referenz. Bei klassischer Sponsoring-Anfrage (Sponsor → Verein
     * via Marktplatz-Detail) immer gesetzt. Bei Kontakt-Anfrage (Verein →
     * Sponsor via /anfragen-Bereich) {@code null}, weil keine Paket-Bindung
     * existiert — dann muss {@link #betreff} gesetzt sein.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "paket_id")
    private SponsoringPaket paket;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "anfragender_org_id", nullable = false)
    private Organisation anfragenderOrg;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "empfaenger_org_id", nullable = false)
    private Organisation empfaengerOrg;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private AnfrageStatus status = AnfrageStatus.NEU;

    /**
     * Betreff — nur bei Kontakt-Anfrage gesetzt (paket == null). Bei
     * klassischer Paket-Anfrage NULL, der Paket-Name dient als Betreff.
     */
    @Column(name = "betreff", length = 255)
    private String betreff;

    @Column(name = "nachricht", columnDefinition = "TEXT")
    private String nachricht;

    @Column(name = "antwort", columnDefinition = "TEXT")
    private String antwort;

    @Column(name = "kontakt_name", length = 255)
    private String kontaktName;

    @Column(name = "kontakt_email", length = 255)
    private String kontaktEmail;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "beantwortet_am")
    private Instant beantwortetAm;

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

    public SponsoringPaket getPaket() { return paket; }
    public void setPaket(SponsoringPaket paket) { this.paket = paket; }

    public Organisation getAnfragenderOrg() { return anfragenderOrg; }
    public void setAnfragenderOrg(Organisation anfragenderOrg) { this.anfragenderOrg = anfragenderOrg; }

    public Organisation getEmpfaengerOrg() { return empfaengerOrg; }
    public void setEmpfaengerOrg(Organisation empfaengerOrg) { this.empfaengerOrg = empfaengerOrg; }

    public AnfrageStatus getStatus() { return status; }
    public void setStatus(AnfrageStatus status) { this.status = status; }

    public String getBetreff() { return betreff; }
    public void setBetreff(String betreff) { this.betreff = betreff; }

    public String getNachricht() { return nachricht; }
    public void setNachricht(String nachricht) { this.nachricht = nachricht; }

    public String getAntwort() { return antwort; }
    public void setAntwort(String antwort) { this.antwort = antwort; }

    public String getKontaktName() { return kontaktName; }
    public void setKontaktName(String kontaktName) { this.kontaktName = kontaktName; }

    public String getKontaktEmail() { return kontaktEmail; }
    public void setKontaktEmail(String kontaktEmail) { this.kontaktEmail = kontaktEmail; }

    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }

    public Instant getBeantwortetAm() { return beantwortetAm; }
    public void setBeantwortetAm(Instant beantwortetAm) { this.beantwortetAm = beantwortetAm; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SponsoringAnfrage that)) return false;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() { return Objects.hash(id); }
}

