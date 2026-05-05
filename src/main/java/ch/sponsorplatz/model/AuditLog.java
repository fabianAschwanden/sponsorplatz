package ch.sponsorplatz.model;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Audit-Log Eintrag — erfasst alle relevanten Plattform-Aktionen.
 * Immutable nach Erstellung (kein Update).
 */
@Entity
@Table(name = "audit_log")
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "zeitpunkt", nullable = false, updatable = false)
    private Instant zeitpunkt;

    @Column(name = "aktion", nullable = false, updatable = false, length = 50)
    private String aktion;

    @Column(name = "bereich", nullable = false, updatable = false, length = 50)
    private String bereich;

    @Column(name = "benutzer_id", updatable = false)
    private UUID benutzerId;

    @Column(name = "benutzer_email", updatable = false, length = 255)
    private String benutzerEmail;

    @Column(name = "ziel_id", updatable = false)
    private UUID zielId;

    @Column(name = "ziel_typ", updatable = false, length = 50)
    private String zielTyp;

    @Column(name = "details", updatable = false, columnDefinition = "TEXT")
    private String details;

    @PrePersist
    void onCreate() {
        if (zeitpunkt == null) {
            zeitpunkt = Instant.now();
        }
    }

    // --- Getter / Setter ---

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public Instant getZeitpunkt() { return zeitpunkt; }
    public void setZeitpunkt(Instant zeitpunkt) { this.zeitpunkt = zeitpunkt; }

    public String getAktion() { return aktion; }
    public void setAktion(String aktion) { this.aktion = aktion; }

    public String getBereich() { return bereich; }
    public void setBereich(String bereich) { this.bereich = bereich; }

    public UUID getBenutzerId() { return benutzerId; }
    public void setBenutzerId(UUID benutzerId) { this.benutzerId = benutzerId; }

    public String getBenutzerEmail() { return benutzerEmail; }
    public void setBenutzerEmail(String benutzerEmail) { this.benutzerEmail = benutzerEmail; }

    public UUID getZielId() { return zielId; }
    public void setZielId(UUID zielId) { this.zielId = zielId; }

    public String getZielTyp() { return zielTyp; }
    public void setZielTyp(String zielTyp) { this.zielTyp = zielTyp; }

    public String getDetails() { return details; }
    public void setDetails(String details) { this.details = details; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof AuditLog that)) return false;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() { return Objects.hash(id); }
}

