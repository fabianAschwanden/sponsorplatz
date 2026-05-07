package ch.sponsorplatz.model;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * In-App-Benachrichtigung für einen Benutzer.
 */
@Entity
@Table(name = "benachrichtigung")
public class Benachrichtigung {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "empfaenger_id", nullable = false)
    private AppUser empfaenger;

    @Enumerated(EnumType.STRING)
    @Column(name = "typ", nullable = false, length = 50)
    private BenachrichtigungTyp typ;

    @Column(name = "titel", nullable = false, length = 255)
    private String titel;

    @Column(name = "text", columnDefinition = "TEXT")
    private String text;

    @Column(name = "link", length = 500)
    private String link;

    @Column(name = "gelesen", nullable = false)
    private boolean gelesen = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void onCreate() {
        if (createdAt == null) createdAt = Instant.now();
    }

    // --- Getter / Setter ---

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public AppUser getEmpfaenger() { return empfaenger; }
    public void setEmpfaenger(AppUser empfaenger) { this.empfaenger = empfaenger; }

    public BenachrichtigungTyp getTyp() { return typ; }
    public void setTyp(BenachrichtigungTyp typ) { this.typ = typ; }

    public String getTitel() { return titel; }
    public void setTitel(String titel) { this.titel = titel; }

    public String getText() { return text; }
    public void setText(String text) { this.text = text; }

    public String getLink() { return link; }
    public void setLink(String link) { this.link = link; }

    public boolean isGelesen() { return gelesen; }
    public void setGelesen(boolean gelesen) { this.gelesen = gelesen; }

    public Instant getCreatedAt() { return createdAt; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Benachrichtigung that)) return false;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() { return Objects.hash(id); }
}

