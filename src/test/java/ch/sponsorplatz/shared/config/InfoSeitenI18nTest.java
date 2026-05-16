package ch.sponsorplatz.shared.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.MessageSource;
import org.springframework.test.context.ActiveProfiles;

import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Regressions-Test gegen Encoding-Fehler in den Info-/Anfrage-Seiten-Strings
 * (AGB, Datenschutz, Kontakt). Schweizerdeutsche Umlaute und Akzente müssen
 * sauber durchkommen — nicht als „�" oder „a", weil eine .properties-Datei in
 * Latin-1 statt UTF-8/Java-Escapes geschrieben wurde.
 *
 * Test-IDs: I18N-INFO-01..04.
 */
@SpringBootTest
@ActiveProfiles("dev")
class InfoSeitenI18nTest {

    @Autowired
    MessageSource messageSource;

    /** I18N-INFO-01: AGB-Geltungsbereich (DE) enthält das ä in „Geschäftsbedingungen". */
    @Test
    @DisplayName("I18N-INFO-01: AGB-Geltungsbereich DE — Umlaute korrekt")
    void agbGeltungsbereichDeEnthaeltUmlaute() {
        String text = messageSource.getMessage(
                "agb.geltungsbereich.text", null, Locale.forLanguageTag("de-CH"));
        assertThat(text)
                .contains("Geschäftsbedingungen")
                .contains("«Sponsorplatz»")
                .doesNotContain("�"); // U+FFFD = REPLACEMENT CHARACTER
    }

    /** I18N-INFO-02: AGB-Gebühren (DE) — ü-Umlaut. */
    @Test
    @DisplayName("I18N-INFO-02: AGB-Gebühren DE — ü korrekt")
    void agbGebuehrenDe() {
        String text = messageSource.getMessage(
                "agb.gebuehren.titel", null, Locale.forLanguageTag("de-CH"));
        assertThat(text).contains("Gebühren");
    }

    /** I18N-INFO-03: Kontakt-Intro (DE) — ä-Umlaut. */
    @Test
    @DisplayName("I18N-INFO-03: Kontakt-Intro DE — ä korrekt")
    void kontaktIntroDe() {
        String text = messageSource.getMessage(
                "kontakt.intro", null, Locale.forLanguageTag("de-CH"));
        assertThat(text).contains("Erzähle");
    }

    /** I18N-INFO-04: AGB-Recht (FR) — Akzent é. */
    @Test
    @DisplayName("I18N-INFO-04: AGB-Recht FR — é korrekt")
    void agbRechtFrEnthaeltAkzent() {
        String text = messageSource.getMessage(
                "agb.recht.text", null, Locale.forLanguageTag("fr-CH"));
        // Französische Übersetzung enthält Akzente; primär Sanity, dass kein
        // Mojibake-� drinsteckt.
        assertThat(text).doesNotContain("�");
    }
}
