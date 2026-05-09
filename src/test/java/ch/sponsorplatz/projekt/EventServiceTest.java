package ch.sponsorplatz.projekt;

import ch.sponsorplatz.organisation.Organisation;
import ch.sponsorplatz.organisation.OrganisationRepository;
import ch.sponsorplatz.shared.util.SlugGenerator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EventServiceTest {

    @Mock private EventRepository repository;
    @Mock private OrganisationRepository orgRepository;
    @Mock private SlugGenerator slugGenerator;
    @InjectMocks private EventService service;

    @Test
    @DisplayName("EVT-01: erstelle/aktualisiere/loesche — Basis-Lifecycle")
    void crudLifecycle() {
        UUID orgId = UUID.randomUUID();
        Organisation org = new Organisation();
        org.setId(orgId);
        org.setName("FC Test");
        when(orgRepository.findById(orgId)).thenReturn(Optional.of(org));
        when(slugGenerator.findeFreienSlug(any(), any())).thenReturn("sommerfest");
        when(repository.save(any(Event.class))).thenAnswer(inv -> {
            Event e = inv.getArgument(0);
            e.setId(UUID.randomUUID());
            return e;
        });

        // Erstelle
        Event created = service.erstelle(orgId, "Sommerfest", "Ein tolles Fest", "Bern",
                LocalDate.of(2026, 7, 15), LocalDate.of(2026, 7, 15), 200);
        assertThat(created.getName()).isEqualTo("Sommerfest");
        assertThat(created.getOrg()).isEqualTo(org);

        // Aktualisiere
        when(repository.findById(any())).thenReturn(Optional.of(created));
        service.aktualisiere(created.getId(), "Winterfest", null, "Luzern",
                LocalDate.of(2026, 12, 1), null, 100);
        assertThat(created.getName()).isEqualTo("Winterfest");

        // Loesche
        service.loesche(created.getId());
        verify(repository).delete(created);
    }

    @Test
    @DisplayName("EVT-04: findeKommendeNachOrgIds sortiert nach Datum aufsteigend")
    void findeKommendeNachOrgIds() {
        UUID orgId = UUID.randomUUID();
        Organisation org = new Organisation();
        org.setId(orgId);
        org.setName("Verein");
        org.setSlug("verein");

        Event frueh = new Event();
        frueh.setName("Frueh");
        frueh.setDatum(LocalDate.now().plusDays(5));
        frueh.setOrg(org);

        Event spaet = new Event();
        spaet.setName("Spaet");
        spaet.setDatum(LocalDate.now().plusDays(30));
        spaet.setOrg(org);

        when(repository.findByOrgIdInAndDatumGreaterThanEqualOrderByDatumAsc(any(), any()))
                .thenReturn(List.of(frueh, spaet));

        List<Event> result = service.findeKommendeNachOrgIds(List.of(orgId), 3);
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getName()).isEqualTo("Frueh");
    }

    @Test
    @DisplayName("EVT-01b: erstelle mit leerem Namen wirft IllegalArgumentException")
    void erstelleOhneNameWirft() {
        assertThatThrownBy(() -> service.erstelle(UUID.randomUUID(), "", null, null,
                LocalDate.now(), null, null))
                .isInstanceOf(IllegalArgumentException.class);
    }
}

