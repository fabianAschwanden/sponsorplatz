package ch.sponsorplatz.aufgabe;

import ch.sponsorplatz.shared.exception.NotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Admin-CRUD für {@link AufgabenDefinition}. System-Seeds (V36) bleiben in der DB,
 * können aber im UI nur eingeschränkt verändert werden — Trigger-Felder sind
 * gegen versehentliches Anfassen geschützt, damit die Engine-Verkabelung mit
 * den Service-Triggern (PENDING → Org-Freigabe etc.) intakt bleibt.
 */
@Service
@Transactional
public class AufgabenDefinitionService {

    private final AufgabenDefinitionRepository repository;

    public AufgabenDefinitionService(AufgabenDefinitionRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public List<AufgabenDefinition> alle() {
        return repository.findAllByOrderByTitelAsc();
    }

    @Transactional(readOnly = true)
    public AufgabenDefinition findeNachId(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new NotFoundException("Aufgaben-Definition nicht gefunden: " + id));
    }

    /** Form-DTO für das Bearbeiten-UI — Controller bekommt so keine Entity. */
    @Transactional(readOnly = true)
    public AufgabenDefinitionFormDto findeFormular(UUID id) {
        AufgabenDefinition def = findeNachId(id);
        AufgabenDefinitionFormDto dto = new AufgabenDefinitionFormDto();
        dto.setTitel(def.getTitel());
        dto.setBeschreibung(def.getBeschreibung());
        dto.setTriggerEntityTyp(def.getTriggerEntityTyp());
        dto.setTriggerStatus(def.getTriggerStatus());
        dto.setZielStatus(def.getZielStatus());
        dto.setAssigneeRegel(def.getAssigneeRegel());
        dto.setLinkTemplate(def.getLinkTemplate());
        dto.setAktiv(def.isAktiv());
        return dto;
    }

    /** Boolean-Helper für das Form-Fieldset-Locking — Controller braucht nicht die Entity. */
    @Transactional(readOnly = true)
    public boolean istSystemDefinition(UUID id) {
        return findeNachId(id).isSystemDefinition();
    }

    public void erstelle(AufgabenDefinitionFormDto dto, String erstelltVon) {
        AufgabenDefinition def = new AufgabenDefinition();
        wendeFormAn(def, dto, false);
        def.setSystemDefinition(false);
        def.setErstelltVon(erstelltVon);
        repository.save(def);
    }

    public void aktualisiere(UUID id, AufgabenDefinitionFormDto dto) {
        AufgabenDefinition def = findeNachId(id);
        wendeFormAn(def, dto, def.isSystemDefinition());
        repository.save(def);
    }

    public void loesche(UUID id) {
        AufgabenDefinition def = findeNachId(id);
        if (def.isSystemDefinition()) {
            throw new IllegalStateException(
                    "System-Aufgaben-Definitionen können nicht gelöscht werden — nur deaktivieren.");
        }
        repository.deleteById(id);
    }

    /**
     * Felder anwenden. Bei System-Definitionen werden die Engine-relevanten
     * Felder ignoriert — Admin darf nur Anzeige-Text + Aktiv-Flag pflegen.
     */
    private void wendeFormAn(AufgabenDefinition def, AufgabenDefinitionFormDto dto, boolean systemSchutz) {
        def.setTitel(dto.getTitel().trim());
        def.setBeschreibung(leereAlsNull(dto.getBeschreibung()));
        def.setLinkTemplate(leereAlsNull(dto.getLinkTemplate()));
        def.setAktiv(dto.isAktiv());
        if (!systemSchutz) {
            def.setTriggerEntityTyp(dto.getTriggerEntityTyp());
            def.setTriggerStatus(dto.getTriggerStatus().trim());
            def.setZielStatus(leereAlsNull(dto.getZielStatus()));
            def.setAssigneeRegel(dto.getAssigneeRegel());
        }
    }

    private static String leereAlsNull(String s) {
        return (s == null || s.isBlank()) ? null : s.trim();
    }
}
