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

    private static Optional<Eintrag> eintrag(String plz) {
        if (plz == null) return Optional.empty();
        return Optional.ofNullable(NACH_PLZ.get(plz.trim()));
    }

    private static Map<String, Eintrag> lade() {
        Map<String, Eintrag> map = new HashMap<>();
        try (InputStream in = PlzVerzeichnis.class.getResourceAsStream("/plz/plz_ort_kanton.csv");
             BufferedReader br = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
            String zeile;
            while ((zeile = br.readLine()) != null) {
                if (zeile.isBlank() || zeile.startsWith("#")) continue;
                String[] teile = zeile.split(";", 3);
                if (teile.length < 3) continue;
                try {
                    map.put(teile[0].trim(), new Eintrag(teile[1].trim(), Kanton.valueOf(teile[2].trim())));
                } catch (IllegalArgumentException ignoriert) {
                    // unbekannter Kanton-Code → Zeile überspringen
                }
            }
        } catch (Exception e) {
            throw new IllegalStateException("PLZ-Verzeichnis konnte nicht geladen werden", e);
        }
        return Map.copyOf(map);
    }
}
