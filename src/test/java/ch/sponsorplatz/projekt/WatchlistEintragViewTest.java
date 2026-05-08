package ch.sponsorplatz.projekt;

import ch.sponsorplatz.benutzer.AppUser;
import ch.sponsorplatz.organisation.OrgTyp;
import ch.sponsorplatz.organisation.Organisation;
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
