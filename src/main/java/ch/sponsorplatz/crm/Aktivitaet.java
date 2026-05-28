package ch.sponsorplatz.crm;

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

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * CRM-Aktivität (Dynamics „Activity") in der Interaktions-Timeline eines
 * {@link SponsorAccount}. Bezieht sich immer auf einen Account, optional auf
 * eine {@link KontaktPerson} (Dynamics „regarding contact").
 *
 * <p><b>Isolation:</b> {@code besitzerSponsorOrgId} (denormalisiert vom Account)
 * ist der Mandanten-Schlüssel; Zugriff ausschliesslich über
 * {@code AktivitaetService} + {@code AccessControl.kannSponsorDatenSehen}.
 */
@Entity
@Table(name = "aktivitaet")
public class Aktivitaet {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "besitzer_sponsor_org_id", nullable = false)
    private UUID besitzerSponsorOrgId;

    /** Dynamics 'regarding' (Account) — die Aktivität gehört zu diesem Account. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sponsor_account_id", nullable = false)
    private SponsorAccount account;

    /** Optionaler 'regarding contact' — die Aktivität betraf diese Person. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "kontakt_person_id")
    private KontaktPerson kontaktPerson;

    @Enumerated(EnumType.STRING)
    @Column(name = "typ", nullable = false, length = 30)
    private AktivitaetTyp typ = AktivitaetTyp.NOTIZ;

    /** Fachliches Datum der Interaktion (vom User gesetzt). */
    @Column(name = "datum", nullable = false)
    private LocalDate datum;

    @Column(name = "betreff", nullable = false, length = 200)
    private String betreff;

    @Column(name = "notiz")
    private String notiz;

    @Column(name = "erstellt_am", nullable = false, updatable = false)
    private Instant erstelltAm;

    @Column(name = "erstellt_von_user_id")
    private UUID erstelltVonUserId;

    @PrePersist
    void prePersist() {
        if (erstelltAm == null) erstelltAm = Instant.now();
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getBesitzerSponsorOrgId() { return besitzerSponsorOrgId; }
    public void setBesitzerSponsorOrgId(UUID besitzerSponsorOrgId) { this.besitzerSponsorOrgId = besitzerSponsorOrgId; }

    public SponsorAccount getAccount() { return account; }
    public void setAccount(SponsorAccount account) { this.account = account; }

    public KontaktPerson getKontaktPerson() { return kontaktPerson; }
    public void setKontaktPerson(KontaktPerson kontaktPerson) { this.kontaktPerson = kontaktPerson; }

    public AktivitaetTyp getTyp() { return typ; }
    public void setTyp(AktivitaetTyp typ) { this.typ = typ; }

    public LocalDate getDatum() { return datum; }
    public void setDatum(LocalDate datum) { this.datum = datum; }

    public String getBetreff() { return betreff; }
    public void setBetreff(String betreff) { this.betreff = betreff; }

    public String getNotiz() { return notiz; }
    public void setNotiz(String notiz) { this.notiz = notiz; }

    public Instant getErstelltAm() { return erstelltAm; }
    public void setErstelltAm(Instant erstelltAm) { this.erstelltAm = erstelltAm; }

    public UUID getErstelltVonUserId() { return erstelltVonUserId; }
    public void setErstelltVonUserId(UUID erstelltVonUserId) { this.erstelltVonUserId = erstelltVonUserId; }
}
