package ch.sponsorplatz.aufgabe;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Read-only View-DTO für Admin-Templates. Hält die Entity vom Template fern
 * (CLAUDE.md View-DTO-Pflicht).
 */
public record AufgabenDefinitionView(
        UUID id,
        String titel,
        String beschreibung,
        TriggerEntityTyp triggerEntityTyp,
        String triggerStatus,
        String zielStatus,
        AssigneeRegel assigneeRegel,
        String linkTemplate,
        boolean aktiv,
        boolean systemDefinition,
        Instant erstelltAm
) {

    public static AufgabenDefinitionView von(AufgabenDefinition d) {
        return new AufgabenDefinitionView(
                d.getId(), d.getTitel(), d.getBeschreibung(),
                d.getTriggerEntityTyp(), d.getTriggerStatus(), d.getZielStatus(),
                d.getAssigneeRegel(), d.getLinkTemplate(),
                d.isAktiv(), d.isSystemDefinition(), d.getErstelltAm());
    }

    public static List<AufgabenDefinitionView> von(List<AufgabenDefinition> defs) {
        return defs.stream().map(AufgabenDefinitionView::von).toList();
    }
}
