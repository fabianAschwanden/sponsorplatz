package ch.sponsorplatz.organisation;

/**
 * Die 26 Schweizer Kantone (Code = Enum-Name, {@link #getAnzeige()} = deutscher
 * Name). Die Zuordnung PLZ → Kanton liefert {@link PlzVerzeichnis#kantonVon(String)}
 * (offizielle GeoNames-Daten).
 */
public enum Kanton {
    ZH("Zürich"), BE("Bern"), LU("Luzern"), UR("Uri"), SZ("Schwyz"),
    OW("Obwalden"), NW("Nidwalden"), GL("Glarus"), ZG("Zug"), FR("Freiburg"),
    SO("Solothurn"), BS("Basel-Stadt"), BL("Basel-Landschaft"), SH("Schaffhausen"),
    AR("Appenzell Ausserrhoden"), AI("Appenzell Innerrhoden"), SG("St. Gallen"),
    GR("Graubünden"), AG("Aargau"), TG("Thurgau"), TI("Tessin"), VD("Waadt"),
    VS("Wallis"), NE("Neuenburg"), GE("Genf"), JU("Jura");

    private final String anzeige;

    Kanton(String anzeige) {
        this.anzeige = anzeige;
    }

    public String getAnzeige() {
        return anzeige;
    }
}
