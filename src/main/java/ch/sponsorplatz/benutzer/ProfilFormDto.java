package ch.sponsorplatz.benutzer;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Form-DTO für Profil-Bearbeitung.
 */
public class ProfilFormDto {

    @NotBlank(message = "Anzeigename ist Pflicht")
    @Size(min = 2, max = 100)
    private String anzeigename;

    @Size(max = 5)
    private String sprache;

    @Size(max = 30)
    private String telefon;

    @Size(max = 2000)
    private String bio;

    @Size(max = 100)
    private String ort;

    @Size(max = 500)
    private String websiteUrl;

    @Size(max = 150)
    private String positionTitel;

    // --- Getter / Setter ---

    public String getAnzeigename() { return anzeigename; }
    public void setAnzeigename(String anzeigename) { this.anzeigename = anzeigename; }

    public String getSprache() { return sprache; }
    public void setSprache(String sprache) { this.sprache = sprache; }

    public String getTelefon() { return telefon; }
    public void setTelefon(String telefon) { this.telefon = telefon; }

    public String getBio() { return bio; }
    public void setBio(String bio) { this.bio = bio; }

    public String getOrt() { return ort; }
    public void setOrt(String ort) { this.ort = ort; }

    public String getWebsiteUrl() { return websiteUrl; }
    public void setWebsiteUrl(String websiteUrl) { this.websiteUrl = websiteUrl; }

    public String getPositionTitel() { return positionTitel; }
    public void setPositionTitel(String positionTitel) { this.positionTitel = positionTitel; }
}

