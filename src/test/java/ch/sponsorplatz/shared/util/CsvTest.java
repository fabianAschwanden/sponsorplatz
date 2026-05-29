package ch.sponsorplatz.shared.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * CSV-01..03 — CSV-Helfer: Quoting, Parsing, Round-Trip.
 */
class CsvTest {

    /** CSV-01: einfache Felder ohne Sonderzeichen. */
    @Test
    @DisplayName("CSV-01: einfache Zeile schreiben + parsen")
    void einfach() {
        String zeile = Csv.zeile(List.of("fc-zuerich", "FC Zürich", "AKTIV"));
        assertThat(zeile).isEqualTo("fc-zuerich;FC Zürich;AKTIV");
        assertThat(Csv.parse(zeile)).containsExactly("fc-zuerich", "FC Zürich", "AKTIV");
    }

    /** CSV-02: Felder mit Delimiter/Quote werden gequotet + korrekt zurückgelesen. */
    @Test
    @DisplayName("CSV-02: Sonderzeichen werden gequotet (Round-Trip)")
    void sonderzeichen() {
        List<String> felder = List.of("a;b", "sagt \"hallo\"", "normal");
        String zeile = Csv.zeile(felder);
        assertThat(zeile).isEqualTo("\"a;b\";\"sagt \"\"hallo\"\"\";normal");
        assertThat(Csv.parse(zeile)).containsExactlyElementsOf(felder);
    }

    /** CSV-03: Zeilenumbrüche in Feldern werden zu Leerzeichen; leere Felder bleiben leer. */
    @Test
    @DisplayName("CSV-03: Newlines → Space, leere Felder")
    void newlinesUndLeer() {
        String zeile = Csv.zeile(List.of("zeile1\nzeile2", "", "x"));
        assertThat(zeile).isEqualTo("zeile1 zeile2;;x");
        assertThat(Csv.parse(zeile)).containsExactly("zeile1 zeile2", "", "x");
    }
}
