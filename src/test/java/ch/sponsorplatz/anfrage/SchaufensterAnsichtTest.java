package ch.sponsorplatz.anfrage;

import ch.sponsorplatz.organisation.Branche;
import ch.sponsorplatz.organisation.Kanton;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ENG-VIEW-01..06 — reine Filter-/Gruppierungs-/Kennzahl-Logik des
 * Schaufensters (ohne DB). Gruppiert wird nach Kanton (aus Verein-PLZ abgeleitet).
 */
class SchaufensterAnsichtTest {

    /** ENG-VIEW-01: Gruppierung nach Kanton, nach Anzeige sortiert, „Übrige Schweiz" (null) zuletzt. */
    @Test
    @DisplayName("ENG-VIEW-01: nach Kanton gruppiert + sortiert, unbekannter Kanton zuletzt")
    void gruppiertNachKanton() {
        var ansicht = baue(List.of(
                ev("FC Zürich", "fc-z", Branche.SPORT, Kanton.ZH),
                ev("FC Aarau", "fc-a", Branche.SPORT, Kanton.AG),
                ev("Reha X", "reha-x", Branche.REHA, null)), null, null);

        // Anzeige: "Aargau" < "Zürich"; null-Kanton ("") zuletzt
        assertThat(ansicht.nachKanton().keySet()).containsExactly("AG", "ZH", "");
        assertThat(ansicht.nachKanton().get("ZH")).hasSize(1);
    }

    /** ENG-VIEW-02: verfügbare Kantone distinct + nach Anzeige sortiert, ohne null. */
    @Test
    @DisplayName("ENG-VIEW-02: verfügbare Kantone distinct + sortiert, ohne null")
    void verfuegbareKantone() {
        var ansicht = baue(List.of(
                ev("A", "a", Branche.SPORT, Kanton.BE),
                ev("B", "b", Branche.SPORT, Kanton.AG),
                ev("C", "c", Branche.SPORT, Kanton.BE),
                ev("D", "d", Branche.SPORT, null)), null, null);

        assertThat(ansicht.verfuegbareKantone()).containsExactly(Kanton.AG, Kanton.BE);
    }

    /** ENG-VIEW-03: verfügbare Branchen distinct + nach Anzeige sortiert. */
    @Test
    @DisplayName("ENG-VIEW-03: verfügbare Branchen distinct + nach Anzeige sortiert")
    void verfuegbareBranchen() {
        var ansicht = baue(List.of(
                ev("A", "a", Branche.SPORT, Kanton.BE),
                ev("B", "b", Branche.REHA, Kanton.BE),
                ev("C", "c", Branche.SPORT, Kanton.BE)), null, null);

        // Anzeige: "Rehabilitation" < "Sport"
        assertThat(ansicht.verfuegbareBranchen()).containsExactly(Branche.REHA, Branche.SPORT);
    }

    /** ENG-VIEW-04: Kanton- und Branche-Filter wirken kombiniert; Optionen bleiben vollständig. */
    @Test
    @DisplayName("ENG-VIEW-04: Kanton + Branche kombiniert gefiltert")
    void kombinierterFilter() {
        var alle = List.of(
                ev("FC Bern Sport", "fc-bs", Branche.SPORT, Kanton.BE),
                ev("Reha Bern", "reha-b", Branche.REHA, Kanton.BE),
                ev("FC Aarau Sport", "fc-as", Branche.SPORT, Kanton.AG));

        var nurBernSport = baue(alle, "BE", Branche.SPORT);

        assertThat(nurBernSport.nachKanton().keySet()).containsExactly("BE");
        assertThat(nurBernSport.nachKanton().get("BE"))
                .extracting(EngagementView::vereinSlug).containsExactly("fc-bs");
        // Filter-Optionen bleiben vollständig (aus dem ungefilterten Set)
        assertThat(nurBernSport.verfuegbareKantone()).containsExactly(Kanton.AG, Kanton.BE);
        assertThat(nurBernSport.verfuegbareBranchen()).contains(Branche.SPORT, Branche.REHA);
    }

    /** ENG-VIEW-05: Kennzahlen — Vereine distinct, Kantone ohne null. */
    @Test
    @DisplayName("ENG-VIEW-05: anzahlVereine distinct, anzahlKantone ohne null")
    void kennzahlen() {
        var ansicht = baue(List.of(
                ev("FC Bern", "fc-b", Branche.SPORT, Kanton.BE),
                ev("FC Bern", "fc-b", Branche.SPORT, Kanton.BE),   // gleicher Verein, 2. Projekt
                ev("FC Aarau", "fc-a", Branche.SPORT, Kanton.AG),
                ev("Reha", "reha", Branche.REHA, null)), null, null);

        assertThat(ansicht.anzahlVereine()).isEqualTo(3); // fc-b, fc-a, reha
        assertThat(ansicht.anzahlKantone()).isEqualTo(2); // BE, AG (null zählt nicht)
    }

    /** ENG-VIEW-06: leeres Set → istLeer. */
    @Test
    @DisplayName("ENG-VIEW-06: keine Engagements → istLeer")
    void leer() {
        assertThat(baue(List.of(), null, null).istLeer()).isTrue();
    }

    private SchaufensterAnsicht baue(List<EngagementView> alle, String kantonCode, Branche branche) {
        return SchaufensterAnsicht.erstelle("CSS Versicherung", "css-versicherung",
                "/medien/logo-id", alle, kantonCode, branche);
    }

    private EngagementView ev(String verein, String slug, Branche branche, Kanton kanton) {
        return new EngagementView(UUID.randomUUID(), "CSS Versicherung", "css-versicherung",
                verein, slug, branche, null, "Projekt " + slug, "p-" + slug, "Gold", null, kanton, Instant.now());
    }
}
