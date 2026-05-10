package ch.sponsorplatz.benutzer;

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
 * Verknüpfung zwischen einem {@link AppUser} und seinem Identity-Provider-Subject.
 *
 * <p>Der DB-Lookup-Pfad ist {@code (provider, subject)} — siehe UNIQUE-Constraint.
 * Email wird zur Diagnostik mitgespeichert (Spalte {@code email_at_provider}),
 * ist aber NICHT der Identifier — der subject vom IdP ist stabil, die Email kann
 * sich ändern.
 *
 * @see <a href="../../../../../specs/AUTH_SSO_OIDC.md">AUTH_SSO_OIDC.md §5.3</a>
 */
@Entity
@Table(name = "federierte_identitaet",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_provider_subject",
                columnNames = {"provider", "subject"}))
public class FederierteIdentitaet {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private AppUser user;

    @Enumerated(EnumType.STRING)
    @Column(name = "provider", nullable = false, length = 50)
    private IdentityProvider provider;

    @Column(name = "subject", nullable = false, length = 255)
    private String subject;

    @Column(name = "email_at_provider", length = 255)
    private String emailAtProvider;

    @Column(name = "verbunden_am", nullable = false, updatable = false)
    private Instant verbundenAm;

    @Column(name = "letzter_login_am")
    private Instant letzterLoginAm;

    @PrePersist
    void onCreate() {
        if (verbundenAm == null) {
            verbundenAm = Instant.now();
        }
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public AppUser getUser() { return user; }
    public void setUser(AppUser user) { this.user = user; }

    public IdentityProvider getProvider() { return provider; }
    public void setProvider(IdentityProvider provider) { this.provider = provider; }

    public String getSubject() { return subject; }
    public void setSubject(String subject) { this.subject = subject; }

    public String getEmailAtProvider() { return emailAtProvider; }
    public void setEmailAtProvider(String emailAtProvider) { this.emailAtProvider = emailAtProvider; }

    public Instant getVerbundenAm() { return verbundenAm; }
    public void setVerbundenAm(Instant verbundenAm) { this.verbundenAm = verbundenAm; }

    public Instant getLetzterLoginAm() { return letzterLoginAm; }
    public void setLetzterLoginAm(Instant letzterLoginAm) { this.letzterLoginAm = letzterLoginAm; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof FederierteIdentitaet that)) return false;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() { return Objects.hash(id); }
}
