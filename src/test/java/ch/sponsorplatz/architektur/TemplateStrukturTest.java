package ch.sponsorplatz.architektur;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.StreamSupport;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Statische Regel — analog zu {@code ArchitekturRegelnTest}, aber für die
 * Thymeleaf-Templates: jedes Template muss in einem Feature-Unterordner liegen,
 * dessen Name einem bestehenden Java-Bounded-Context entspricht
 * (z.B. {@code templates/organisation/foo.html} ↔ {@code ch.sponsorplatz.organisation}).
 *
 * <p>ArchUnit kennt nur Java-Klassen — daher hier eine schlanke JUnit-Regel,
 * die das Dateisystem inspiziert. Verhindert dass die nach Phase „Templates
 * strukturiert" sauber aufgeteilten 47 Templates wieder im Top-Level landen.
 *
 * <p>Test-IDs: ARCH-14 (Template-Folder-Disziplin).
 */
class TemplateStrukturTest {

    /** Top-Level-Templates, die nicht in einem Feature-Folder liegen müssen — Spring-Konvention. */
    private static final Set<String> ERLAUBTE_TOPLEVEL = Set.of("error.html");

    /** Sub-Folder, die KEINEN Java-Paket-Match brauchen — Querschnitts-Templates. */
    private static final Set<String> AUSNAHME_FOLDER = Set.of("fragments");

    @Test
    @DisplayName("ARCH-14: jedes Template liegt in einem Feature-Folder, der einem Java-Paket entspricht")
    void templatesLiegenInFeatureFolderMitJavaPaket() throws Exception {
        Path templatesRoot = Paths.get("src/main/resources/templates");
        Path javaRoot = Paths.get("src/main/java/ch/sponsorplatz");

        Set<String> javaPakete = lesePakete(javaRoot);

        // 1) Keine zusätzlichen Top-Level-HTML-Files ausser den erlaubten
        Set<String> toplevelHtml = new TreeSet<>();
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(templatesRoot, "*.html")) {
            for (Path p : stream) {
                toplevelHtml.add(p.getFileName().toString());
            }
        }
        toplevelHtml.removeAll(ERLAUBTE_TOPLEVEL);
        assertThat(toplevelHtml)
                .as("Top-Level-Templates sind verboten — gehören in Feature-Folder "
                        + "(Ausnahme: " + ERLAUBTE_TOPLEVEL + ")")
                .isEmpty();

        // 2) Jeder Sub-Folder unter templates/ entspricht einem Java-Paket
        Set<String> unbekannteFolder = new TreeSet<>();
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(templatesRoot)) {
            for (Path p : stream) {
                if (!Files.isDirectory(p)) continue;
                String name = p.getFileName().toString();
                if (AUSNAHME_FOLDER.contains(name)) continue;
                if (!javaPakete.contains(name)) {
                    unbekannteFolder.add(name);
                }
            }
        }
        assertThat(unbekannteFolder)
                .as("Template-Folder ohne korrespondierendes ch.sponsorplatz.<name>-Java-Paket "
                        + "(Ausnahme: " + AUSNAHME_FOLDER + ")")
                .isEmpty();
    }

    private Set<String> lesePakete(Path javaRoot) throws Exception {
        Set<String> result = new HashSet<>();
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(javaRoot)) {
            StreamSupport.stream(stream.spliterator(), false)
                    .filter(Files::isDirectory)
                    .map(p -> p.getFileName().toString())
                    .forEach(result::add);
        }
        return result;
    }
}
