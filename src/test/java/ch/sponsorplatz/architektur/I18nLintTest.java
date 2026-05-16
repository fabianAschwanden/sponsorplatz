package ch.sponsorplatz.architektur;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Lint-Style-Regel: HTML-Attribute mit deutschem Text (Umlaute oder
 * deutsche Stoppwörter) müssen ein {@code th:}-Pendant in der Nähe haben,
 * sonst sind sie nicht lokalisierbar.
 *
 * <p>Geprüfte Attribute: {@code placeholder}, {@code title}, {@code alt},
 * {@code aria-label}. Diese sind sichtbar für End-User und sollten via
 * {@code messages_*.properties} in allen vier Plattform-Sprachen verfügbar
 * sein.
 *
 * <p>Konvention im Code: Designer-Fallback bleibt im Plain-Attribut für
 * IDE-Preview, der th:-Bindung steht UNMITTELBAR ÜBER oder NEBEN dem
 * Plain-Attribut. Beispiel:
 * <pre>
 *   th:placeholder="#{einstellungen.profil.ortPlaceholder}"
 *   placeholder="z.B. Zürich"
 * </pre>
 *
 * <p>Allowlist:
 * <ul>
 *   <li>{@code admin/} — Admin-Backend ist intern, Plattform-Sprache Deutsch</li>
 *   <li>{@code home/datenschutz.html}, {@code home/impressum.html},
 *       {@code home/agb.html} — Legal-Texte werden bewusst nur auf Deutsch
 *       gepflegt; FR/IT/EN-Übersetzung erfordert juristische Prüfung</li>
 * </ul>
 *
 * <p>Test-IDs: ARCH-15 (i18n-Lint-Disziplin).
 */
class I18nLintTest {

    private static final Path TEMPLATES_ROOT = Paths.get("src/main/resources/templates");

    /** Pfade (relativ zu templates/), die das Lint NICHT prüft. */
    private static final List<String> ALLOWLIST = List.of(
            "admin/",                  // Admin-Backend deutschsprachig per Konvention
            "home/datenschutz.html",   // Legal-Text, juristische DE-Pflege
            "home/impressum.html",     // Legal-Text
            "fragments/"               // Querschnitts-Fragmente
    );

    /** Attribute, deren Text sichtbar für End-User wird. */
    private static final List<String> GEPRUEFTE_ATTRIBUTE = List.of(
            "placeholder", "title", "alt", "aria-label"
    );

    /** Umlaute oder typisch-deutsche Stoppwörter — Indiz für deutsche Sprache. */
    private static final Pattern DEUTSCH = Pattern.compile(
            "[äöüÄÖÜß]"
            + "|\\b(der|die|das|den|dem|für|von|mit|als|nach|über|nicht|"
            + "kann|werden|wird|soll|muss|eine?n?|sind|hat|haben|"
            + "zurück|weiter|wieder|öffnen|löschen|abbrechen|speichern|"
            + "anmelden|abmelden|verein|sponsor|anfrage|nachricht|"
            + "begründung|beschreibung)\\b",
            Pattern.CASE_INSENSITIVE);

    @Test
    @DisplayName("ARCH-15: HTML-Attribute mit deutschem Text haben th:-Pendant in der Nähe")
    void hardcodedGermanAttributesHaveThymeleafBinding() throws IOException {
        List<String> gaps = new ArrayList<>();

        try (Stream<Path> stream = Files.walk(TEMPLATES_ROOT)) {
            stream.filter(p -> p.toString().endsWith(".html"))
                    .filter(this::nichtAllowlisted)
                    .forEach(p -> gaps.addAll(checkeDatei(p)));
        }

        assertThat(gaps)
                .as("Fundstellen — diese Attribute brauchen ein th:-Pendant "
                        + "(unmittelbar drüber/daneben) oder gehören in die Allowlist:")
                .isEmpty();
    }

    private boolean nichtAllowlisted(Path p) {
        String relativ = TEMPLATES_ROOT.relativize(p).toString().replace('\\', '/');
        return ALLOWLIST.stream().noneMatch(relativ::startsWith);
    }

    private List<String> checkeDatei(Path datei) {
        List<String> gefunden = new ArrayList<>();
        List<String> zeilen;
        try {
            zeilen = Files.readAllLines(datei);
        } catch (IOException e) {
            return gefunden;
        }

        for (int i = 0; i < zeilen.size(); i++) {
            String zeile = zeilen.get(i);
            for (String attr : GEPRUEFTE_ATTRIBUTE) {
                Pattern pHart = Pattern.compile("\\b" + attr + "=\"([^\"]+)\"");
                var matcher = pHart.matcher(zeile);
                while (matcher.find()) {
                    String wert = matcher.group(1);
                    if (!DEUTSCH.matcher(wert).find()) continue;

                    // Allowlist: hat dieselbe Zeile, die Zeile davor oder
                    // die Zeile danach ein th:<attr>-Binding? Dann ist's ok.
                    if (hatThPendant(zeilen, i, attr)) continue;

                    String relativ = TEMPLATES_ROOT.relativize(datei).toString()
                            .replace('\\', '/');
                    gefunden.add(String.format("%s:%d  %s=\"%s\"",
                            relativ, i + 1, attr, wert));
                }
            }
        }
        return gefunden;
    }

    private boolean hatThPendant(List<String> zeilen, int idx, String attr) {
        String thAttr = "th:" + attr + "=";
        for (int j = Math.max(0, idx - 1); j <= Math.min(zeilen.size() - 1, idx + 1); j++) {
            if (zeilen.get(j).contains(thAttr)) return true;
        }
        return false;
    }
}
