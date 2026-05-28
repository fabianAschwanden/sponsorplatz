package ch.sponsorplatz.anfrage;

import ch.sponsorplatz.organisation.Branche;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Aufbereitete Sicht für das öffentliche Engagement-Schaufenster einer Marke
 * (PROJEKT_INFO §"Öffentliches Schaufenster"). Bündelt den Marken-Kopf
 * (Name + Logo), die nach Region gruppierten Engagements sowie die für die
 * Filter-Leiste verfügbaren Regionen/Branchen und ein paar Kennzahlen.
 *
 * <p>Die Filter- und Gruppierungs-Logik steckt in {@link #erstelle} und ist
 * damit ohne DB pur testbar. Region = {@code Projekt.ort}; Engagements ohne Ort
 * landen unter dem leeren Schlüssel ({@code ""}) und werden vom Template als
 * „ohne Region" beschriftet.
 */
public record SchaufensterAnsicht(
        String sponsorName,
        String sponsorSlug,
        String sponsorLogoUrl,
        Map<String, List<EngagementView>> nachRegion,
        List<String> verfuegbareRegionen,
        List<Branche> verfuegbareBranchen,
        int anzahlVereine,
        int anzahlRegionen,
        String filterRegion,
        Branche filterBranche
) {

    public boolean istLeer() {
        return nachRegion.isEmpty();
    }

    /** Regionen alphabetisch, der „ohne Region"-Eimer ({@code ""}) immer zuletzt. */
    private static final Comparator<String> REGION_ORDNUNG =
            Comparator.comparing((String r) -> r.isEmpty() ? 1 : 0)
                    .thenComparing(r -> r, String.CASE_INSENSITIVE_ORDER);

    public static SchaufensterAnsicht erstelle(String sponsorName, String sponsorSlug,
                                               String sponsorLogoUrl,
                                               List<EngagementView> alle,
                                               String filterRegion, Branche filterBranche) {
        String region = (filterRegion != null && !filterRegion.isBlank()) ? filterRegion.trim() : null;

        List<String> verfuegbareRegionen = alle.stream()
                .map(EngagementView::region)
                .filter(o -> o != null && !o.isBlank())
                .map(String::trim)
                .distinct()
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .toList();

        List<Branche> verfuegbareBranchen = alle.stream()
                .map(EngagementView::vereinBranche)
                .filter(b -> b != null)
                .distinct()
                .sorted(Comparator.comparing(Branche::getAnzeige))
                .toList();

        List<EngagementView> gefiltert = alle.stream()
                .filter(e -> region == null || region.equalsIgnoreCase(schluessel(e)))
                .filter(e -> filterBranche == null || filterBranche == e.vereinBranche())
                .toList();

        Map<String, List<EngagementView>> nachRegion = gefiltert.stream()
                .collect(Collectors.groupingBy(
                        SchaufensterAnsicht::schluessel,
                        () -> new LinkedHashMap<>(),
                        Collectors.toList()));
        Map<String, List<EngagementView>> sortiert = new LinkedHashMap<>();
        nachRegion.entrySet().stream()
                .sorted(Map.Entry.comparingByKey(REGION_ORDNUNG))
                .forEach(en -> sortiert.put(en.getKey(), en.getValue()));

        int anzahlVereine = (int) gefiltert.stream().map(EngagementView::vereinSlug).distinct().count();
        int anzahlRegionen = (int) gefiltert.stream()
                .map(SchaufensterAnsicht::schluessel)
                .filter(s -> !s.isEmpty())
                .distinct().count();

        return new SchaufensterAnsicht(sponsorName, sponsorSlug, sponsorLogoUrl, sortiert,
                verfuegbareRegionen, verfuegbareBranchen, anzahlVereine, anzahlRegionen,
                region, filterBranche);
    }

    /** Region-Gruppierungs-Schlüssel: getrimmter Ort, {@code ""} wenn keiner. */
    private static String schluessel(EngagementView e) {
        return (e.region() == null || e.region().isBlank()) ? "" : e.region().trim();
    }
}
