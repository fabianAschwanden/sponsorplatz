package ch.sponsorplatz.anfrage;
import ch.sponsorplatz.organisation.Organisation;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Sponsoring-Rechnung aus einem unterzeichneten Vertrag — mit Swiss QR-Bill
 * für Banking-App-Bezahlung.
 *
 * <p>Snapshot-Felder: IBAN, Betrag und Sponsor-Adresse werden bei der
 * Erstellung kopiert; nachträgliche Änderungen am Vertrag oder Org-Profil
 * wirken nicht zurück.
 */
@Entity
@Table(name = "rechnung")
public class Rechnung {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vertrag_id", nullable = false, unique = true)
    private Vertrag vertrag;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "org_id", nullable = false)
    private Organisation org;

    @Column(name = "rechnungsnummer", nullable = false, length = 50)
    private String rechnungsnummer;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private RechnungsStatus status = RechnungsStatus.OFFEN;

    @Column(name = "betrag_chf", nullable = false, precision = 10, scale = 2)
    private BigDecimal betragChf;

    @Column(name = "iban", nullable = false, length = 34)
    private String iban;

    @Column(name = "qr_referenz", length = 27)
    private String qrReferenz;

    @Column(name = "sponsor_name", nullable = false)
    private String sponsorName;

    @Column(name = "sponsor_email")
    private String sponsorEmail;

    @Column(name = "sponsor_adresse", length = 500)
    private String sponsorAdresse;

    @Column(name = "zahlungszweck", length = 140)
    private String zahlungszweck;

    @Column(name = "erstellt_am", nullable = false)
    private Instant erstelltAm = Instant.now();

    @Column(name = "erstellt_von")
    private String erstelltVon;

    @Column(name = "faellig_am", nullable = false)
    private LocalDate faelligAm;

    @Column(name = "bezahlt_am")
    private Instant bezahltAm;

    @Column(name = "bezahlt_von")
    private String bezahltVon;

    @PrePersist
    void initId() {
        if (this.id == null) this.id = UUID.randomUUID();
        if (this.erstelltAm == null) this.erstelltAm = Instant.now();
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public Vertrag getVertrag() { return vertrag; }
    public void setVertrag(Vertrag vertrag) { this.vertrag = vertrag; }

    public Organisation getOrg() { return org; }
    public void setOrg(Organisation org) { this.org = org; }

    public String getRechnungsnummer() { return rechnungsnummer; }
    public void setRechnungsnummer(String rechnungsnummer) { this.rechnungsnummer = rechnungsnummer; }

    public RechnungsStatus getStatus() { return status; }
    public void setStatus(RechnungsStatus status) { this.status = status; }

    public BigDecimal getBetragChf() { return betragChf; }
    public void setBetragChf(BigDecimal betragChf) { this.betragChf = betragChf; }

    public String getIban() { return iban; }
    public void setIban(String iban) { this.iban = iban; }

    public String getQrReferenz() { return qrReferenz; }
    public void setQrReferenz(String qrReferenz) { this.qrReferenz = qrReferenz; }

    public String getSponsorName() { return sponsorName; }
    public void setSponsorName(String sponsorName) { this.sponsorName = sponsorName; }

    public String getSponsorEmail() { return sponsorEmail; }
    public void setSponsorEmail(String sponsorEmail) { this.sponsorEmail = sponsorEmail; }

    public String getSponsorAdresse() { return sponsorAdresse; }
    public void setSponsorAdresse(String sponsorAdresse) { this.sponsorAdresse = sponsorAdresse; }

    public String getZahlungszweck() { return zahlungszweck; }
    public void setZahlungszweck(String zahlungszweck) { this.zahlungszweck = zahlungszweck; }

    public Instant getErstelltAm() { return erstelltAm; }
    public void setErstelltAm(Instant erstelltAm) { this.erstelltAm = erstelltAm; }

    public String getErstelltVon() { return erstelltVon; }
    public void setErstelltVon(String erstelltVon) { this.erstelltVon = erstelltVon; }

    public LocalDate getFaelligAm() { return faelligAm; }
    public void setFaelligAm(LocalDate faelligAm) { this.faelligAm = faelligAm; }

    public Instant getBezahltAm() { return bezahltAm; }
    public void setBezahltAm(Instant bezahltAm) { this.bezahltAm = bezahltAm; }

    public String getBezahltVon() { return bezahltVon; }
    public void setBezahltVon(String bezahltVon) { this.bezahltVon = bezahltVon; }
}
