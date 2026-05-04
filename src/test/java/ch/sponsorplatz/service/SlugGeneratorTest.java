package ch.sponsorplatz.service;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SlugGeneratorTest {

    private final SlugGenerator generator = new SlugGenerator();

    /** ORG-01: Umlaute werden korrekt zu ASCII konvertiert. */
    @Test
    void umlauteWerdenZuAscii() {
        assertThat(generator.fromName("FC Beispiel Zürich")).isEqualTo("fc-beispiel-zuerich");
        assertThat(generator.fromName("Fußball Verein")).isEqualTo("fussball-verein");
        assertThat(generator.fromName("Mädchen-Förderung Österreich")).isEqualTo("maedchen-foerderung-oesterreich");
    }

    /** ORG-02: Sonderzeichen werden entfernt, mehrfach-Bindestriche reduziert. */
    @Test
    void sonderzeichenWerdenEntferntUndBindestricheReduziert() {
        assertThat(generator.fromName("Verein für Sport & Kultur"))
            .isEqualTo("verein-fuer-sport-kultur");
        assertThat(generator.fromName("Kleiner   Klub!!!"))
            .isEqualTo("kleiner-klub");
        assertThat(generator.fromName("--Vorne--Hinten--"))
            .isEqualTo("vorne-hinten");
    }

    @Test
    void akzenteWerdenEntfernt() {
        assertThat(generator.fromName("Café Léo")).isEqualTo("cafe-leo");
        assertThat(generator.fromName("À l'envers")).isEqualTo("a-lenvers");
    }

    @Test
    void zahlenBleibenErhalten() {
        assertThat(generator.fromName("FC 1879")).isEqualTo("fc-1879");
        assertThat(generator.fromName("Verein 24/7")).isEqualTo("verein-247");
    }

    @Test
    void leererNameWirftException() {
        assertThatThrownBy(() -> generator.fromName(""))
            .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> generator.fromName("   "))
            .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> generator.fromName(null))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void nurSonderzeichenWirftException() {
        assertThatThrownBy(() -> generator.fromName("!!!@@@"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Slug");
    }
}
