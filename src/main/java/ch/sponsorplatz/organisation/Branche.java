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
    SPORT("Sport"),

    /** Bewegungsangebote ausserhalb des klassischen Vereinssports (Fitness, Outdoor, Tanz). */
    BEWEGUNG("Bewegung & Fitness"),

    /** Rehabilitation, Physiotherapie-nahe Angebote, Bewegungstherapie. */
    REHA("Rehabilitation"),

    /** Behindertensport, inklusive Sportvereine, paralympische Disziplinen. */
    BEHINDERTENSPORT("Behindertensport"),

    /** Sport- und Bewegungsangebote für Seniorinnen und Senioren. */
    SENIORENSPORT("Seniorensport"),

    /** Gesundheits-Prävention, Aufklärung, Vorsorge. */
    PRAEVENTION("Prävention"),

    /** Mentale Gesundheit, Psychohygiene, Stressbewältigung. */
    MENTAL_HEALTH("Mentale Gesundheit"),

    /** Ernährung, Esskultur, Foodliteracy. */
    ERNAEHRUNG("Ernährung"),

    /** Wellness, Achtsamkeit, ganzheitliche Gesundheit. */
    WELLNESS("Wellness & Achtsamkeit"),

    /** Selbsthilfegruppen rund um Gesundheitsthemen. */
    SELBSTHILFE("Selbsthilfe"),

    /** Patientenorganisationen, krankheitsspezifische Vereine. */
    PATIENTENORGANISATION("Patientenorganisation");

    private final String anzeige;

    Branche(String anzeige) {
        this.anzeige = anzeige;
    }

    /**
     * Deutsche Anzeige für Templates und Auswahl-Listen.
     */
    public String getAnzeige() {
        return anzeige;
    }
}
