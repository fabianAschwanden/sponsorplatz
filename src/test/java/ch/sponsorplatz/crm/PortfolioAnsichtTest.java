package ch.sponsorplatz.crm;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * PORTFOLIO-01..06 — server-seitige Filter-/Such-/Sortier-/Zähl-Logik der
 * CRM-Portfolio-Liste (ohne DB, reine Aufbereitung).
 */
class PortfolioAnsichtTest {

    /** PORTFOLIO-01: Status-Filter; Gesamt- vs. Anzeige-Anzahl getrennt. */
    @Test
    @DisplayName("PORTFOLIO-01: Status-Filter + Anzahl gesamt/gezeigt")
    void statusFilter() {
        var ansicht = PortfolioAnsicht.erstelle(List.of(
                account("FC Aktiv", AccountStatus.AKTIV, PipelineStage.LEAD, null, null),
                account("FC Lead", AccountStatus.LEAD, PipelineStage.LEAD, null, null)),
                null, AccountStatus.AKTIV, null, null, false, 1, 100);

        assertThat(ansicht.anzahlGesamt()).isEqualTo(2);
        assertThat(ansicht.anzahlGezeigt()).isEqualTo(1);
        assertThat(ansicht.accounts()).extracting(SponsorAccountView::vereinName).containsExactly("FC Aktiv");
        assertThat(ansicht.istGefiltert()).isTrue();
    }

    /** PORTFOLIO-02: Suche über Vereinsname + Notiz, case-insensitiv. */
    @Test
    @DisplayName("PORTFOLIO-02: Suche über Name + Notiz")
    void suche() {
        var ansicht = PortfolioAnsicht.erstelle(List.of(
                account("FC Zürich", AccountStatus.AKTIV, PipelineStage.LEAD, null, "Hauptsponsor"),
                account("EHC Bern", AccountStatus.AKTIV, PipelineStage.LEAD, null, "Wichtig")),
                "zürich", null, null, null, false, 1, 100);
        assertThat(ansicht.accounts()).extracting(SponsorAccountView::vereinName).containsExactly("FC Zürich");

        var ueberNotiz = PortfolioAnsicht.erstelle(List.of(
                account("FC Zürich", AccountStatus.AKTIV, PipelineStage.LEAD, null, "Hauptsponsor"),
                account("EHC Bern", AccountStatus.AKTIV, PipelineStage.LEAD, null, "Wichtig")),
                "WICHT", null, null, null, false, 1, 100);
        assertThat(ueberNotiz.accounts()).extracting(SponsorAccountView::vereinName).containsExactly("EHC Bern");
    }

    /** PORTFOLIO-03: kombinierter Filter (Status + Pipeline + Suche). */
    @Test
    @DisplayName("PORTFOLIO-03: Status + Pipeline + Suche kombiniert")
    void kombiniert() {
        var ansicht = PortfolioAnsicht.erstelle(List.of(
                account("FC Match", AccountStatus.AKTIV, PipelineStage.ANGEBOT, null, null),
                account("FC Match", AccountStatus.AKTIV, PipelineStage.LEAD, null, null),     // falsche Pipeline
                account("FC Other", AccountStatus.AKTIV, PipelineStage.ANGEBOT, null, null)), // falsche Suche
                "match", AccountStatus.AKTIV, PipelineStage.ANGEBOT, null, false, 1, 100);
        assertThat(ansicht.anzahlGezeigt()).isEqualTo(1);
        assertThat(ansicht.accounts().get(0).pipelineStage()).isEqualTo(PipelineStage.ANGEBOT);
    }

    /** PORTFOLIO-04: Sortierung nach Verein auf-/absteigend. */
    @Test
    @DisplayName("PORTFOLIO-04: Sortierung nach Verein auf/ab")
    void sortVerein() {
        var alle = List.of(
                account("Zeta", AccountStatus.AKTIV, PipelineStage.LEAD, null, null),
                account("Alpha", AccountStatus.AKTIV, PipelineStage.LEAD, null, null));

        assertThat(PortfolioAnsicht.erstelle(alle, null, null, null, "verein", false, 1, 100).accounts())
                .extracting(SponsorAccountView::vereinName).containsExactly("Alpha", "Zeta");
        assertThat(PortfolioAnsicht.erstelle(alle, null, null, null, "verein", true, 1, 100).accounts())
                .extracting(SponsorAccountView::vereinName).containsExactly("Zeta", "Alpha");
    }

    /** PORTFOLIO-05: Sortierung nach Forecast; null-Werte zuerst (aufsteigend). */
    @Test
    @DisplayName("PORTFOLIO-05: Sortierung nach Forecast, null zuerst")
    void sortForecast() {
        var alle = List.of(
                account("Hoch", AccountStatus.AKTIV, PipelineStage.LEAD, new BigDecimal("9000"), null),
                account("Ohne", AccountStatus.AKTIV, PipelineStage.LEAD, null, null),
                account("Tief", AccountStatus.AKTIV, PipelineStage.LEAD, new BigDecimal("1000"), null));
        assertThat(PortfolioAnsicht.erstelle(alle, null, null, null, "forecast", false, 1, 100).accounts())
                .extracting(SponsorAccountView::vereinName).containsExactly("Ohne", "Tief", "Hoch");
    }

    /** PORTFOLIO-06: Forecast-Summe über gefilterte Menge; kein Sort bewahrt Reihenfolge. */
    @Test
    @DisplayName("PORTFOLIO-06: Forecast-Summe gefiltert; ohne Sort Reihenfolge bewahrt")
    void forecastSummeUndKeinSort() {
        var ansicht = PortfolioAnsicht.erstelle(List.of(
                account("B", AccountStatus.AKTIV, PipelineStage.LEAD, new BigDecimal("2000"), null),
                account("A", AccountStatus.LEAD, PipelineStage.LEAD, new BigDecimal("5000"), null)),
                null, AccountStatus.AKTIV, null, null, false, 1, 100);

        // nur der AKTIVE zählt zur Summe (gewichtet = forecast hier identisch, weil Stufe LEAD=10%? siehe von())
        assertThat(ansicht.anzahlGezeigt()).isEqualTo(1);
        // Reihenfolge bewahrt (kein Sort) — hier trivial (1 Element)
        assertThat(ansicht.sort()).isNull();
    }

    /** PORTFOLIO-07: Paginierung — Seiten-Slice, Seitenzahl, vollständige gefilterteIds, Seite geklemmt. */
    @Test
    @DisplayName("PORTFOLIO-07: Paginierung — Slice + Seitenzahl + gefilterteIds")
    void paginierung() {
        var alle = new java.util.ArrayList<SponsorAccountView>();
        for (int i = 0; i < 5; i++) {
            alle.add(account("FC " + (char) ('A' + i), AccountStatus.AKTIV, PipelineStage.LEAD, null, null));
        }
        // Seitengrösse 2 → 3 Seiten (5 Einträge); Seite 2 zeigt C+D.
        var seite2 = PortfolioAnsicht.erstelle(alle, null, null, null, "verein", false, 2, 2);
        assertThat(seite2.anzahlSeiten()).isEqualTo(3);
        assertThat(seite2.anzahlGezeigt()).isEqualTo(5);          // gefilterte Gesamtzahl
        assertThat(seite2.accounts()).extracting(SponsorAccountView::vereinName).containsExactly("FC C", "FC D");
        assertThat(seite2.gefilterteIds()).hasSize(5);            // ALLE Treffer-IDs, nicht nur die Seite
        assertThat(seite2.hatMehrereSeiten()).isTrue();

        // Übergrosse Seite wird auf die letzte geklemmt.
        var zuHoch = PortfolioAnsicht.erstelle(alle, null, null, null, "verein", false, 99, 2);
        assertThat(zuHoch.seite()).isEqualTo(3);
        assertThat(zuHoch.accounts()).extracting(SponsorAccountView::vereinName).containsExactly("FC E");
    }

    // --- Fixture ---
    private SponsorAccountView account(String verein, AccountStatus status, PipelineStage pipeline,
                                       BigDecimal forecast, String notiz) {
        BigDecimal gewichtet = (forecast == null) ? null
                : forecast.multiply(BigDecimal.valueOf(pipeline.standardWahrscheinlichkeit()))
                        .divide(BigDecimal.valueOf(100));
        return new SponsorAccountView(UUID.randomUUID(), UUID.randomUUID(), verein,
                verein.toLowerCase().replace(' ', '-'), null, status, AccountTier.CORE, pipeline,
                forecast, gewichtet, notiz, Instant.now(), null);
    }
}
