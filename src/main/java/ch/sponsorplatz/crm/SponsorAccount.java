package ch.sponsorplatz.crm;

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
import java.util.UUID;

/**
 * CRM-Account: die Beziehung einer Sponsor-Org zu einem gesponserten Verein
 * (ADR-0011 private Sponsor-Layer).
 *
 * <p><b>Isolation:</b> {@code besitzerSponsorOrgId} ist der Mandanten-Schlüssel.
 * Zugriff läuft ausschliesslich über {@code SponsorAccountService}, der jeden
 * Query gegen {@code AccessControl.kannSponsorDatenSehen} absichert. Diese
 * Entität darf NIE im Marktplatz oder bei anderen Sponsoren erscheinen.
 */
@Entity
@Table(name = "sponsor_account")
public class SponsorAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /** Mandanten-Schlüssel: Sponsor-Org, der diese CRM-Daten gehören. */
    @Column(name = "besitzer_sponsor_org_id", nullable = false)
    private UUID besitzerSponsorOrgId;

    /** Der gesponserte Verein (kollaborative Layer) — als Navigation für den View. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "verein_org_id", nullable = false)
    private Organisation verein;

    /** Account-Verantwortliche/r im Sponsor-Team (optional). */
    @Column(name = "account_owner_user_id")
    private UUID accountOwnerUserId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private AccountStatus status = AccountStatus.LEAD;

    @Enumerated(EnumType.STRING)
    @Column(name = "tier", length = 20)
    private AccountTier tier;

    /** Vertriebs-Pipeline-Stufe (CRM-Lücke #4). LEAD als Start. */
    @Enumerated(EnumType.STRING)
    @Column(name = "pipeline_stage", nullable = false, length = 20)
    private PipelineStage pipelineStage = PipelineStage.LEAD;

    /** Erwartetes Sponsoring-Volumen dieses Deals (CHF, optional). */
    @Column(name = "forecast_betrag_chf", precision = 12, scale = 2)
    private java.math.BigDecimal forecastBetragChf;

    @Column(name = "notiz")
    private String notiz;

    @Column(name = "erstellt_am", nullable = false, updatable = false)
    private Instant erstelltAm;

    @Column(name = "aktualisiert_am")
    private Instant aktualisiertAm;

    @PrePersist
    void prePersist() {
        if (erstelltAm == null) erstelltAm = Instant.now();
    }

    @PreUpdate
    void preUpdate() {
        aktualisiertAm = Instant.now();
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getBesitzerSponsorOrgId() { return besitzerSponsorOrgId; }
    public void setBesitzerSponsorOrgId(UUID besitzerSponsorOrgId) { this.besitzerSponsorOrgId = besitzerSponsorOrgId; }

    public Organisation getVerein() { return verein; }
    public void setVerein(Organisation verein) { this.verein = verein; }

    public UUID getAccountOwnerUserId() { return accountOwnerUserId; }
    public void setAccountOwnerUserId(UUID accountOwnerUserId) { this.accountOwnerUserId = accountOwnerUserId; }

    public AccountStatus getStatus() { return status; }
    public void setStatus(AccountStatus status) { this.status = status; }

    public AccountTier getTier() { return tier; }
    public void setTier(AccountTier tier) { this.tier = tier; }

    public PipelineStage getPipelineStage() { return pipelineStage; }
    public void setPipelineStage(PipelineStage pipelineStage) { this.pipelineStage = pipelineStage; }

    public java.math.BigDecimal getForecastBetragChf() { return forecastBetragChf; }
    public void setForecastBetragChf(java.math.BigDecimal forecastBetragChf) { this.forecastBetragChf = forecastBetragChf; }

    public String getNotiz() { return notiz; }
    public void setNotiz(String notiz) { this.notiz = notiz; }

    public Instant getErstelltAm() { return erstelltAm; }
    public void setErstelltAm(Instant erstelltAm) { this.erstelltAm = erstelltAm; }

    public Instant getAktualisiertAm() { return aktualisiertAm; }
    public void setAktualisiertAm(Instant aktualisiertAm) { this.aktualisiertAm = aktualisiertAm; }
}
