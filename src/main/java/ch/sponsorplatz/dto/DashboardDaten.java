package ch.sponsorplatz.dto;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.WeekFields;
import java.util.Locale;

/**
 * Aggregierte Dashboard-Kennzahlen + View-Strings für einen Benutzer.
 * Wird vom DashboardService befüllt — keine JPA-Entity.
 *
 * <p>M5-Fix: aktuellerMonat / aktuelleKw werden hier zentral berechnet,
 * damit der Controller View-frei bleibt.</p>
 */
public record DashboardDaten(
        long anzahlOrganisationen,
        long anzahlProjekte,
        long anzahlAnfragen,
        long anzahlOffeneAnfragen,
        String aktuellerMonat,
        String aktuelleKw
) {

    private static final DateTimeFormatter MONAT_JAHR =
            DateTimeFormatter.ofPattern("MMMM yyyy", Locale.GERMAN);

    /** Leeres DTO mit aktuellem Monat/KW — für unbekannte User oder ohne Mitgliedschaften. */
    public static DashboardDaten leer() {
        return zaehlerLeerMitDatum(LocalDate.now());
    }

    public static DashboardDaten zaehlerLeerMitDatum(LocalDate heute) {
        return new DashboardDaten(0, 0, 0, 0,
                heute.format(MONAT_JAHR),
                "KW " + heute.get(WeekFields.ISO.weekOfWeekBasedYear()));
    }

    /** Konstruiert ein vollständiges DTO mit Zählern + heutiger Monats-/KW-Anzeige. */
    public static DashboardDaten von(long orgs, long projekte, long anfragen, long offen) {
        LocalDate heute = LocalDate.now();
        return new DashboardDaten(orgs, projekte, anfragen, offen,
                heute.format(MONAT_JAHR),
                "KW " + heute.get(WeekFields.ISO.weekOfWeekBasedYear()));
    }
}
