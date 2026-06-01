package ch.sponsorplatz.shared.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * LISTE-01..04 — generische Paginierung (Slice, Seitenzahl, Klemmung, Leer).
 */
class ListenSeiteTest {

    private List<Integer> n(int count) {
        return IntStream.rangeClosed(1, count).boxed().toList();
    }

    /** LISTE-01: Slice + Seitenzahl korrekt. */
    @Test
    @DisplayName("LISTE-01: Slice + Seitenzahl")
    void slice() {
        var s = ListenSeite.von(n(5), 2, 2, "x", false);
        assertThat(s.gesamt()).isEqualTo(5);
        assertThat(s.anzahlSeiten()).isEqualTo(3);
        assertThat(s.seite()).isEqualTo(2);
        assertThat(s.inhalt()).containsExactly(3, 4);
        assertThat(s.hatMehrereSeiten()).isTrue();
    }

    /** LISTE-02: zu hohe Seite wird auf die letzte geklemmt. */
    @Test
    @DisplayName("LISTE-02: Seite geklemmt")
    void klemmung() {
        var s = ListenSeite.von(n(5), 99, 2, null, false);
        assertThat(s.seite()).isEqualTo(3);
        assertThat(s.inhalt()).containsExactly(5);
    }

    /** LISTE-03: ungültige Grösse → Standard 25; eine Seite. */
    @Test
    @DisplayName("LISTE-03: ungültige Grösse → Standard")
    void standardGroesse() {
        var s = ListenSeite.von(n(10), 1, 0, null, false);
        assertThat(s.seitenGroesse()).isEqualTo(25);
        assertThat(s.anzahlSeiten()).isEqualTo(1);
        assertThat(s.inhalt()).hasSize(10);
    }

    /** LISTE-04: leere Liste → istLeer, eine Seite, leerer Slice. */
    @Test
    @DisplayName("LISTE-04: leere Liste")
    void leer() {
        var s = ListenSeite.von(List.<Integer>of(), 1, 25, null, false);
        assertThat(s.istLeer()).isTrue();
        assertThat(s.anzahlSeiten()).isEqualTo(1);
        assertThat(s.inhalt()).isEmpty();
        assertThat(s.hatMehrereSeiten()).isFalse();
    }
}
