package ch.sponsorplatz.organisation;
import ch.sponsorplatz.benutzer.AppUserRepository;

import ch.sponsorplatz.benutzer.AppUser;
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

    /** MG-03: findSponsorOrgSlugs liefert nur Firmen (UNTERNEHMEN) mit Edit-Rolle, sortiert. */
    @Test
    void findSponsorOrgSlugsNurFirmaMitEditRolle() {
        AppUser user = erstelleUser("crm-user@example.com", "CRM User");
        Organisation firmaZeta = erstelleFirma("Zeta AG", "zeta-ag");
        Organisation firmaAlpha = erstelleFirma("Alpha AG", "alpha-ag");
        Organisation firmaViewer = erstelleFirma("Viewer AG", "viewer-ag");
        Organisation verein = erstelleOrg("FC Edit", "fc-edit");

        mitgliedschaftRepository.saveAndFlush(neueMitgliedschaft(user, firmaZeta, Rolle.ORG_OWNER));
        mitgliedschaftRepository.saveAndFlush(neueMitgliedschaft(user, firmaAlpha, Rolle.ORG_EDITOR));
        mitgliedschaftRepository.saveAndFlush(neueMitgliedschaft(user, firmaViewer, Rolle.ORG_VIEWER));
        mitgliedschaftRepository.saveAndFlush(neueMitgliedschaft(user, verein, Rolle.ORG_OWNER));

        var slugs = mitgliedschaftRepository.findSponsorOrgSlugs(
                "crm-user@example.com", Set.of(Rolle.ORG_OWNER, Rolle.ORG_EDITOR), OrgTyp.UNTERNEHMEN);

        // Viewer-Firma + Verein fallen raus; alphabetisch nach Name sortiert.
        assertThat(slugs).containsExactly("alpha-ag", "zeta-ag");
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
        org.setBranche(Branche.SPORT);
        return organisationRepository.saveAndFlush(org);
    }

    private Organisation erstelleFirma(String name, String slug) {
        Organisation org = new Organisation();
        org.setName(name);
        org.setSlug(slug);
        org.setTyp(OrgTyp.UNTERNEHMEN);
        org.setSponsorBranche(SponsorBranche.VERSICHERUNG);
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

