package ch.sponsorplatz.organisation;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * KANTON-01..03 — PLZ→Kanton-Heuristik. Geprüft werden die Kantonshauptorte
 * und klar abgegrenzte Kantone (verlässlich); Grenz-PLZ sind bewusst nicht Teil
 * der Garantie (siehe {@link Kanton}-Doc).
 */
class KantonTest {

    /** KANTON-01: Hauptorte / eindeutige PLZ → korrekter Kanton. */
    @ParameterizedTest
    @CsvSource({
            "8001, ZH", "3011, BE", "6003, LU", "6460, UR", "6430, SZ",
            "6060, OW", "6370, NW", "8750, GL", "6300, ZG", "1700, FR",
            "4500, SO", "4001, BS", "4410, BL", "8200, SH", "9100, AR",
            "9050, AI", "9000, SG", "7000, GR", "5000, AG", "8500, TG",
            "6900, TI", "1003, VD", "1204, GE", "1950, VS", "2000, NE", "2800, JU"
    })
    @DisplayName("KANTON-01: Hauptorte werden korrekt zugeordnet")
    void hauptorte(String plz, Kanton erwartet) {
        assertThat(Kanton.vonPlz(plz)).contains(erwartet);
    }

    /** KANTON-02: ganzes Graubünden (7xxx) → GR. */
    @ParameterizedTest
    @ValueSource(strings = {"7000", "7270", "7500", "7999"})
    @DisplayName("KANTON-02: 7xxx → Graubünden")
    void graubuenden(String plz) {
        assertThat(Kanton.vonPlz(plz)).contains(Kanton.GR);
    }

    /** KANTON-03: ungültige/leere PLZ → empty. */
    @Test
    @DisplayName("KANTON-03: ungültige PLZ → empty")
    void ungueltig() {
        assertThat(Kanton.vonPlz(null)).isEmpty();
        assertThat(Kanton.vonPlz("")).isEmpty();
        assertThat(Kanton.vonPlz("abc")).isEmpty();
        assertThat(Kanton.vonPlz("123")).isEmpty();
        assertThat(Kanton.vonPlz("80000")).isEmpty();
    }
}
