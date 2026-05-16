package ch.sponsorplatz.aufgabe;

import ch.sponsorplatz.benutzer.AppUser;
import ch.sponsorplatz.benutzer.AppUserRepository;
import ch.sponsorplatz.benutzer.PlatformRolle;
import ch.sponsorplatz.organisation.Mitgliedschaft;
import ch.sponsorplatz.organisation.MitgliedschaftRepository;
import ch.sponsorplatz.organisation.Branche;
import ch.sponsorplatz.organisation.OrgStatus;
import ch.sponsorplatz.organisation.OrgTyp;
import ch.sponsorplatz.organisation.Organisation;
import ch.sponsorplatz.organisation.OrganisationRepository;
import ch.sponsorplatz.organisation.Rolle;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Real-Data-Integration: Persistierte Aufgabe → Sidebar-Badge auf einer
 * gerenderten Seite. Kein Service-Mock, voller Stack H2 + JPA + Thymeleaf.
 * Verifiziert AUFG-BADGE-INT-04..05.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@ActiveProfiles("dev")
@Transactional
class AufgabenBadgeAdviceRealDataTest {

    private static final java.util.UUID DEF_VEREIN_FREIGEBEN =
            java.util.UUID.fromString("aaaaaaa1-0000-0000-0000-000000000001");
    private static final java.util.UUID DEF_ANFRAGE_BEARBEITEN =
            java.util.UUID.fromString("aaaaaaa1-0000-0000-0000-000000000002");

    @Autowired private WebApplicationContext context;
    @Autowired private AppUserRepository appUserRepository;
    @Autowired private OrganisationRepository organisationRepository;
    @Autowired private MitgliedschaftRepository mitgliedschaftRepository;
    @Autowired private AufgabeRepository aufgabeRepository;
    @Autowired private AufgabenDefinitionRepository definitionRepository;

    private MockMvc mockMvc;

    @org.junit.jupiter.api.BeforeEach
    void setUp() {
        this.mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(springSecurity())
                .build();
    }

    @AfterEach
    void cleanup() {
        aufgabeRepository.deleteAll();
        mitgliedschaftRepository.deleteAll();
        organisationRepository.deleteAll();
        appUserRepository.deleteAll();
    }

    /** AUFG-BADGE-INT-04: PLATFORM_ADMIN mit einer offenen Admin-only Aufgabe → Badge "1". */
    @Test
    @WithMockUser("admin@badge.test")
    @DisplayName("AUFG-BADGE-INT-04: Admin mit einer offenen Aufgabe → Badge 1 im HTML")
    void adminMitEinerOffenenAdminAufgabe() throws Exception {
        neuerUser("admin@badge.test", PlatformRolle.PLATFORM_ADMIN);
        Organisation pendingOrg = neueOrg("pending-org", OrgStatus.PENDING);
        neueAdminAufgabe(pendingOrg, "Verein verifizieren");

        mockMvc.perform(get("/aufgaben"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("badgeAufgaben", 1L))
                .andExpect(content().string(containsString(">1<")));
    }

    /** AUFG-BADGE-INT-05: User mit Org-Mitgliedschaft + Org-Aufgabe → Badge "1". */
    @Test
    @WithMockUser("editor@badge.test")
    @DisplayName("AUFG-BADGE-INT-05: Org-Editor mit Org-Aufgabe → Badge 1 im HTML")
    void editorMitOrgAufgabe() throws Exception {
        AppUser editor = neuerUser("editor@badge.test", null);
        Organisation org = neueOrg("editor-org", OrgStatus.ACTIVE);
        mitgliedschaft(editor, org, Rolle.ORG_EDITOR);
        neueOrgAufgabe(org, "Anfrage bearbeiten");

        mockMvc.perform(get("/aufgaben"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("badgeAufgaben", 1L))
                .andExpect(content().string(containsString(">1<")));
    }

    // --- Helpers ---

    private AppUser neuerUser(String email, PlatformRolle rolle) {
        AppUser u = new AppUser();
        u.setEmail(email);
        u.setAnzeigename(email);
        u.setPasswortHash("$2a$10$dummy");
        u.setEmailVerifiziert(true);
        u.setAktiv(true);
        if (rolle != null) u.setPlatformRolle(rolle);
        return appUserRepository.save(u);
    }

    private Organisation neueOrg(String slug, OrgStatus status) {
        Organisation o = new Organisation();
        o.setName(slug);
        o.setSlug(slug);
        o.setTyp(OrgTyp.VEREIN);
        o.setBranche(Branche.SPORT);
        o.setStatus(status);
        return organisationRepository.save(o);
    }

    private void mitgliedschaft(AppUser user, Organisation org, Rolle rolle) {
        Mitgliedschaft m = new Mitgliedschaft();
        m.setUser(user);
        m.setOrg(org);
        m.setRolle(rolle);
        mitgliedschaftRepository.save(m);
    }

    private void neueAdminAufgabe(Organisation entity, String titel) {
        Aufgabe a = new Aufgabe();
        a.setDefinition(definitionRepository.findById(DEF_VEREIN_FREIGEBEN).orElseThrow());
        a.setEntityTyp(TriggerEntityTyp.ORG);
        a.setEntityId(entity.getId());
        a.setTitel(titel);
        a.setLink("/admin/orgs/" + entity.getSlug());
        a.setNurPlatformAdmin(true);
        a.setStatus(AufgabenStatus.OFFEN);
        aufgabeRepository.save(a);
    }

    private void neueOrgAufgabe(Organisation assignee, String titel) {
        Aufgabe a = new Aufgabe();
        a.setDefinition(definitionRepository.findById(DEF_ANFRAGE_BEARBEITEN).orElseThrow());
        a.setEntityTyp(TriggerEntityTyp.ANFRAGE);
        a.setEntityId(java.util.UUID.randomUUID());
        a.setTitel(titel);
        a.setLink("/anfragen");
        a.setAssigneeOrg(assignee);
        a.setStatus(AufgabenStatus.OFFEN);
        aufgabeRepository.save(a);
    }
}
