package ch.sponsorplatz.organisation;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

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

    /**
     * Verein-Branche (Health/Sport). Pflicht für VEREIN, NULL für UNTERNEHMEN —
     * die XOR-Validierung steht im {@code OrganisationService.wendeFormDatenAn},
     * nicht hier als {@code @NotNull}, weil das DTO beide Org-Typen abdeckt.
     */
    private Branche branche;

    /** Sponsor-Industrie. Pflicht für UNTERNEHMEN, NULL für VEREIN. */
    private SponsorBranche sponsorBranche;

    /**
     * Optionale Eltern-Org für hierarchische Konzern-Strukturen
     * (Konzern → Tochter → Abteilung). NULL = Root-Org.
     * Validierung im Service: nicht selbst, kein Cycle, max. Tiefe.
     */
    private UUID uebergeordneteOrgId;

    private String beschreibung;

    @Size(max = 500)
    private String websiteUrl;

    @Size(max = 34)
    @jakarta.validation.constraints.Pattern(
            regexp = "^$|^[A-Za-z]{2}[0-9]{2}[A-Za-z0-9 ]+$",
            message = "IBAN muss Format CH... haben (z.B. CH00 0000 0000 0000 0000 0)")
    private String iban;

    @Size(max = 70)
    private String strasse;

    @Size(max = 16)
    private String postleitzahl;

    @Size(max = 70)
    private String ort;

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

    public SponsorBranche getSponsorBranche() {
        return sponsorBranche;
    }

    public void setSponsorBranche(SponsorBranche sponsorBranche) {
        this.sponsorBranche = sponsorBranche;
    }

    public UUID getUebergeordneteOrgId() {
        return uebergeordneteOrgId;
    }

    public void setUebergeordneteOrgId(UUID uebergeordneteOrgId) {
        this.uebergeordneteOrgId = uebergeordneteOrgId;
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

    public String getIban() {
        return iban;
    }

    public void setIban(String iban) {
        this.iban = iban;
    }

    public String getStrasse() { return strasse; }
    public void setStrasse(String strasse) { this.strasse = strasse; }

    public String getPostleitzahl() { return postleitzahl; }
    public void setPostleitzahl(String postleitzahl) { this.postleitzahl = postleitzahl; }

    public String getOrt() { return ort; }
    public void setOrt(String ort) { this.ort = ort; }
}
