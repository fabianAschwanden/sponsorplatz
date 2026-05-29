package ch.sponsorplatz.anfrage;

import ch.sponsorplatz.organisation.Branche;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ENG-VIEW-01..06 — reine Filter-/Gruppierungs-/Kennzahl-Logik des
 * Schaufensters (ohne DB). Region = Ort des Projekts.
 */
class SchaufensterAnsichtTest {

    /** ENG-VIEW-01: Gruppierung nach Region, alphabetisch, „ohne Region" zuletzt. */
    @Test
    @DisplayName("ENG-VIEW-01: nach Region gruppiert + sortiert, leere Region zuletzt")
    void gruppiertNachRegion() {
        var ansicht = baue(List.of(
                ev("FC Zürich", "fc-z", Branche.SPORT, "Zürich"),
                ev("FC Aarau", "fc-a", Branche.SPORT, "Aarau"),
                ev("Reha X", "reha-x", Branche.REHA, null)), null, null);

        assertThat(ansicht.nachRegion().keySet()).containsExactly("Aarau", "Zürich", "");
        assertThat(ansicht.nachRegion().get("Zürich")).hasSize(1);
    }

    /** ENG-VIEW-02: verfügbare Regionen distinct + sortiert, ohne Leerwerte. */
    @Test
    @DisplayName("ENG-VIEW-02: verfügbare Regionen distinct + sortiert, ohne Leere")
    void verfuegbareRegionen() {
        var ansicht = baue(List.of(
                ev("A", "a", Branche.SPORT, "Bern"),
                ev("B", "b", Branche.SPORT, "Aarau"),
                ev("C", "c", Branche.SPORT, "Bern"),
                ev("D", "d", Branche.SPORT, null)), null, null);

        assertThat(ansicht.verfuegbareRegionen()).containsExactly("Aarau", "Bern");
    }

    /** ENG-VIEW-03: verfügbare Branchen distinct + nach Anzeige sortiert. */
    @Test
    @DisplayName("ENG-VIEW-03: verfügbare Branchen distinct + nach Anzeige sortiert")
    void verfuegbareBranchen() {
        var ansicht = baue(List.of(
                ev("A", "a", Branche.SPORT, "Bern"),
                ev("B", "b", Branche.REHA, "Bern"),
                ev("C", "c", Branche.SPORT, "Bern")), null, null);

        // Anzeige: "Rehabilitation" < "Sport"
        assertThat(ansicht.verfuegbareBranchen()).containsExactly(Branche.REHA, Branche.SPORT);
    }

    /** ENG-VIEW-04: Region- und Branche-Filter wirken kombiniert. */
    @Test
    @DisplayName("ENG-VIEW-04: Region + Branche kombiniert gefiltert")
    void kombinierterFilter() {
        var alle = List.of(
                ev("FC Bern Sport", "fc-bs", Branche.SPORT, "Bern"),
                ev("Reha Bern", "reha-b", Branche.REHA, "Bern"),
                ev("FC Aarau Sport", "fc-as", Branche.SPORT, "Aarau"));

        var nurBernSport = baue(alle, "Bern", Branche.SPORT);

        assertThat(nurBernSport.nachRegion().keySet()).containsExactly("Bern");
        assertThat(nurBernSport.nachRegion().get("Bern"))
                .extracting(EngagementView::vereinSlug).containsExactly("fc-bs");
        // Filter-Optionen bleiben vollständig (aus dem ungefilterten Set)
        assertThat(nurBernSport.verfuegbareRegionen()).containsExactly("Aarau", "Bern");
        assertThat(nurBernSport.verfuegbareBranchen()).contains(Branche.SPORT, Branche.REHA);
    }

    /** ENG-VIEW-05: Kennzahlen — Vereine distinct, Regionen ohne Leerwert. */
    @Test
    @DisplayName("ENG-VIEW-05: anzahlVereine distinct, anzahlRegionen ohne Leere")
    void kennzahlen() {
        var ansicht = baue(List.of(
                ev("FC Bern", "fc-b", Branche.SPORT, "Bern"),
                ev("FC Bern", "fc-b", Branche.SPORT, "Bern"),   // gleicher Verein, 2. Projekt
                ev("FC Aarau", "fc-a", Branche.SPORT, "Aarau"),
                ev("Reha", "reha", Branche.REHA, null)), null, null);

        assertThat(ansicht.anzahlVereine()).isEqualTo(3); // fc-b, fc-a, reha
        assertThat(ansicht.anzahlRegionen()).isEqualTo(2); // Bern, Aarau (leer zählt nicht)
    }

    /** ENG-VIEW-06: leeres Set → istLeer. */
    @Test
    @DisplayName("ENG-VIEW-06: keine Engagements → istLeer")
    void leer() {
        assertThat(baue(List.of(), null, null).istLeer()).isTrue();
    }

    private SchaufensterAnsicht baue(List<EngagementView> alle, String region, Branche branche) {
        return SchaufensterAnsicht.erstelle("CSS Versicherung", "css-versicherung",
                "/medien/logo-id", alle, region, branche);
    }

    private EngagementView ev(String verein, String slug, Branche branche, String region) {
        return new EngagementView(UUID.randomUUID(), "CSS Versicherung", "css-versicherung",
                verein, slug, branche, null, "Projekt " + slug, "p-" + slug, "Gold", region, Instant.now());
    }
}
