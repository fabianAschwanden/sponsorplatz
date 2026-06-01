package ch.sponsorplatz.shared.util;

/**
 * String-Hilfsmethoden für die gesamte Plattform.
 *
 * <p>Zentrale Stelle statt privater Kopien in einzelnen Services
 * (OrganisationService, AppUserService, AufgabenDefinitionService etc.).
 */
public final class Strings {

    private Strings() {
        // Utility-Klasse
    }

    /**
     * Gibt {@code null} zurück wenn der String leer oder blank ist,
     * sonst den getrimmten Wert.
     */
    public static String leereAlsNull(String s) {
        return (s == null || s.isBlank()) ? null : s.trim();
    }
}

