package ch.sponsorplatz.aufgabe;

import ch.sponsorplatz.shared.exception.NotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit-Tests für {@link AufgabenDefinitionService}.
 * Test-IDs: AUFG-DEF-01..09
 */
@ExtendWith(MockitoExtension.class)
class AufgabenDefinitionServiceTest {

    @Mock
    private AufgabenDefinitionRepository repository;

    @InjectMocks
    private AufgabenDefinitionService service;

    private AufgabenDefinition systemDef;
    private AufgabenDefinition customDef;

    @BeforeEach
    void setUp() {
        systemDef = new AufgabenDefinition();
        systemDef.setId(UUID.randomUUID());
        systemDef.setTitel("Org freigeben");
        systemDef.setBeschreibung("Admin muss Org prüfen");
        systemDef.setTriggerEntityTyp(TriggerEntityTyp.ORG);
        systemDef.setTriggerStatus("PENDING");
        systemDef.setZielStatus("ACTIVE");
        systemDef.setAssigneeRegel(AssigneeRegel.PLATFORM_ADMIN);
        systemDef.setLinkTemplate("/admin/verifizierungen");
        systemDef.setAktiv(true);
        systemDef.setSystemDefinition(true);

        customDef = new AufgabenDefinition();
        customDef.setId(UUID.randomUUID());
        customDef.setTitel("Custom Task");
        customDef.setTriggerEntityTyp(TriggerEntityTyp.ANFRAGE);
        customDef.setTriggerStatus("NEU");
        customDef.setAssigneeRegel(AssigneeRegel.ANFRAGE_EMPFAENGER_ORG);
        customDef.setAktiv(true);
        customDef.setSystemDefinition(false);
    }

    @Test
    @DisplayName("AUFG-DEF-01: alle() liefert sortierte Liste")
    void alleListet() {
        when(repository.findAllByOrderByTitelAsc()).thenReturn(List.of(customDef, systemDef));

        List<AufgabenDefinition> result = service.alle();

        assertThat(result).hasSize(2);
        verify(repository).findAllByOrderByTitelAsc();
    }

    @Test
    @DisplayName("AUFG-DEF-02: findeNachId() wirft NotFoundException bei unbekannter ID")
    void findeNachIdWirftBeiUnbekannt() {
        UUID id = UUID.randomUUID();
        when(repository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findeNachId(id))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    @DisplayName("AUFG-DEF-03: findeFormular() befüllt DTO korrekt")
    void findeFormularMapptFelder() {
        when(repository.findById(systemDef.getId())).thenReturn(Optional.of(systemDef));

        AufgabenDefinitionFormDto dto = service.findeFormular(systemDef.getId());

        assertThat(dto.getTitel()).isEqualTo("Org freigeben");
        assertThat(dto.getBeschreibung()).isEqualTo("Admin muss Org prüfen");
        assertThat(dto.getTriggerEntityTyp()).isEqualTo(TriggerEntityTyp.ORG);
        assertThat(dto.getTriggerStatus()).isEqualTo("PENDING");
        assertThat(dto.getZielStatus()).isEqualTo("ACTIVE");
        assertThat(dto.getAssigneeRegel()).isEqualTo(AssigneeRegel.PLATFORM_ADMIN);
        assertThat(dto.getLinkTemplate()).isEqualTo("/admin/verifizierungen");
        assertThat(dto.isAktiv()).isTrue();
    }

    @Test
    @DisplayName("AUFG-DEF-04: erstelle() legt neue Definition an (nicht systemDefinition)")
    void erstelleLegtNeuAn() {
        AufgabenDefinitionFormDto dto = new AufgabenDefinitionFormDto();
        dto.setTitel(" Neue Aufgabe ");
        dto.setTriggerEntityTyp(TriggerEntityTyp.VERTRAG);
        dto.setTriggerStatus("ENTWURF");
        dto.setZielStatus("UNTERZEICHNET");
        dto.setAssigneeRegel(AssigneeRegel.VERTRAG_VEREIN_ORG);
        dto.setAktiv(true);

        service.erstelle(dto, "admin@test.ch");

        ArgumentCaptor<AufgabenDefinition> captor = ArgumentCaptor.forClass(AufgabenDefinition.class);
        verify(repository).save(captor.capture());
        AufgabenDefinition saved = captor.getValue();
        assertThat(saved.getTitel()).isEqualTo("Neue Aufgabe");
        assertThat(saved.isSystemDefinition()).isFalse();
        assertThat(saved.getErstelltVon()).isEqualTo("admin@test.ch");
        assertThat(saved.getTriggerEntityTyp()).isEqualTo(TriggerEntityTyp.VERTRAG);
    }

    @Test
    @DisplayName("AUFG-DEF-05: aktualisiere() bei System-Def ignoriert Trigger-Felder")
    void aktualisiereSystemDefSchütztTriggerFelder() {
        when(repository.findById(systemDef.getId())).thenReturn(Optional.of(systemDef));

        AufgabenDefinitionFormDto dto = new AufgabenDefinitionFormDto();
        dto.setTitel("Neuer Titel");
        dto.setBeschreibung("Neue Beschreibung");
        dto.setTriggerEntityTyp(TriggerEntityTyp.RECHNUNG); // sollte ignoriert werden
        dto.setTriggerStatus("OFFEN");                       // sollte ignoriert werden
        dto.setZielStatus("BEZAHLT");                        // sollte ignoriert werden
        dto.setAssigneeRegel(AssigneeRegel.VERTRAG_SPONSOR_ORG); // ignoriert
        dto.setLinkTemplate("/neu");
        dto.setAktiv(false);

        service.aktualisiere(systemDef.getId(), dto);

        verify(repository).save(any());
        // System-Trigger bleibt unverändert
        assertThat(systemDef.getTriggerEntityTyp()).isEqualTo(TriggerEntityTyp.ORG);
        assertThat(systemDef.getTriggerStatus()).isEqualTo("PENDING");
        assertThat(systemDef.getZielStatus()).isEqualTo("ACTIVE");
        assertThat(systemDef.getAssigneeRegel()).isEqualTo(AssigneeRegel.PLATFORM_ADMIN);
        // Anzeige-Felder sind aktualisiert
        assertThat(systemDef.getTitel()).isEqualTo("Neuer Titel");
        assertThat(systemDef.getBeschreibung()).isEqualTo("Neue Beschreibung");
        assertThat(systemDef.getLinkTemplate()).isEqualTo("/neu");
        assertThat(systemDef.isAktiv()).isFalse();
    }

    @Test
    @DisplayName("AUFG-DEF-06: aktualisiere() bei Custom-Def ändert alle Felder")
    void aktualisiereCustomDefAendertAlles() {
        when(repository.findById(customDef.getId())).thenReturn(Optional.of(customDef));

        AufgabenDefinitionFormDto dto = new AufgabenDefinitionFormDto();
        dto.setTitel("Geändert");
        dto.setTriggerEntityTyp(TriggerEntityTyp.RECHNUNG);
        dto.setTriggerStatus("BEZAHLT");
        dto.setZielStatus("");  // leer → null
        dto.setAssigneeRegel(AssigneeRegel.PLATFORM_ADMIN);
        dto.setLinkTemplate("");
        dto.setAktiv(false);

        service.aktualisiere(customDef.getId(), dto);

        assertThat(customDef.getTriggerEntityTyp()).isEqualTo(TriggerEntityTyp.RECHNUNG);
        assertThat(customDef.getTriggerStatus()).isEqualTo("BEZAHLT");
        assertThat(customDef.getZielStatus()).isNull();
        assertThat(customDef.getAssigneeRegel()).isEqualTo(AssigneeRegel.PLATFORM_ADMIN);
        assertThat(customDef.getLinkTemplate()).isNull();
    }

    @Test
    @DisplayName("AUFG-DEF-07: loesche() wirft bei System-Definition")
    void loescheBlockiertSystemDef() {
        when(repository.findById(systemDef.getId())).thenReturn(Optional.of(systemDef));

        assertThatThrownBy(() -> service.loesche(systemDef.getId()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("System-Aufgaben-Definitionen");
    }

    @Test
    @DisplayName("AUFG-DEF-08: loesche() entfernt Custom-Def")
    void loescheEntferntCustomDef() {
        when(repository.findById(customDef.getId())).thenReturn(Optional.of(customDef));

        service.loesche(customDef.getId());

        verify(repository).deleteById(customDef.getId());
    }

    @Test
    @DisplayName("AUFG-DEF-09: istSystemDefinition() delegiert an Entity")
    void istSystemDefinitionDelegiert() {
        when(repository.findById(systemDef.getId())).thenReturn(Optional.of(systemDef));
        when(repository.findById(customDef.getId())).thenReturn(Optional.of(customDef));

        assertThat(service.istSystemDefinition(systemDef.getId())).isTrue();
        assertThat(service.istSystemDefinition(customDef.getId())).isFalse();
    }
}
