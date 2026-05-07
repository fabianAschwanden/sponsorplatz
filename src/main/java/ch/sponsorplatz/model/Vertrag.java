package ch.sponsorplatz.model;
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
 * Sponsoring-Vertrag, aus einer angenommenen {@link SponsoringAnfrage}
 * erzeugt. Snapshot-Felder (orgName/sponsorName/paketName/preisChf) werden
 * bei der Erstellung kopiert — der Vertrag bleibt damit konsistent, auch
 * wenn Org-Profil oder Paket später geändert werden.
 */
@Entity
@Table(name = "vertrag")
public class Vertrag {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "anfrage_id", nullable = false, unique = true)
    private SponsoringAnfrage anfrage;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private VertragsStatus status = VertragsStatus.ENTWURF;

    @Column(name = "org_name", nullable = false)
    private String orgName;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "org_id", nullable = false)
    private Organisation org;

    @Column(name = "sponsor_name")
    private String sponsorName;

    @Column(name = "sponsor_email")
    private String sponsorEmail;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sponsor_org_id")
    private Organisation sponsorOrg;

    @Column(name = "paket_name", nullable = false)
    private String paketName;

    @Column(name = "paket_beschreibung", columnDefinition = "TEXT")
    private String paketBeschreibung;

    @Column(name = "preis_chf", nullable = false, precision = 10, scale = 2)
    private BigDecimal preisChf;

    @Column(name = "laufzeit_von")
    private LocalDate laufzeitVon;

    @Column(name = "laufzeit_bis")
    private LocalDate laufzeitBis;

    @Column(name = "leistung_verein", columnDefinition = "TEXT")
    private String leistungVerein;

    @Column(name = "leistung_sponsor", columnDefinition = "TEXT")
    private String leistungSponsor;

    @Column(name = "erstellt_am", nullable = false)
    private Instant erstelltAm = Instant.now();

    @Column(name = "erstellt_von")
    private String erstelltVon;

    @Column(name = "unterzeichnet_am")
    private Instant unterzeichnetAm;

    @Column(name = "unterzeichnet_von")
    private String unterzeichnetVon;

    @PrePersist
    void initId() {
        if (this.id == null) this.id = UUID.randomUUID();
        if (this.erstelltAm == null) this.erstelltAm = Instant.now();
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public SponsoringAnfrage getAnfrage() { return anfrage; }
    public void setAnfrage(SponsoringAnfrage anfrage) { this.anfrage = anfrage; }

    public VertragsStatus getStatus() { return status; }
    public void setStatus(VertragsStatus status) { this.status = status; }

    public String getOrgName() { return orgName; }
    public void setOrgName(String orgName) { this.orgName = orgName; }

    public Organisation getOrg() { return org; }
    public void setOrg(Organisation org) { this.org = org; }

    public String getSponsorName() { return sponsorName; }
    public void setSponsorName(String sponsorName) { this.sponsorName = sponsorName; }

    public String getSponsorEmail() { return sponsorEmail; }
    public void setSponsorEmail(String sponsorEmail) { this.sponsorEmail = sponsorEmail; }

    public Organisation getSponsorOrg() { return sponsorOrg; }
    public void setSponsorOrg(Organisation sponsorOrg) { this.sponsorOrg = sponsorOrg; }

    public String getPaketName() { return paketName; }
    public void setPaketName(String paketName) { this.paketName = paketName; }

    public String getPaketBeschreibung() { return paketBeschreibung; }
    public void setPaketBeschreibung(String paketBeschreibung) { this.paketBeschreibung = paketBeschreibung; }

    public BigDecimal getPreisChf() { return preisChf; }
    public void setPreisChf(BigDecimal preisChf) { this.preisChf = preisChf; }

    public LocalDate getLaufzeitVon() { return laufzeitVon; }
    public void setLaufzeitVon(LocalDate laufzeitVon) { this.laufzeitVon = laufzeitVon; }

    public LocalDate getLaufzeitBis() { return laufzeitBis; }
    public void setLaufzeitBis(LocalDate laufzeitBis) { this.laufzeitBis = laufzeitBis; }

    public String getLeistungVerein() { return leistungVerein; }
    public void setLeistungVerein(String leistungVerein) { this.leistungVerein = leistungVerein; }

    public String getLeistungSponsor() { return leistungSponsor; }
    public void setLeistungSponsor(String leistungSponsor) { this.leistungSponsor = leistungSponsor; }

    public Instant getErstelltAm() { return erstelltAm; }
    public void setErstelltAm(Instant erstelltAm) { this.erstelltAm = erstelltAm; }

    public String getErstelltVon() { return erstelltVon; }
    public void setErstelltVon(String erstelltVon) { this.erstelltVon = erstelltVon; }

    public Instant getUnterzeichnetAm() { return unterzeichnetAm; }
    public void setUnterzeichnetAm(Instant unterzeichnetAm) { this.unterzeichnetAm = unterzeichnetAm; }

    public String getUnterzeichnetVon() { return unterzeichnetVon; }
    public void setUnterzeichnetVon(String unterzeichnetVon) { this.unterzeichnetVon = unterzeichnetVon; }
}
