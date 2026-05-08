package ch.sponsorplatz.admin;


import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * View-DTO für das Backlog-Listen-Template. Enthält das Enum als Objekt
 * (für Label + Vergleiche im Template) und die abgeleiteten Status-Flags.
 */
public record BacklogItemView(
        UUID id,
        String titel,
        String beschreibung,
        BacklogStatus status,
        BacklogPrioritaet prioritaet,
        Instant erstelltAm,
        String erstelltVon,
        Instant erledigtAm
) {
    public static BacklogItemView von(BacklogItem item) {
        return new BacklogItemView(
                item.getId(),
                item.getTitel(),
                item.getBeschreibung(),
                item.getStatus(),
                item.getPrioritaet(),
                item.getErstelltAm(),
                item.getErstelltVon(),
                item.getErledigtAm()
        );
    }

    public static List<BacklogItemView> von(List<BacklogItem> items) {
        return items.stream().map(BacklogItemView::von).toList();
    }
}
