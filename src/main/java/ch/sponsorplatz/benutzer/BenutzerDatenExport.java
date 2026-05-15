package ch.sponsorplatz.benutzer;

import java.util.Map;
import java.util.UUID;

/**
 * Port für den DSG-Datenexport eines Users — wird vom Einstellungen-Controller
 * konsumiert. Die Implementation lebt im {@code audit}-Paket (Adapter), damit
 * {@code benutzer} nicht in höhere Schichten greifen muss (ARCH-06).
 */
public interface BenutzerDatenExport {

    /** Bündelt alle DSG-relevanten User-Daten in einer JSON-serialisierbaren Map. */
    Map<String, Object> exportiere(UUID userId);
}
