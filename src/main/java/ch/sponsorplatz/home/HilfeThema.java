package ch.sponsorplatz.home;

import java.util.Arrays;
import java.util.Optional;

/**
 * Hilfe-Themen mit Detailseite. Jedes Thema entspricht einer Kachel auf
 * {@code /hilfe} und einer Detailseite unter {@code /hilfe/{slug}}.
 *
 * <p>Der {@code i18nSchluessel} ist die Basis für alle Texte des Themas
 * (Titel/Intro/Schritte) in {@code messages_*.properties}; {@code anzahlSchritte}
 * legt fest, wie viele nummerierte Schritte das Detail-Template rendert.
 */
public enum HilfeThema {

    DASHBOARD("dashboard", "dashboard", 4),
    ORGANISATIONEN("organisationen", "organisationen", 5),
    PROJEKTE("projekte", "projekte", 5),
    MARKTPLATZ("marktplatz", "marktplatz", 4),
    ANFRAGEN("anfragen", "anfragen", 5),
    AUFGABEN("aufgaben", "aufgaben", 4),
    ZWEIFA("2fa", "zweifa", 5),
    CRM("crm", "crm", 5);

    private final String slug;
    private final String i18nSchluessel;
    private final int anzahlSchritte;

    HilfeThema(String slug, String i18nSchluessel, int anzahlSchritte) {
        this.slug = slug;
        this.i18nSchluessel = i18nSchluessel;
        this.anzahlSchritte = anzahlSchritte;
    }

    public String slug() {
        return slug;
    }

    public String i18nSchluessel() {
        return i18nSchluessel;
    }

    public int anzahlSchritte() {
        return anzahlSchritte;
    }

    /** Findet ein Thema anhand seines URL-Slugs (case-insensitive). */
    public static Optional<HilfeThema> nachSlug(String slug) {
        if (slug == null) {
            return Optional.empty();
        }
        return Arrays.stream(values())
                .filter(t -> t.slug.equalsIgnoreCase(slug))
                .findFirst();
    }
}
