package ch.sponsorplatz.home;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests für {@link HilfeThema}.
 * Test-IDs: HILFE-06..07 in {@code specs/TESTSTRATEGIE.md}.
 */
class HilfeThemaTest {

    /** HILFE-06: Slug-Lookup ist case-insensitive und löst auf das richtige Thema auf. */
    @Test
    void nachSlugFindetThema() {
        assertThat(HilfeThema.nachSlug("dashboard")).contains(HilfeThema.DASHBOARD);
        assertThat(HilfeThema.nachSlug("DASHBOARD")).contains(HilfeThema.DASHBOARD);
        // Slug „2fa" ≠ i18n-Key „zweifa"
        assertThat(HilfeThema.nachSlug("2fa")).contains(HilfeThema.ZWEIFA);
        assertThat(HilfeThema.ZWEIFA.i18nSchluessel()).isEqualTo("zweifa");
    }

    /** HILFE-07: Unbekannter/null-Slug → leeres Optional, jedes Thema hat >0 Schritte. */
    @Test
    void unbekannterSlugLeerUndSchritteVorhanden() {
        assertThat(HilfeThema.nachSlug("gibtsnicht")).isEmpty();
        assertThat(HilfeThema.nachSlug(null)).isEmpty();
        for (HilfeThema t : HilfeThema.values()) {
            assertThat(t.anzahlSchritte()).as("Thema %s muss Schritte haben", t).isGreaterThan(0);
            assertThat(t.slug()).isNotBlank();
            assertThat(t.i18nSchluessel()).isNotBlank();
        }
    }
}
