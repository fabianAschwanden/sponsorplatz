package ch.sponsorplatz.organisation;

/**
 * Branche einer Organisation — bewusst eingeschränkt auf den Health-Fokus
 * der Plattform.
 *
 * <p>Sponsorplatz positioniert sich strikt auf Sport und Gesundheit. Vereine
 * und Initiativen, die sich nicht in einem dieser Felder verorten, werden
 * im Verifizierungs-Workflow abgelehnt (siehe {@code 05_Rollenkonzept}).
 *
 * <p>Der Themen-Umfang ist <b>breit</b> gefasst — Bewegung, Reha, Mental
 * Health, Ernährung und Wellness gehören ausdrücklich dazu. Jede Erweiterung
 * verlangt eine bewusste Produktentscheidung und eine Flyway-Migration.
 */
public enum Branche {

    /** Klassischer Sportverein, Klub, Liga. */
    SPORT("Sport", "Vereine und Klubs im klassischen Vereinssport"),

    /** Bewegungsangebote ausserhalb des klassischen Vereinssports (Fitness, Outdoor, Tanz). */
    BEWEGUNG("Bewegung & Fitness", "Bewegungsangebote ausserhalb des klassischen Vereinssports"),

    /** Rehabilitation, Physiotherapie-nahe Angebote, Bewegungstherapie. */
    REHA("Rehabilitation", "Rehabilitation und Bewegungstherapie"),

    /** Behindertensport, inklusive Sportvereine, paralympische Disziplinen. */
    BEHINDERTENSPORT("Behindertensport", "Inklusive Sportvereine und paralympische Disziplinen"),

    /** Sport- und Bewegungsangebote für Seniorinnen und Senioren. */
    SENIORENSPORT("Seniorensport", "Sport- und Bewegungsangebote für Seniorinnen und Senioren"),

    /** Gesundheits-Prävention, Aufklärung, Vorsorge. */
    PRAEVENTION("Prävention", "Gesundheits-Prävention, Aufklärung und Vorsorge"),

    /** Mentale Gesundheit, Psychohygiene, Stressbewältigung. */
    MENTAL_HEALTH("Mentale Gesundheit", "Mentale Gesundheit, Psychohygiene und Stressbewältigung"),

    /** Ernährung, Esskultur, Foodliteracy. */
    ERNAEHRUNG("Ernährung", "Ernährung, Esskultur und Foodliteracy"),

    /** Wellness, Achtsamkeit, ganzheitliche Gesundheit. */
    WELLNESS("Wellness & Achtsamkeit", "Wellness, Achtsamkeit und ganzheitliche Gesundheit"),

    /** Selbsthilfegruppen rund um Gesundheitsthemen. */
    SELBSTHILFE("Selbsthilfe", "Selbsthilfegruppen rund um Gesundheitsthemen"),

    /** Patientenorganisationen, krankheitsspezifische Vereine. */
    PATIENTENORGANISATION("Patientenorganisation", "Patientenorganisationen und krankheitsspezifische Vereine");

    private final String anzeige;
    private final String beschreibung;

    Branche(String anzeige, String beschreibung) {
        this.anzeige = anzeige;
        this.beschreibung = beschreibung;
    }

    /**
     * Deutsche Anzeige für Templates und Auswahl-Listen.
     */
    public String getAnzeige() {
        return anzeige;
    }

    /**
     * Beschreibender Subhead-Text für das Vereinsprofil und Marken-Landing.
     */
    public String getBeschreibung() {
        return beschreibung;
    }
}
