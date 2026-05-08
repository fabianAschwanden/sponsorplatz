package ch.sponsorplatz.organisation;

/**
 * Industrie einer Sponsor-Organisation (Typ {@link OrgTyp#UNTERNEHMEN}).
 *
 * <p>Bewusst getrennt von {@link Branche}: Vereine (Empfänger) sind strikt
 * Health/Sport-fokussiert, Sponsor-Firmen kommen aus jeder Industrie.
 * Eine Vermischung beider Achsen würde die Kuratierungs-Klarheit zerstören.
 *
 * <p>Wird konditional gerendert: im Verein-Form sieht man {@link Branche},
 * im Sponsor-Form {@link SponsorBranche}.
 */
public enum SponsorBranche {

    VERSICHERUNG("Versicherung"),
    BANK("Bank & Finanzen"),
    PHARMA("Pharma & Medizintechnik"),
    LEBENSMITTEL("Lebensmittel & Getränke"),
    SPORTARTIKEL("Sportartikel & Bekleidung"),
    MOBILITAET("Mobilität & Verkehr"),
    ENERGIE("Energie & Versorgung"),
    TELEKOM("Telekom & IT"),
    RETAIL("Handel & Detailhandel"),
    MEDIEN("Medien & Verlag"),
    IMMOBILIEN("Immobilien & Bau"),
    BERATUNG("Beratung & Dienstleistung"),
    ANDERE("Andere Industrie");

    private final String anzeige;

    SponsorBranche(String anzeige) {
        this.anzeige = anzeige;
    }

    public String getAnzeige() {
        return anzeige;
    }
}
