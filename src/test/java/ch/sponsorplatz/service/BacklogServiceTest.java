package ch.sponsorplatz.service;

import ch.sponsorplatz.exception.NotFoundException;
import ch.sponsorplatz.model.BacklogItem;
import ch.sponsorplatz.model.BacklogPrioritaet;
import ch.sponsorplatz.model.BacklogStatus;
import ch.sponsorplatz.repository.BacklogItemRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests für {@link BacklogService}.
 *
 * Test-IDs: BL-01..06.
 */
@ExtendWith(MockitoExtension.class)
class BacklogServiceTest {

    @Mock private BacklogItemRepository repository;

    private BacklogService service;

    @BeforeEach
    void setUp() {
        service = new BacklogService(repository);
    }

    @Test
    @DisplayName("BL-01: erstelle setzt Status OFFEN, Priorität, erstelltVon")
    void erstelleSetztDefaults() {
        when(repository.save(any(BacklogItem.class))).thenAnswer(inv -> inv.getArgument(0));

        BacklogItem item = service.erstelle("PDF-Export", "Use-Case", BacklogPrioritaet.HOCH, "fabian");

        assertThat(item.getTitel()).isEqualTo("PDF-Export");
        assertThat(item.getBeschreibung()).isEqualTo("Use-Case");
        assertThat(item.getStatus()).isEqualTo(BacklogStatus.OFFEN);
        assertThat(item.getPrioritaet()).isEqualTo(BacklogPrioritaet.HOCH);
        assertThat(item.getErstelltVon()).isEqualTo("fabian");
    }

    @Test
    @DisplayName("BL-02: findeAlleSortiert — offen vor erledigt, HOCH vor MITTEL")
    void sortierungOffenVorErledigt() {
        BacklogItem erledigtMittel = neuesItem("erledigt", BacklogStatus.ERLEDIGT, BacklogPrioritaet.MITTEL);
        BacklogItem offenMittel = neuesItem("offen-m", BacklogStatus.OFFEN, BacklogPrioritaet.MITTEL);
        BacklogItem offenHoch = neuesItem("offen-h", BacklogStatus.OFFEN, BacklogPrioritaet.HOCH);
        BacklogItem inArbeit = neuesItem("in-arb", BacklogStatus.IN_ARBEIT, BacklogPrioritaet.NIEDRIG);
        when(repository.findAll()).thenReturn(List.of(erledigtMittel, offenMittel, offenHoch, inArbeit));

        List<BacklogItem> sortiert = service.findeAlleSortiert();

        assertThat(sortiert).extracting(BacklogItem::getTitel)
                .containsExactly("offen-h", "offen-m", "in-arb", "erledigt");
    }

    @Test
    @DisplayName("BL-03: aendereStatus auf ERLEDIGT setzt erledigtAm; OFFEN setzt zurück")
    void aendereStatusZeitstempel() {
        UUID id = UUID.randomUUID();
        BacklogItem item = neuesItem("x", BacklogStatus.IN_ARBEIT, BacklogPrioritaet.MITTEL);
        item.setId(id);
        when(repository.findById(id)).thenReturn(Optional.of(item));
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.aendereStatus(id, BacklogStatus.ERLEDIGT);
        assertThat(item.getStatus()).isEqualTo(BacklogStatus.ERLEDIGT);
        assertThat(item.getErledigtAm()).isNotNull();

        service.aendereStatus(id, BacklogStatus.OFFEN);
        assertThat(item.getStatus()).isEqualTo(BacklogStatus.OFFEN);
        assertThat(item.getErledigtAm()).isNull();
    }

    @Test
    @DisplayName("BL-04: aendereStatus auf unbekannte ID wirft NotFoundException")
    void aendereStatusUnbekannt() {
        UUID id = UUID.randomUUID();
        when(repository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.aendereStatus(id, BacklogStatus.ERLEDIGT))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    @DisplayName("BL-05: zaehleOffen summiert OFFEN + IN_ARBEIT")
    void zaehleOffen() {
        when(repository.countByStatus(BacklogStatus.OFFEN)).thenReturn(3L);
        when(repository.countByStatus(BacklogStatus.IN_ARBEIT)).thenReturn(2L);

        assertThat(service.zaehleOffen()).isEqualTo(5L);
    }

    @Test
    @DisplayName("BL-06: loesche bei unbekannter ID wirft NotFoundException")
    void loescheUnbekannt() {
        UUID id = UUID.randomUUID();
        when(repository.existsById(id)).thenReturn(false);

        assertThatThrownBy(() -> service.loesche(id))
                .isInstanceOf(NotFoundException.class);
        verify(repository, org.mockito.Mockito.never()).deleteById(any());
    }

    private static BacklogItem neuesItem(String titel, BacklogStatus status, BacklogPrioritaet prio) {
        BacklogItem item = new BacklogItem();
        item.setId(UUID.randomUUID());
        item.setTitel(titel);
        item.setStatus(status);
        item.setPrioritaet(prio);
        item.setErstelltAm(Instant.now());
        return item;
    }
}
