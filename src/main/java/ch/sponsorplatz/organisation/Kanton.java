package ch.sponsorplatz.organisation;

import java.util.Optional;

/**
 * Die 26 Schweizer Kantone (Code = Enum-Name, {@link #getAnzeige()} = deutscher
 * Name). {@link #vonPlz(String)} leitet den Kanton aus einer vierstelligen PLZ ab.
 *
 * <p><b>Achtung — Heuristik:</b> Die Schweizer PLZ lassen sich nicht
 * verlustfrei in Kantons-Bereiche zerlegen (Grenz-PLZ, gemischte Leitregionen in
 * der Zentralschweiz). Diese Bereichs-Tabelle ist eine Best-Effort-Annäherung:
 * die Kantonshauptorte und klar abgegrenzte Kantone (z.B. GR = 7xxx) stimmen,
 * einzelne Grenz-PLZ können falsch oder als {@code empty} („Übrige Schweiz")
 * landen. Für 100% Genauigkeit den offiziellen BFS/Post-Datensatz vendoren und
 * {@link #vonPlz(String)} darauf umstellen — der Rest des Codes bleibt gleich.
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

    /**
     * Leitet den Kanton aus einer Schweizer PLZ ab. {@code Optional.empty()} bei
     * fehlender/ungültiger PLZ oder wenn der Bereich nicht eindeutig einem Kanton
     * zugeordnet ist („Übrige Schweiz").
     */
    public static Optional<Kanton> vonPlz(String plz) {
        if (plz == null) return Optional.empty();
        String trimmed = plz.trim();
        if (trimmed.length() != 4 || !trimmed.chars().allMatch(Character::isDigit)) {
            return Optional.empty();
        }
        return Optional.ofNullable(ausBereich(Integer.parseInt(trimmed)));
    }

    // Reihenfolge wichtig: spezifische Ausnahmen vor den breiten Bereichen.
    private static Kanton ausBereich(int p) {
        // --- Ausnahmen / kleine Kantone zuerst ---
        if (p == 6052) return NW;                     // Hergiswil NW (im OW-Block)
        if (p >= 6053 && p <= 6078) return OW;        // Sarnen, Alpnach, Kerns, Sachseln, Giswil, Lungern
        if (p == 6390) return OW;                     // Engelberg
        if (p >= 6370 && p <= 6389) return NW;        // Stans, Buochs, Beckenried, Dallenwil …
        if (p >= 6450 && p <= 6493) return UR;        // Altdorf, Erstfeld, Andermatt, Flüelen
        if (p >= 8750 && p <= 8784) return GL;        // Glarus, Näfels, Linthal
        if (p >= 9050 && p <= 9059) return AI;        // Appenzell
        if (p >= 9100 && p <= 9119) return AR;        // Herisau, Teufen, Gais
        if (p >= 8200 && p <= 8299) return SH;        // Schaffhausen
        if (p >= 2800 && p <= 2999) return JU;        // Delémont, Porrentruy
        if (p >= 1890 && p <= 1899) return VS;        // St-Maurice, Monthey-Umfeld
        if (p >= 1900 && p <= 1999) return VS;        // Sion, Sierre, Martigny, Brig-Umfeld unten
        if (p >= 3900 && p <= 3999) return VS;        // Brig, Visp, Zermatt (Oberwallis)

        // --- Breite Leitregionen (dominanter Kanton) ---
        if (p >= 1000 && p <= 1199) return VD;        // Lausanne, Renens, Morges
        if (p >= 1200 && p <= 1299) return GE;        // Genf (Nyon VD als Unschärfe akzeptiert)
        if (p >= 1300 && p <= 1599) return VD;        // Yverdon, Nyon-Umfeld, La Côte
        if (p >= 1600 && p <= 1799) return FR;        // Bulle, Fribourg (Payerne VD als Unschärfe)
        if (p >= 1800 && p <= 1889) return VD;        // Vevey, Montreux, Aigle
        if (p >= 2000 && p <= 2499) return NE;        // Neuchâtel, La Chaux-de-Fonds, Le Locle
        if (p >= 2500 && p <= 2799) return BE;        // Biel/Bienne, Berner Jura
        if (p >= 3000 && p <= 3899) return BE;        // Bern, Thun, Interlaken, Emmental (Murten FR unscharf)
        if (p >= 4000 && p <= 4099) return BS;        // Basel-Stadt
        if (p >= 4300 && p <= 4399) return AG;        // Rheinfelden, Frick (Fricktal AG)
        if (p >= 4100 && p <= 4499) return BL;        // Allschwil, Liestal, Laufen
        if (p >= 4500 && p <= 4799) return SO;        // Solothurn, Olten, Balsthal
        if (p >= 4800 && p <= 4899) return AG;        // Zofingen
        if (p >= 4900 && p <= 4999) return BE;        // Langenthal
        if (p >= 5000 && p <= 5999) return AG;        // Aarau, Baden, Lenzburg, Wohlen
        if (p >= 6000 && p <= 6299) return LU;        // Luzern, Sursee, Hochdorf
        if (p >= 6300 && p <= 6349) return ZG;        // Zug, Cham, Baar
        if (p >= 6350 && p <= 6449) return SZ;        // Küssnacht, Schwyz, Einsiedeln, Brunnen
        if (p >= 6500 && p <= 6999) return TI;        // Bellinzona, Locarno, Lugano
        if (p >= 7000 && p <= 7999) return GR;        // ganzer Kanton Graubünden
        if (p >= 8000 && p <= 8199) return ZH;        // Stadt Zürich, Unterland
        if (p >= 8300 && p <= 8499) return ZH;        // Kloten, Winterthur
        if (p >= 8500 && p <= 8599) return TG;        // Frauenfeld, Weinfelden (Thurgau)
        if (p >= 8600 && p <= 8999) return ZH;        // Uster, Zürichsee (Rapperswil SG / Höfe SZ unscharf)
        if (p >= 9000 && p <= 9049) return SG;        // Stadt St. Gallen
        if (p >= 9200 && p <= 9999) return SG;        // Gossau, Wil, Rorschach, Toggenburg
        return null;                                  // Übrige Schweiz
    }
}
