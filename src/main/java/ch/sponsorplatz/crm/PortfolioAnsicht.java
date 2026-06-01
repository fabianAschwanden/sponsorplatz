package ch.sponsorplatz.crm;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Aufbereitete Sicht der CRM-Portfolio-Liste einer Marke: gefilterte + sortierte
 * Accounts plus Kennzahlen (Gesamt-/Anzeige-Anzahl, gewichtete Forecast-Summe)
 * und der echo-zurückgegebene Filter-/Sortier-Zustand für die Toolbar.
 *
 * <p>Filter, Suche und Sortierung laufen server-seitig (Projekt-Konvention, kein
 * SPA) — die Logik steckt rein in {@link #erstelle} und ist damit ohne DB testbar.
 * Mirror des {@code SchaufensterAnsicht}-Musters.
 */
public record PortfolioAnsicht(
        List<SponsorAccountView> accounts,      // nur die aktuelle Seite
        List<UUID> gefilterteIds,               // ALLE gefilterten IDs (für „Alle N auswählen")
        int anzahlGesamt,
        int anzahlGezeigt,                       // gefilterte Gesamtzahl (über alle Seiten)
        BigDecimal forecastSummeChf,            // Summe über die gefilterte Menge (nicht nur Seite)
        String suche,
        AccountStatus filterStatus,
        PipelineStage filterPipeline,
        String sort,
        boolean absteigend,
        int seite,                               // 1-basiert
        int seitenGroesse,
        int anzahlSeiten
) {

    /** Zulässige Seitengrössen für die Auswahl im UI. */
    public static final List<Integer> SEITENGROESSEN = List.of(25, 50, 100);
    private static final int STANDARD_GROESSE = 25;

    /** True, wenn irgendein Filter/Such-Kriterium aktiv ist (steuert „zurücksetzen"/Leer-Hinweis). */
    public boolean istGefiltert() {
        return (suche != null && !suche.isBlank()) || filterStatus != null || filterPipeline != null;
    }

    /** True, wenn mehr Treffer als eine Seite — steuert die Sichtbarkeit der Paginierung. */
    public boolean hatMehrereSeiten() {
        return anzahlSeiten > 1;
    }

    public static PortfolioAnsicht erstelle(List<SponsorAccountView> alle,
                                            String suche, AccountStatus status, PipelineStage pipeline,
                                            String sort, boolean absteigend,
                                            int seite, int seitenGroesse) {
        String suchBegriff = (suche != null && !suche.isBlank()) ? suche.trim() : null;
        // Allowlist SEITENGROESSEN steuert nur das UI-Dropdown; serverseitig genügt > 0.
        int groesse = seitenGroesse > 0 ? seitenGroesse : STANDARD_GROESSE;

        List<SponsorAccountView> gefiltert = alle.stream()
                .filter(a -> status == null || a.status() == status)
                .filter(a -> pipeline == null || a.pipelineStage() == pipeline)
                .filter(a -> suchBegriff == null || passtZurSuche(a, suchBegriff))
                .toList();

        List<SponsorAccountView> sortiert = sortiere(gefiltert, sort, absteigend);

        BigDecimal forecastSumme = sortiert.stream()
                .map(SponsorAccountView::gewichteterForecastChf)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        List<UUID> gefilterteIds = sortiert.stream().map(SponsorAccountView::id).toList();

        // Paginierung: Seite auf gültigen Bereich klemmen.
        int anzahlSeiten = Math.max(1, (int) Math.ceil((double) sortiert.size() / groesse));
        int aktuelleSeite = Math.min(Math.max(1, seite), anzahlSeiten);
        int von = (aktuelleSeite - 1) * groesse;
        int bis = Math.min(von + groesse, sortiert.size());
        List<SponsorAccountView> seitenSlice = von >= sortiert.size() ? List.of() : sortiert.subList(von, bis);

        return new PortfolioAnsicht(seitenSlice, gefilterteIds, alle.size(), sortiert.size(), forecastSumme,
                suchBegriff, status, pipeline, sort, absteigend, aktuelleSeite, groesse, anzahlSeiten);
    }

    private static boolean passtZurSuche(SponsorAccountView a, String begriff) {
        String b = begriff.toLowerCase();
        return (a.vereinName() != null && a.vereinName().toLowerCase().contains(b))
                || (a.notiz() != null && a.notiz().toLowerCase().contains(b));
    }

    /** Sortiert nach der gewählten Spalte; {@code sort == null} bewahrt die Eingangsreihenfolge. */
    private static List<SponsorAccountView> sortiere(List<SponsorAccountView> liste,
                                                     String sort, boolean absteigend) {
        Comparator<SponsorAccountView> comparator = comparatorFuer(sort);
        if (comparator == null) {
            return liste;
        }
        if (absteigend) {
            comparator = comparator.reversed();
        }
        return liste.stream().sorted(comparator).toList();
    }

    private static Comparator<SponsorAccountView> comparatorFuer(String sort) {
        if (sort == null) return null;
        return switch (sort) {
            case "verein" -> Comparator.comparing(SponsorAccountView::vereinName,
                    Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER));
            case "status" -> Comparator.comparing(a -> a.status() != null ? a.status().name() : "",
                    Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER));
            case "pipeline" -> Comparator.comparing(a -> a.pipelineStage() != null ? a.pipelineStage().ordinal() : -1);
            case "tier" -> Comparator.comparing(a -> a.tier() != null ? a.tier().ordinal() : -1);
            case "forecast" -> Comparator.comparing(SponsorAccountView::gewichteterForecastChf,
                    Comparator.nullsFirst(Comparator.naturalOrder()));
            default -> null;
        };
    }
}
