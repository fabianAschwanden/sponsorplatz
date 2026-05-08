package ch.sponsorplatz.projekt;

import ch.sponsorplatz.organisation.OrgTyp;
import ch.sponsorplatz.organisation.Organisation;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class ProjektViewTest {

    /** VIEW-03: von(entity) mappt inkl. nested OrganisationKurzView. */
    @Test
    void mappingMitNestedOrg() {
        UUID orgId = UUID.randomUUID();
        Organisation org = new Organisation();
        org.setId(orgId);
        org.setName("FC Test");
        org.setSlug("fc-test");
        org.setTyp(OrgTyp.VEREIN);

        UUID projektId = UUID.randomUUID();
        Projekt projekt = new Projekt();
        projekt.setId(projektId);
        projekt.setName("Sommerfest 2026");
        projekt.setSlug("sommerfest-2026");
        projekt.setSichtbarkeit(Sichtbarkeit.ENTWURF);
        projekt.setKategorie("SPORT");
        projekt.setOrt("Zürich");
        projekt.setStartDatum(LocalDate.of(2026, 6, 15));
        projekt.setEndDatum(LocalDate.of(2026, 6, 16));
        projekt.setBeschreibung("Tolles Fest");
        projekt.setOrg(org);

        ProjektView view = ProjektView.von(projekt);

        assertThat(view.id()).isEqualTo(projektId);
        assertThat(view.name()).isEqualTo("Sommerfest 2026");
        assertThat(view.slug()).isEqualTo("sommerfest-2026");
        assertThat(view.sichtbarkeit()).isEqualTo(Sichtbarkeit.ENTWURF);
        assertThat(view.kategorie()).isEqualTo("SPORT");
        assertThat(view.ort()).isEqualTo("Zürich");
        assertThat(view.startDatum()).isEqualTo(LocalDate.of(2026, 6, 15));
        assertThat(view.endDatum()).isEqualTo(LocalDate.of(2026, 6, 16));
        assertThat(view.beschreibung()).isEqualTo("Tolles Fest");
        assertThat(view.org()).isNotNull();
        assertThat(view.org().id()).isEqualTo(orgId);
        assertThat(view.org().name()).isEqualTo("FC Test");
        assertThat(view.org().slug()).isEqualTo("fc-test");
    }
}
