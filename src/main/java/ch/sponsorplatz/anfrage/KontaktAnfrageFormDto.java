package ch.sponsorplatz.anfrage;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Form-DTO für eine Kontakt-Anfrage (Verein → Sponsor).
 *
 * <p>Im Gegensatz zu {@link AnfrageFormDto} ist hier kein Paket gebunden —
 * der Verein wählt einen Sponsor und beschreibt sein Anliegen frei.
 */
public class KontaktAnfrageFormDto {

    @NotNull(message = "Bitte einen Sponsor auswählen")
    private UUID empfaengerOrgId;

    @NotNull(message = "Bitte eine eigene Org auswählen")
    private UUID anfragenderOrgId;

    @NotBlank(message = "Betreff ist Pflicht")
    @Size(min = 3, max = 255, message = "Betreff muss zwischen 3 und 255 Zeichen lang sein")
    private String betreff;

    @NotBlank(message = "Nachricht ist Pflicht")
    @Size(min = 10, max = 2000, message = "Nachricht muss zwischen 10 und 2000 Zeichen lang sein")
    private String nachricht;

    @Size(max = 255)
    private String kontaktName;

    @Email(message = "Ungültige E-Mail-Adresse")
    @Size(max = 255)
    private String kontaktEmail;

    /**
     * Optionaler Wunsch-Betrag in CHF. {@code null} = kein Richtbetrag,
     * der Verein-Owner ergänzt ihn beim Vertrag-Edit-Form. {@code 0} ist
     * erlaubt (Naturalien-Sponsoring). Max 9'999'999.99 entspricht der
     * DB-Spalte NUMERIC(12, 2).
     */
    @DecimalMin(value = "0", message = "Wunsch-Betrag darf nicht negativ sein")
    @DecimalMax(value = "9999999.99", message = "Wunsch-Betrag zu hoch")
    private BigDecimal wunschBetragChf;

    public UUID getEmpfaengerOrgId() { return empfaengerOrgId; }
    public void setEmpfaengerOrgId(UUID empfaengerOrgId) { this.empfaengerOrgId = empfaengerOrgId; }

    public UUID getAnfragenderOrgId() { return anfragenderOrgId; }
    public void setAnfragenderOrgId(UUID anfragenderOrgId) { this.anfragenderOrgId = anfragenderOrgId; }

    public String getBetreff() { return betreff; }
    public void setBetreff(String betreff) { this.betreff = betreff; }

    public String getNachricht() { return nachricht; }
    public void setNachricht(String nachricht) { this.nachricht = nachricht; }

    public String getKontaktName() { return kontaktName; }
    public void setKontaktName(String kontaktName) { this.kontaktName = kontaktName; }

    public String getKontaktEmail() { return kontaktEmail; }
    public void setKontaktEmail(String kontaktEmail) { this.kontaktEmail = kontaktEmail; }

    public BigDecimal getWunschBetragChf() { return wunschBetragChf; }
    public void setWunschBetragChf(BigDecimal wunschBetragChf) { this.wunschBetragChf = wunschBetragChf; }
}
