package ch.sponsorplatz.anfrage;

import ch.sponsorplatz.organisation.Kanton;

import java.util.List;

/**
 * Datenpaket für den Engagement-Teaser auf der Startseite: die (nach Kanton
 * gefilterten und limitierten) Engagements für das Karten-Raster, die für die
 * Inline-Auswahlbox verfügbaren Kantone, der aktive Kanton-Filter sowie ein
 * Flag, ob überhaupt Engagements existieren (steuert die Sichtbarkeit der
 * Sektion, damit die Auswahlbox auch bei leerem Kanton-Treffer erhalten bleibt).
 */
public record StartseitenTeaser(
        List<EngagementView> engagements,
        List<Kanton> verfuegbareKantone,
        Kanton filterKanton,
        boolean vorhanden
) {
}
