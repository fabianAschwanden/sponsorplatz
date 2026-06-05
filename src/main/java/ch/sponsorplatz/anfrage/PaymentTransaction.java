package ch.sponsorplatz.anfrage;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Persistierte Zahlungs-Transaktion aus Provider-Interaktion (Init + Webhook).
 * Dient als Audit-Spur und Idempotenz-Schutz (UNIQUE auf provider + provider_reference).
 */
@Entity
@Table(name = "payment_transaction")
public class PaymentTransaction {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rechnung_id", nullable = false)
    private Rechnung rechnung;

    @Column(name = "provider", nullable = false, length = 50)
    private String provider;

    @Column(name = "provider_reference", nullable = false, length = 255)
    private String providerReference;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private PaymentTransactionStatus status = PaymentTransactionStatus.ERSTELLT;

    @Column(name = "betrag_chf", nullable = false, precision = 10, scale = 2)
    private BigDecimal betragChf;

    @Column(name = "checkout_url", length = 2000)
    private String checkoutUrl;

    @Column(name = "raw_payload", columnDefinition = "TEXT")
    private String rawPayload;

    @Column(name = "erstellt_am", nullable = false)
    private Instant erstelltAm;

    @Column(name = "aktualisiert_am")
    private Instant aktualisiertAm;

    @PrePersist
    void initId() {
        if (this.id == null) this.id = UUID.randomUUID();
        if (this.erstelltAm == null) this.erstelltAm = Instant.now();
    }

    // --- Getter / Setter ---

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public Rechnung getRechnung() { return rechnung; }
    public void setRechnung(Rechnung rechnung) { this.rechnung = rechnung; }

    public String getProvider() { return provider; }
    public void setProvider(String provider) { this.provider = provider; }

    public String getProviderReference() { return providerReference; }
    public void setProviderReference(String providerReference) { this.providerReference = providerReference; }

    public PaymentTransactionStatus getStatus() { return status; }
    public void setStatus(PaymentTransactionStatus status) { this.status = status; }

    public BigDecimal getBetragChf() { return betragChf; }
    public void setBetragChf(BigDecimal betragChf) { this.betragChf = betragChf; }

    public String getCheckoutUrl() { return checkoutUrl; }
    public void setCheckoutUrl(String checkoutUrl) { this.checkoutUrl = checkoutUrl; }

    public String getRawPayload() { return rawPayload; }
    public void setRawPayload(String rawPayload) { this.rawPayload = rawPayload; }

    public Instant getErstelltAm() { return erstelltAm; }
    public void setErstelltAm(Instant erstelltAm) { this.erstelltAm = erstelltAm; }

    public Instant getAktualisiertAm() { return aktualisiertAm; }
    public void setAktualisiertAm(Instant aktualisiertAm) { this.aktualisiertAm = aktualisiertAm; }
}

