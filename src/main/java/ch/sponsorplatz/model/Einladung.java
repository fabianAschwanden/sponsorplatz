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
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.Instant;
import java.util.UUID;

/**
 * Einladung eines Benutzers zu einer Organisation.
 * Token-basiert: der Eingeladene klickt einen Link und wird Mitglied.
 */
@Entity
@Table(name = "einladung", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"org_id", "email"}),
        @UniqueConstraint(columnNames = {"token"})
})
public class Einladung {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "org_id", nullable = false)
    private Organisation org;

    @Column(name = "email", nullable = false, length = 255)
    private String email;

    @Enumerated(EnumType.STRING)
    @Column(name = "rolle", nullable = false, length = 20)
    private Rolle rolle;

    @Column(name = "token", nullable = false, length = 64, unique = true)
    private String token;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "eingeladen_von", nullable = false)
    private AppUser eingeladenVon;

    @Column(name = "gueltig_bis", nullable = false)
    private Instant gueltigBis;

    @Column(name = "angenommen_am")
    private Instant angenommenAm;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }

    // --- Getter / Setter ---

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public Organisation getOrg() { return org; }
    public void setOrg(Organisation org) { this.org = org; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public Rolle getRolle() { return rolle; }
    public void setRolle(Rolle rolle) { this.rolle = rolle; }

    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }

    public AppUser getEingeladenVon() { return eingeladenVon; }
    public void setEingeladenVon(AppUser eingeladenVon) { this.eingeladenVon = eingeladenVon; }

    public Instant getGueltigBis() { return gueltigBis; }
    public void setGueltigBis(Instant gueltigBis) { this.gueltigBis = gueltigBis; }

    public Instant getAngenommenAm() { return angenommenAm; }
    public void setAngenommenAm(Instant angenommenAm) { this.angenommenAm = angenommenAm; }

    public Instant getCreatedAt() { return createdAt; }
}

