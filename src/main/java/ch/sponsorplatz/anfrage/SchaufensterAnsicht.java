package ch.sponsorplatz.anfrage;

import ch.sponsorplatz.organisation.Branche;
import ch.sponsorplatz.organisation.Kanton;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Aufbereitete Sicht für das öffentliche Engagement-Schaufenster einer Marke
 * (PROJEKT_INFO §"Öffentliches Schaufenster"). Bündelt den Marken-Kopf
 * (Name + Logo), die nach Kanton gruppierten Engagements sowie die für die
 * Filter-Leiste verfügbaren Kantone/Branchen und ein paar Kennzahlen.
 *
 * <p>Die Filter- und Gruppierungs-Logik steckt in {@link #erstelle} und ist
 * damit ohne DB pur testbar. Der Kanton wird aus der Verein-PLZ abgeleitet
 * ({@link EngagementView#kanton()}); Engagements ohne erkennbaren Kanton landen
 * unter dem leeren Schlüssel ({@code ""}) → Template-Label „Übrige Schweiz".
 */
public record SchaufensterAnsicht(
        String sponsorName,
        String sponsorSlug,
        String sponsorLogoUrl,
        Map<String, List<EngagementView>> nachKanton,
        List<Kanton> verfuegbareKantone,
        List<Branche> verfuegbareBranchen,
        int anzahlVereine,
        int anzahlKantone,
        Kanton filterKanton,
        Branche filterBranche
) {

    public boolean istLeer() {
        return nachKanton.isEmpty();
    }

    public static SchaufensterAnsicht erstelle(String sponsorName, String sponsorSlug,
                                               String sponsorLogoUrl,
                                               List<EngagementView> alle,
                                               String filterKantonCode, Branche filterBranche) {
        Kanton filterKanton = parseKanton(filterKantonCode);

        List<Kanton> verfuegbareKantone = alle.stream()
                .map(EngagementView::kanton)
                .filter(k -> k != null)
                .distinct()
                .sorted(Comparator.comparing(Kanton::getAnzeige))
                .toList();

        List<Branche> verfuegbareBranchen = alle.stream()
                .map(EngagementView::vereinBranche)
                .filter(b -> b != null)
                .distinct()
                .sorted(Comparator.comparing(Branche::getAnzeige))
                .toList();

        List<EngagementView> gefiltert = alle.stream()
                .filter(e -> filterKanton == null || filterKanton == e.kanton())
                .filter(e -> filterBranche == null || filterBranche == e.vereinBranche())
                .toList();

        Map<String, List<EngagementView>> gruppen = gefiltert.stream()
                .collect(Collectors.groupingBy(
                        SchaufensterAnsicht::schluessel,
                        LinkedHashMap::new,
                        Collectors.toList()));
        Map<String, List<EngagementView>> sortiert = new LinkedHashMap<>();
        gruppen.entrySet().stream()
                .sorted(Map.Entry.comparingByKey(KANTON_ORDNUNG))
                .forEach(en -> sortiert.put(en.getKey(), en.getValue()));

        int anzahlVereine = (int) gefiltert.stream().map(EngagementView::vereinSlug).distinct().count();
        int anzahlKantone = (int) gefiltert.stream()
                .map(EngagementView::kanton)
                .filter(k -> k != null)
                .distinct().count();

        return new SchaufensterAnsicht(sponsorName, sponsorSlug, sponsorLogoUrl, sortiert,
                verfuegbareKantone, verfuegbareBranchen, anzahlVereine, anzahlKantone,
                filterKanton, filterBranche);
    }

    /** Gruppierungs-Schlüssel: Kanton-Code (z.B. „ZH") oder {@code ""} bei unbekanntem Kanton. */
    private static String schluessel(EngagementView e) {
        return e.kanton() == null ? "" : e.kanton().name();
    }

    /** Kantone nach Anzeige-Namen sortiert, „Übrige Schweiz" ({@code ""}) zuletzt. */
    private static final Comparator<String> KANTON_ORDNUNG =
            Comparator.comparing((String code) -> code.isEmpty() ? 1 : 0)
                    .thenComparing(code -> code.isEmpty() ? "" : Kanton.valueOf(code).getAnzeige(),
                            String.CASE_INSENSITIVE_ORDER);

    private static Kanton parseKanton(String code) {
        if (code == null || code.isBlank()) return null;
        try {
            return Kanton.valueOf(code.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
