package ch.sponsorplatz.organisation;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Kombiniertes Form-DTO für Sponsor-Registrierung.
 * Erstellt in einem Schritt: AppUser + Organisation (UNTERNEHMEN) + Mitgliedschaft (ORG_OWNER).
 */
public class SponsorRegistrierungFormDto {

    // --- User-Felder ---

    @NotBlank(message = "E-Mail ist Pflicht")
    @Email(message = "Bitte eine gültige E-Mail-Adresse eingeben")
    private String email;

    @NotBlank(message = "Anzeigename ist Pflicht")
    @Size(min = 2, max = 100, message = "Anzeigename muss zwischen 2 und 100 Zeichen sein")
    private String anzeigename;

    @NotBlank(message = "Passwort ist Pflicht")
    @Size(min = 8, message = "Passwort muss mindestens 8 Zeichen haben")
    private String passwort;

    // --- Firmen-Felder ---

    @NotBlank(message = "Firmenname ist Pflicht")
    @Size(min = 2, max = 255, message = "Firmenname muss zwischen 2 und 255 Zeichen sein")
    private String firmenname;

    @NotNull(message = "Industrie ist Pflicht — in welcher Industrie ist Ihr Unternehmen tätig?")
    private SponsorBranche sponsorBranche;

    @Size(max = 50)
    private String rechtsform;

    @Size(max = 500)
    private String websiteUrl;

    private String beschreibung;

    // --- Getter/Setter ---

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getAnzeigename() { return anzeigename; }
    public void setAnzeigename(String anzeigename) { this.anzeigename = anzeigename; }

    public String getPasswort() { return passwort; }
    public void setPasswort(String passwort) { this.passwort = passwort; }

    public String getFirmenname() { return firmenname; }
    public void setFirmenname(String firmenname) { this.firmenname = firmenname; }

    public SponsorBranche getSponsorBranche() { return sponsorBranche; }
    public void setSponsorBranche(SponsorBranche sponsorBranche) { this.sponsorBranche = sponsorBranche; }

    public String getRechtsform() { return rechtsform; }
    public void setRechtsform(String rechtsform) { this.rechtsform = rechtsform; }

    public String getWebsiteUrl() { return websiteUrl; }
    public void setWebsiteUrl(String websiteUrl) { this.websiteUrl = websiteUrl; }

    public String getBeschreibung() { return beschreibung; }
    public void setBeschreibung(String beschreibung) { this.beschreibung = beschreibung; }
}

