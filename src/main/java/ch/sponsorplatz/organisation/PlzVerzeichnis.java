package ch.sponsorplatz.organisation;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Offizielles Schweizer PLZ-Verzeichnis (Quelle: GeoNames, CC BY 4.0) — geladen
 * aus {@code /plz/plz_ort_kanton.csv} (eine Zeile je PLZ). Liefert den Kanton und
 * den Ort zu einer PLZ. Ersetzt die frühere PLZ-Bereichs-Heuristik durch exakte
 * Daten.
 *
 * <p>Statisch + einmalig beim Klassen-Load eingelesen (immutable Map) — kein
 * Spring-Bean nötig, damit auch statische Mapper wie {@code EngagementView.von}
 * darauf zugreifen können.
 */
public final class PlzVerzeichnis {

    private record Eintrag(String ort, Kanton kanton) {}

    private static final Map<String, Eintrag> NACH_PLZ = lade();

    private PlzVerzeichnis() {
    }

    /** Kanton zur PLZ, oder {@code empty} bei unbekannter/ungültiger PLZ. */
    public static Optional<Kanton> kantonVon(String plz) {
        return eintrag(plz).map(Eintrag::kanton);
    }

    /** Ort zur PLZ (für die Adress-Auswahlhilfe), oder {@code empty}. */
    public static Optional<String> ortVon(String plz) {
        return eintrag(plz).map(Eintrag::ort);
    }

    /**
     * Kanton zu einem Ortsnamen (Fallback, wenn keine PLZ vorhanden ist).
     * Nur <b>eindeutige</b> Orte werden aufgelöst — kommt ein Ortsname in mehreren
     * Kantonen vor (z.B. „Buchs"), gibt es {@code empty}, um Falschzuordnung zu vermeiden.
     */
    public static Optional<Kanton> kantonVonOrt(String ort) {
        if (ort == null || ort.isBlank()) return Optional.empty();
        return Optional.ofNullable(NACH_ORT.get(normalisiere(ort)));
    }

    private static Optional<Eintrag> eintrag(String plz) {
        if (plz == null) return Optional.empty();
        return Optional.ofNullable(NACH_PLZ.get(plz.trim()));
    }

    private static String normalisiere(String ort) {
        return ort.trim().toLowerCase();
    }

    private static final Map<String, Kanton> NACH_ORT = ladeOrte();

    private static Map<String, Eintrag> lade() {
        Map<String, Eintrag> map = new HashMap<>();
        leseZeilen((plz, ort, kanton) -> map.put(plz, new Eintrag(ort, kanton)));
        return Map.copyOf(map);
    }

    /** Ort → Kanton, aber nur eindeutige Orte (mehrdeutige werden entfernt). */
    private static Map<String, Kanton> ladeOrte() {
        Map<String, Kanton> map = new HashMap<>();
        java.util.Set<String> mehrdeutig = new java.util.HashSet<>();
        leseZeilen((plz, ort, kanton) -> {
            String key = normalisiere(ort);
            Kanton vorhanden = map.putIfAbsent(key, kanton);
            if (vorhanden != null && vorhanden != kanton) {
                mehrdeutig.add(key);
            }
        });
        mehrdeutig.forEach(map::remove);
        return Map.copyOf(map);
    }

    private interface ZeilenLeser {
        void zeile(String plz, String ort, Kanton kanton);
    }

    private static void leseZeilen(ZeilenLeser leser) {
        try (InputStream in = PlzVerzeichnis.class.getResourceAsStream("/plz/plz_ort_kanton.csv");
             BufferedReader br = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
            String zeile;
            while ((zeile = br.readLine()) != null) {
                if (zeile.isBlank() || zeile.startsWith("#")) continue;
                String[] teile = zeile.split(";", 3);
                if (teile.length < 3) continue;
                try {
                    leser.zeile(teile[0].trim(), teile[1].trim(), Kanton.valueOf(teile[2].trim()));
                } catch (IllegalArgumentException ignoriert) {
                    // unbekannter Kanton-Code → Zeile überspringen
                }
            }
        } catch (Exception e) {
            throw new IllegalStateException("PLZ-Verzeichnis konnte nicht geladen werden", e);
        }
    }
}
