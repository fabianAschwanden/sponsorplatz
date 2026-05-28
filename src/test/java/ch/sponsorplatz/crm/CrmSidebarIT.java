package ch.sponsorplatz.crm;

import ch.sponsorplatz.benutzer.AppUser;
import ch.sponsorplatz.benutzer.AppUserRepository;
import ch.sponsorplatz.organisation.Branche;
import ch.sponsorplatz.organisation.Mitgliedschaft;
import ch.sponsorplatz.organisation.MitgliedschaftRepository;
import ch.sponsorplatz.organisation.OrgStatus;
import ch.sponsorplatz.organisation.OrgTyp;
import ch.sponsorplatz.organisation.Organisation;
import ch.sponsorplatz.organisation.OrganisationRepository;
import ch.sponsorplatz.organisation.Rolle;
import ch.sponsorplatz.organisation.SponsorBranche;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;

/**
 * CRM-NAV-IT-01..02 — End-to-End-Beweis, dass der CRM-Einstieg in der Sidebar
 * (CrmZugangAdvice → Fragment) nur für Mitglieder einer Firma mit
 * Bearbeitungsrechten erscheint, gegen den vollen Stack (echte DB + Security +
 * Thymeleaf). Geprüft auf einer neutralen Auth-Seite (`/dashboard`), nicht der
 * CRM-Seite selbst — die Sidebar ist global.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@ActiveProfiles("dev")
@Transactional
class CrmSidebarIT {

    @Autowired private MockMvc mockMvc;
    @Autowired private OrganisationRepository organisationRepository;
    @Autowired private AppUserRepository appUserRepository;
    @Autowired private MitgliedschaftRepository mitgliedschaftRepository;

    /** CRM-NAV-IT-01: Firmen-Editor sieht den CRM-Link auf /dashboard. */
    @Test
    @DisplayName("CRM-NAV-IT-01: Firmen-Editor sieht CRM in der Sidebar")
    void firmaEditorSiehtCrmLink() throws Exception {
        Organisation firma = firma("Acme Versicherung", "acme-versicherung");
        AppUser editor = erstelleUser("editor@acme.ch");
        mitglied(editor, firma, Rolle.ORG_EDITOR);

        mockMvc.perform(get("/dashboard").with(user("editor@acme.ch")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("/crm/acme-versicherung")));
    }

    /** CRM-NAV-IT-02: Verein-Owner (keine Firma) sieht keinen CRM-Link. */
    @Test
    @DisplayName("CRM-NAV-IT-02: Verein-Owner sieht KEINEN CRM-Link")
    void vereinOwnerSiehtKeinenCrmLink() throws Exception {
        Organisation verein = verein("FC Ohne CRM", "fc-ohne-crm");
        AppUser owner = erstelleUser("owner@fc.ch");
        mitglied(owner, verein, Rolle.ORG_OWNER);

        mockMvc.perform(get("/dashboard").with(user("owner@fc.ch")))
                .andExpect(status().isOk())
                .andExpect(content().string(not(containsString("href=\"/crm/"))));
    }

    private Organisation firma(String name, String slug) {
        Organisation o = new Organisation();
        o.setName(name);
        o.setSlug(slug);
        o.setTyp(OrgTyp.UNTERNEHMEN);
        o.setSponsorBranche(SponsorBranche.VERSICHERUNG);
        o.setStatus(OrgStatus.ACTIVE);
        o.setRegistriertAm(Instant.now());
        return organisationRepository.save(o);
    }

    private Organisation verein(String name, String slug) {
        Organisation o = new Organisation();
        o.setName(name);
        o.setSlug(slug);
        o.setTyp(OrgTyp.VEREIN);
        o.setBranche(Branche.SPORT);
        o.setStatus(OrgStatus.ACTIVE);
        o.setRegistriertAm(Instant.now());
        return organisationRepository.save(o);
    }

    private AppUser erstelleUser(String email) {
        AppUser u = new AppUser();
        u.setEmail(email);
        u.setAnzeigename(email);
        u.setPasswortHash("$2a$test");
        u.setAktiv(true);
        u.setEmailVerifiziert(true);
        return appUserRepository.save(u);
    }

    private void mitglied(AppUser user, Organisation org, Rolle rolle) {
        Mitgliedschaft m = new Mitgliedschaft();
        m.setUser(user);
        m.setOrg(org);
        m.setRolle(rolle);
        mitgliedschaftRepository.save(m);
    }
}
