package ch.sponsorplatz.aufgabe;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Read-only View-DTO für die /aufgaben-Seite. Flacht den Assignee-Org-Namen
 * ein und stellt den Definitions-Titel (zum Zeitpunkt der Erstellung kopiert)
 * direkt bereit.
 */
public record AufgabeView(
        UUID id,
        String titel,
        String link,
        AufgabenStatus status,
        TriggerEntityTyp entityTyp,
        UUID entityId,
        String assigneeOrgName,
        boolean nurPlatformAdmin,
        Instant erstelltAm,
        Instant erledigtAm
) {

    public static AufgabeView von(Aufgabe a) {
        return new AufgabeView(
                a.getId(), a.getTitel(), a.getLink(), a.getStatus(),
                a.getEntityTyp(), a.getEntityId(),
                a.getAssigneeOrg() != null ? a.getAssigneeOrg().getName() : null,
                a.isNurPlatformAdmin(),
                a.getErstelltAm(), a.getErledigtAm());
    }

    public static List<AufgabeView> von(List<Aufgabe> aufgaben) {
        return aufgaben.stream().map(AufgabeView::von).toList();
    }
}
