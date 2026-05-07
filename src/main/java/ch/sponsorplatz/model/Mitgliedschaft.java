package ch.sponsorplatz.model;
import ch.sponsorplatz.benutzer.AppUser;

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
import java.util.Objects;
import java.util.UUID;

/**
 * Verknüpft einen AppUser mit einer Organisation und weist eine Rolle zu.
 * Pro User–Org-Paar ist genau ein Eintrag erlaubt.
 */
@Entity
@Table(name = "mitgliedschaft", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"user_id", "org_id"})
})
public class Mitgliedschaft {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private AppUser user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "org_id", nullable = false)
    private Organisation org;

    @Enumerated(EnumType.STRING)
    @Column(name = "rolle", nullable = false, length = 20)
    private Rolle rolle;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "eingeladen_von")
    private AppUser eingeladenVon;

    @Column(name = "beigetreten_am", nullable = false, updatable = false)
    private Instant beigetretenAm;

    @PrePersist
    void onCreate() {
        if (beigetretenAm == null) {
            beigetretenAm = Instant.now();
        }
    }

    // --- Getter / Setter ---

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public AppUser getUser() { return user; }
    public void setUser(AppUser user) { this.user = user; }

    public Organisation getOrg() { return org; }
    public void setOrg(Organisation org) { this.org = org; }

    public Rolle getRolle() { return rolle; }
    public void setRolle(Rolle rolle) { this.rolle = rolle; }

    public AppUser getEingeladenVon() { return eingeladenVon; }
    public void setEingeladenVon(AppUser eingeladenVon) { this.eingeladenVon = eingeladenVon; }

    public Instant getBeigetretenAm() { return beigetretenAm; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Mitgliedschaft that)) return false;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() { return Objects.hash(id); }

    @Override
    public String toString() {
        return "Mitgliedschaft{id=" + id + ", rolle=" + rolle + "}";
    }
}

