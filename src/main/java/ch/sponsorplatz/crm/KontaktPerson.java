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
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/**
 * CRM-Kontakt (Dynamics „Contact") unter einem {@link SponsorAccount}. Externer
 * Ansprechpartner ohne Plattform-Account — nur Visitenkarten-Felder.
 *
 * <p><b>Isolation:</b> {@code besitzerSponsorOrgId} (denormalisiert vom Account)
 * ist der Mandanten-Schlüssel; Zugriff ausschliesslich über
 * {@code KontaktPersonService} + {@code AccessControl.kannSponsorDatenSehen}.
 */
@Entity
@Table(name = "kontakt_person")
public class KontaktPerson {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /** Mandanten-Schlüssel (denormalisiert vom Account). */
    @Column(name = "besitzer_sponsor_org_id", nullable = false)
    private UUID besitzerSponsorOrgId;

    /** Dynamics 'parentcustomerid' — der Account, zu dem der Kontakt gehört. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sponsor_account_id", nullable = false)
    private SponsorAccount account;

    @Column(name = "vorname", nullable = false, length = 120)
    private String vorname;

    @Column(name = "nachname", nullable = false, length = 120)
    private String nachname;

    /** Jobtitel / Funktion im Verein (z.B. „Präsident"). */
    @Column(name = "funktion", length = 160)
    private String funktion;

    @Enumerated(EnumType.STRING)
    @Column(name = "kontakt_rolle", nullable = false, length = 30)
    private KontaktRolle kontaktRolle = KontaktRolle.SONSTIGE;

    @Column(name = "email", length = 255)
    private String email;

    @Column(name = "telefon", length = 60)
    private String telefon;

    @Column(name = "mobile", length = 60)
    private String mobile;

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

    public SponsorAccount getAccount() { return account; }
    public void setAccount(SponsorAccount account) { this.account = account; }

    public String getVorname() { return vorname; }
    public void setVorname(String vorname) { this.vorname = vorname; }

    public String getNachname() { return nachname; }
    public void setNachname(String nachname) { this.nachname = nachname; }

    public String getFunktion() { return funktion; }
    public void setFunktion(String funktion) { this.funktion = funktion; }

    public KontaktRolle getKontaktRolle() { return kontaktRolle; }
    public void setKontaktRolle(KontaktRolle kontaktRolle) { this.kontaktRolle = kontaktRolle; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getTelefon() { return telefon; }
    public void setTelefon(String telefon) { this.telefon = telefon; }

    public String getMobile() { return mobile; }
    public void setMobile(String mobile) { this.mobile = mobile; }

    public String getNotiz() { return notiz; }
    public void setNotiz(String notiz) { this.notiz = notiz; }

    public Instant getErstelltAm() { return erstelltAm; }
    public void setErstelltAm(Instant erstelltAm) { this.erstelltAm = erstelltAm; }

    public Instant getAktualisiertAm() { return aktualisiertAm; }
    public void setAktualisiertAm(Instant aktualisiertAm) { this.aktualisiertAm = aktualisiertAm; }
}
