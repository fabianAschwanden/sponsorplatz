package ch.sponsorplatz.organisation;

import ch.sponsorplatz.benutzer.AppUser;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * AC-HIER-CTE: Verifiziert dass die rekursive CTE-Query
 * {@link MitgliedschaftRepository#zaehleMitgliedschaftenInHierarchie} gegen
 * eine ECHTE Datenbank korrekt arbeitet — Service-Mocks haben das verschluckt.
 *
 * <p>Bug-Reproduktion: nach Umstellung von iterativem Loop auf CTE liefert
 * der Query auf prod 0 obwohl Mitgliedschaft existiert → 403 beim Bearbeiten.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("dev")
class MitgliedschaftHierarchieQueryTest {

    @Autowired private MitgliedschaftRepository mitgliedschaftRepository;
    @Autowired private TestEntityManager em;

    @Test
    @DisplayName("AC-HIER-CTE-01: Direkte Mitgliedschaft auf Org → CTE liefert > 0")
    void direkteMitgliedschaft() {
        AppUser user = persistUser("owner@test.ch");
        Organisation org = persistVerein("FC Test", "fc-test");
        persistMitgliedschaft(user, org, Rolle.ORG_OWNER);

        long count = mitgliedschaftRepository.zaehleMitgliedschaftenInHierarchie(
                user.getId(), org.getId(), Set.of("ORG_OWNER", "ORG_EDITOR"));

        assertThat(count).isPositive();
    }

    @Test
    @DisplayName("AC-HIER-CTE-02: Owner auf Eltern-Org → CTE findet via Hierarchie")
    void elternOwnerFindet() {
        AppUser user = persistUser("eltern-owner@test.ch");
        Organisation eltern = persistUnternehmen("Konzern AG", "konzern-ag");
        Organisation kind = persistUnternehmen("Tochter GmbH", "tochter-gmbh");
        kind.setUebergeordneteOrg(eltern);
        em.persistAndFlush(kind);
        persistMitgliedschaft(user, eltern, Rolle.ORG_OWNER);

        long count = mitgliedschaftRepository.zaehleMitgliedschaftenInHierarchie(
                user.getId(), kind.getId(), Set.of("ORG_OWNER", "ORG_EDITOR"));

        assertThat(count).isPositive();
    }

    @Test
    @DisplayName("AC-HIER-CTE-03: Keine Mitgliedschaft → 0")
    void keineMitgliedschaft() {
        AppUser user = persistUser("fremd@test.ch");
        Organisation org = persistVerein("Andere", "andere");

        long count = mitgliedschaftRepository.zaehleMitgliedschaftenInHierarchie(
                user.getId(), org.getId(), Set.of("ORG_OWNER", "ORG_EDITOR"));

        assertThat(count).isZero();
    }

    private AppUser persistUser(String email) {
        AppUser u = new AppUser();
        u.setEmail(email);
        u.setAnzeigename(email);
        u.setPasswortHash("$2a$test");
        return em.persistAndFlush(u);
    }

    private Organisation persistVerein(String name, String slug) {
        Organisation o = new Organisation();
        o.setName(name);
        o.setSlug(slug);
        o.setTyp(OrgTyp.VEREIN);
        o.setStatus(OrgStatus.VERIFIED);
        o.setBranche(Branche.SPORT);
        return em.persistAndFlush(o);
    }

    private Organisation persistUnternehmen(String name, String slug) {
        Organisation o = new Organisation();
        o.setName(name);
        o.setSlug(slug);
        o.setTyp(OrgTyp.UNTERNEHMEN);
        o.setStatus(OrgStatus.VERIFIED);
        o.setSponsorBranche(SponsorBranche.ANDERE);
        return em.persistAndFlush(o);
    }

    private void persistMitgliedschaft(AppUser user, Organisation org, Rolle rolle) {
        Mitgliedschaft m = new Mitgliedschaft();
        m.setUser(user);
        m.setOrg(org);
        m.setRolle(rolle);
        em.persistAndFlush(m);
    }
}
