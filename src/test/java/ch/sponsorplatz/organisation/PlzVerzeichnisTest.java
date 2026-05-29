package ch.sponsorplatz.organisation;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * PLZ-01..03 — offizielles PLZ-Verzeichnis (GeoNames). Exakte PLZ→Kanton-/Ort-
 * Zuordnung statt Heuristik.
 */
class PlzVerzeichnisTest {

    /** PLZ-01: PLZ → korrekter Kanton (Querschnitt über die Sprachregionen). */
    @ParameterizedTest
    @CsvSource({
            "8001, ZH", "3011, BE", "1204, GE", "1003, VD", "6900, TI",
            "7000, GR", "9000, SG", "4001, BS", "5000, AG", "6300, ZG",
            "2000, NE", "1950, VS", "6460, UR", "9050, AI"
    })
    @DisplayName("PLZ-01: PLZ → korrekter Kanton")
    void plzZuKanton(String plz, Kanton erwartet) {
        assertThat(PlzVerzeichnis.kantonVon(plz)).contains(erwartet);
    }

    /** PLZ-02: PLZ → Ort (für die Adress-Auswahlhilfe). */
    @Test
    @DisplayName("PLZ-02: PLZ → Ort")
    void plzZuOrt() {
        assertThat(PlzVerzeichnis.ortVon("8001")).contains("Zürich");
        assertThat(PlzVerzeichnis.ortVon("3011")).contains("Bern");
        assertThat(PlzVerzeichnis.ortVon("7000")).contains("Chur");
    }

    /** PLZ-03: unbekannte/ungültige PLZ → empty. */
    @Test
    @DisplayName("PLZ-03: unbekannte/ungültige PLZ → empty")
    void unbekannt() {
        assertThat(PlzVerzeichnis.kantonVon(null)).isEmpty();
        assertThat(PlzVerzeichnis.kantonVon("")).isEmpty();
        assertThat(PlzVerzeichnis.kantonVon("0000")).isEmpty();
        assertThat(PlzVerzeichnis.kantonVon("abcd")).isEmpty();
        assertThat(PlzVerzeichnis.ortVon("0000")).isEmpty();
    }
}
