package ch.sponsorplatz.projekt;

import ch.sponsorplatz.organisation.Organisation;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class EventViewTest {

    @Test
    @DisplayName("EVT-05: EventView.von mappt korrekt — kein Org-Entity im View")
    void vonMapptKorrekt() {
        Organisation org = new Organisation();
        org.setName("FC Test");
        org.setSlug("fc-test");

        Event event = new Event();
        event.setId(UUID.randomUUID());
        event.setName("Sommerfest");
        event.setSlug("sommerfest");
        event.setBeschreibung("Tolles Fest");
        event.setOrt("Bern");
        event.setDatum(LocalDate.of(2026, 7, 15));
        event.setDatumEnde(LocalDate.of(2026, 7, 16));
        event.setKapazitaet(200);
        event.setOrg(org);

        EventView view = EventView.von(event);
        assertThat(view.name()).isEqualTo("Sommerfest");
        assertThat(view.slug()).isEqualTo("sommerfest");
        assertThat(view.orgName()).isEqualTo("FC Test");
        assertThat(view.orgSlug()).isEqualTo("fc-test");
        assertThat(view.datum()).isEqualTo(LocalDate.of(2026, 7, 15));
        assertThat(view.kapazitaet()).isEqualTo(200);
    }

    @Test
    @DisplayName("EVT-05b: EventView.von(List) mappt Liste")
    void vonListeMappt() {
        Organisation org = new Organisation();
        org.setName("Verein");
        org.setSlug("verein");

        Event e1 = new Event();
        e1.setName("A");
        e1.setSlug("a");
        e1.setDatum(LocalDate.now());
        e1.setOrg(org);

        Event e2 = new Event();
        e2.setName("B");
        e2.setSlug("b");
        e2.setDatum(LocalDate.now());
        e2.setOrg(org);

        List<EventView> views = EventView.von(List.of(e1, e2));
        assertThat(views).hasSize(2);
    }
}

