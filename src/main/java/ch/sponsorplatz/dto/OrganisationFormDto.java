package ch.sponsorplatz.dto;

import ch.sponsorplatz.model.Branche;
import ch.sponsorplatz.model.OrgTyp;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Form-DTO für Anlage / Bearbeitung einer Organisation.
 *
 * Bewusst KEIN id-Feld — Update-Pfad identifiziert die Org via Slug aus URL,
 * niemals via Body-Parameter (Mass-Assignment-Defense, K3).
 */
public class OrganisationFormDto {

    @NotNull(message = "Typ ist Pflicht")
    private OrgTyp typ;

    @NotBlank(message = "Name ist Pflicht")
    @Size(min = 2, max = 255, message = "Name muss zwischen 2 und 255 Zeichen lang sein")
    private String name;

    @Size(max = 120)
    private String slug;

    @Size(max = 50)
    private String rechtsform;

    @NotNull(message = "Branche ist Pflicht — Sponsorplatz ist auf Sport und Gesundheit fokussiert")
    private Branche branche;

    private String beschreibung;

    @Size(max = 500)
    private String websiteUrl;

    public OrgTyp getTyp() {
        return typ;
    }

    public void setTyp(OrgTyp typ) {
        this.typ = typ;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSlug() {
        return slug;
    }

    public void setSlug(String slug) {
        this.slug = slug;
    }

    public String getRechtsform() {
        return rechtsform;
    }

    public void setRechtsform(String rechtsform) {
        this.rechtsform = rechtsform;
    }

    public Branche getBranche() {
        return branche;
    }

    public void setBranche(Branche branche) {
        this.branche = branche;
    }

    public String getBeschreibung() {
        return beschreibung;
    }

    public void setBeschreibung(String beschreibung) {
        this.beschreibung = beschreibung;
    }

    public String getWebsiteUrl() {
        return websiteUrl;
    }

    public void setWebsiteUrl(String websiteUrl) {
        this.websiteUrl = websiteUrl;
    }
}
