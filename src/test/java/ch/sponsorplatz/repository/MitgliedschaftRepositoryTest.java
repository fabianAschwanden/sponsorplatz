package ch.sponsorplatz.repository;

import ch.sponsorplatz.model.AppUser;
import ch.sponsorplatz.model.Mitgliedschaft;
import ch.sponsorplatz.model.OrgTyp;
import ch.sponsorplatz.model.Organisation;
import ch.sponsorplatz.model.Rolle;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("dev")
class MitgliedschaftRepositoryTest {

    @Autowired
    private MitgliedschaftRepository mitgliedschaftRepository;

    @Autowired
    private AppUserRepository appUserRepository;

    @Autowired
    private OrganisationRepository organisationRepository;

    /** MG-01: UNIQUE (user_id, org_id) — Duplikat wirft Exception. */
    @Test
    void userOrgKombinationIstEindeutig() {
        AppUser user = erstelleUser("user1@example.com", "User 1");
        Organisation org = erstelleOrg("Test-Org", "test-org");

        mitgliedschaftRepository.saveAndFlush(neueMitgliedschaft(user, org, Rolle.ORG_EDITOR));

        Mitgliedschaft zweite = neueMitgliedschaft(user, org, Rolle.ORG_VIEWER);

        assertThatThrownBy(() -> mitgliedschaftRepository.saveAndFlush(zweite))
                .isInstanceOf(Exception.class);
    }

    /** MG-02: existsByUserIdAndOrgIdAndRolleIn findet korrekte Treffer. */
    @Test
    void existsByUserUndOrgUndRolleIn() {
        AppUser user = erstelleUser("editor@example.com", "Editor");
        Organisation org = erstelleOrg("Org-A", "org-a");
        Organisation andereOrg = erstelleOrg("Org-B", "org-b");

        mitgliedschaftRepository.saveAndFlush(neueMitgliedschaft(user, org, Rolle.ORG_EDITOR));

        assertThat(mitgliedschaftRepository.existsByUserIdAndOrgIdAndRolleIn(
                user.getId(), org.getId(), Set.of(Rolle.ORG_EDITOR, Rolle.ORG_OWNER)))
                .isTrue();

        assertThat(mitgliedschaftRepository.existsByUserIdAndOrgIdAndRolleIn(
                user.getId(), org.getId(), Set.of(Rolle.ORG_OWNER)))
                .isFalse();

        assertThat(mitgliedschaftRepository.existsByUserIdAndOrgIdAndRolleIn(
                user.getId(), andereOrg.getId(), Set.of(Rolle.ORG_EDITOR)))
                .isFalse();
    }

    private AppUser erstelleUser(String email, String name) {
        AppUser user = new AppUser();
        user.setEmail(email);
        user.setAnzeigename(name);
        user.setPasswortHash("$2a$10$dummyhash");
        return appUserRepository.saveAndFlush(user);
    }

    private Organisation erstelleOrg(String name, String slug) {
        Organisation org = new Organisation();
        org.setName(name);
        org.setSlug(slug);
        org.setTyp(OrgTyp.VEREIN);
        return organisationRepository.saveAndFlush(org);
    }

    private Mitgliedschaft neueMitgliedschaft(AppUser user, Organisation org, Rolle rolle) {
        Mitgliedschaft m = new Mitgliedschaft();
        m.setUser(user);
        m.setOrg(org);
        m.setRolle(rolle);
        return m;
    }
}

