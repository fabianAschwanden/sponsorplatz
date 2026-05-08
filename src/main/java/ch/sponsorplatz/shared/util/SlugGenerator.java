package ch.sponsorplatz.shared.util;

import org.springframework.stereotype.Component;

import java.text.Normalizer;
import java.util.function.Predicate;
import java.util.regex.Pattern;

/**
 * Generiert URL-freundliche Slugs aus deutschem/schweizer Text.
 *
 * - Umlaute: ä→ae, ö→oe, ü→ue, ß→ss
 * - Lowercase
 * - Leerzeichen → -
 * - alles andere als [a-z0-9-] entfernen
 * - mehrfache - reduzieren
 * - führende/abschließende - entfernen
 */
@Component
public class SlugGenerator {

    private static final Pattern MEHRFACH_BINDESTRICH = Pattern.compile("-{2,}");
    private static final Pattern UNGUELTIG            = Pattern.compile("[^a-z0-9-]");

    public String fromName(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Name darf nicht leer sein");
        }

        String slug = name
            .toLowerCase()
            .replace("ä", "ae")
            .replace("ö", "oe")
            .replace("ü", "ue")
            .replace("ß", "ss");

        // restliche Akzente entfernen (é, à, ç, …) — Decompose-Form, dann Combining Marks droppen
        slug = Normalizer.normalize(slug, Normalizer.Form.NFD)
                         .replaceAll("\\p{InCombiningDiacriticalMarks}+", "");

        slug = slug.replace(' ', '-');
        slug = UNGUELTIG.matcher(slug).replaceAll("");
        slug = MEHRFACH_BINDESTRICH.matcher(slug).replaceAll("-");
        slug = trimBindestriche(slug);

        if (slug.isEmpty()) {
            throw new IllegalArgumentException("Name ergibt keinen gültigen Slug: " + name);
        }
        return slug;
    }

    /**
     * Findet einen freien Slug zum gegebenen Namen — wenn der Basis-Slug schon
     * belegt ist, hängt {@code -1}, {@code -2}, … an, bis ein freier gefunden ist.
     *
     * @param name      Original-Name (für die Slug-Basis)
     * @param istBelegt Predicate, das prüft ob ein Kandidat bereits existiert
     *                  (typisch: {@code repository::existsBySlug})
     */
    public String findeFreienSlug(String name, Predicate<String> istBelegt) {
        String basis = fromName(name);
        String slug = basis;
        int zaehler = 1;
        while (istBelegt.test(slug)) {
            slug = basis + "-" + zaehler++;
        }
        return slug;
    }

    private String trimBindestriche(String s) {
        int von = 0;
        int bis = s.length();
        while (von < bis && s.charAt(von) == '-') {
            von++;
        }
        while (bis > von && s.charAt(bis - 1) == '-') {
            bis--;
        }
        return s.substring(von, bis);
    }
}
