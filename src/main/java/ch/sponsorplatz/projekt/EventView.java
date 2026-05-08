package ch.sponsorplatz.projekt;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * View-DTO fuer Event — keine Org-Entity, nur flache Felder.
 */
public record EventView(
        UUID id,
        String name,
        String slug,
        String beschreibung,
        String ort,
        LocalDate datum,
        LocalDate datumEnde,
        Integer kapazitaet,
        String orgName,
        String orgSlug
) {
    public static EventView von(Event event) {
        return new EventView(
                event.getId(),
                event.getName(),
                event.getSlug(),
                event.getBeschreibung(),
                event.getOrt(),
                event.getDatum(),
                event.getDatumEnde(),
                event.getKapazitaet(),
                event.getOrg().getName(),
                event.getOrg().getSlug()
        );
    }

    public static List<EventView> von(List<Event> events) {
        return events.stream().map(EventView::von).toList();
    }
}

