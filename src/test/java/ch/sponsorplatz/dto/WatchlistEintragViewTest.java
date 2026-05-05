package ch.sponsorplatz.dto;

import ch.sponsorplatz.model.AppUser;
import ch.sponsorplatz.model.OrgTyp;
import ch.sponsorplatz.model.Organisation;
import ch.sponsorplatz.model.Projekt;
import ch.sponsorplatz.model.Sichtbarkeit;
import ch.sponsorplatz.model.WatchlistEintrag;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class WatchlistEintragViewTest {

    /** VIEW-07: WatchlistEintragView mappt inkl. nested ProjektView. */
    @Test
    void mappingEinesEintrags() {
        Organisation org = new Organisation();
        org.setId(UUID.randomUUID());
        org.setName("FC Test");
        org.setSlug("fc-test");
        org.setTyp(OrgTyp.VEREIN);

        UUID projektId = UUID.randomUUID();
        Projekt projekt = new Projekt();
        projekt.setId(projektId);
        projekt.setName("Sommerfest");
        projekt.setSlug("sommerfest");
        projekt.setKategorie("SPORT");
        projekt.setOrt("Zürich");
        projekt.setSichtbarkeit(Sichtbarkeit.OEFFENTLICH);
        projekt.setOrg(org);

        AppUser user = new AppUser();
        user.setId(UUID.randomUUID());

        UUID eintragId = UUID.randomUUID();
        WatchlistEintrag e = new WatchlistEintrag();
        e.setId(eintragId);
        e.setUser(user);
        e.setProjekt(projekt);

        WatchlistEintragView view = WatchlistEintragView.von(e);

        assertThat(view.id()).isEqualTo(eintragId);
        assertThat(view.projekt()).isNotNull();
        assertThat(view.projekt().id()).isEqualTo(projektId);
        assertThat(view.projekt().slug()).isEqualTo("sommerfest");
        assertThat(view.projekt().name()).isEqualTo("Sommerfest");
        assertThat(view.projekt().kategorie()).isEqualTo("SPORT");
    }
}
