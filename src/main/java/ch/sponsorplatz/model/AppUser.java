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
 * Plattform-Benutzer. Passwort wird nie im Klartext gespeichert (BCrypt).
 */
@Entity
@Table(name = "app_user")
public class AppUser {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "email", nullable = false, length = 255, unique = true)
    private String email;

    @Column(name = "passwort_hash", nullable = false, length = 255)
    private String passwortHash;

    @Column(name = "anzeigename", nullable = false, length = 100)
    private String anzeigename;

    @Enumerated(EnumType.STRING)
    @Column(name = "platform_rolle", length = 30)
    private PlatformRolle platformRolle;

    @Column(name = "aktiv", nullable = false)
    private boolean aktiv = true;

    @Column(name = "email_verifiziert", nullable = false)
    private boolean emailVerifiziert = false;

    @Column(name = "verifikations_token", length = 64)
    private String verifikationsToken;

    @Column(name = "token_gueltig_bis")
    private Instant tokenGueltigBis;

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
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }

    // --- Getter / Setter ---

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPasswortHash() { return passwortHash; }
    public void setPasswortHash(String passwortHash) { this.passwortHash = passwortHash; }

    public String getAnzeigename() { return anzeigename; }
    public void setAnzeigename(String anzeigename) { this.anzeigename = anzeigename; }

    public PlatformRolle getPlatformRolle() { return platformRolle; }
    public void setPlatformRolle(PlatformRolle platformRolle) { this.platformRolle = platformRolle; }

    public boolean isAktiv() { return aktiv; }
    public void setAktiv(boolean aktiv) { this.aktiv = aktiv; }

    public boolean isEmailVerifiziert() { return emailVerifiziert; }
    public void setEmailVerifiziert(boolean emailVerifiziert) { this.emailVerifiziert = emailVerifiziert; }

    public String getVerifikationsToken() { return verifikationsToken; }
    public void setVerifikationsToken(String verifikationsToken) { this.verifikationsToken = verifikationsToken; }

    public Instant getTokenGueltigBis() { return tokenGueltigBis; }
    public void setTokenGueltigBis(Instant tokenGueltigBis) { this.tokenGueltigBis = tokenGueltigBis; }

    public Instant getRegistriertAm() { return registriertAm; }
    public void setRegistriertAm(Instant registriertAm) { this.registriertAm = registriertAm; }

    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof AppUser that)) return false;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() { return Objects.hash(id); }

    @Override
    public String toString() {
        return "AppUser{id=" + id + ", email='" + email + "'}";
    }
}

