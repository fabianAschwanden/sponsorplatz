package ch.sponsorplatz.organisation;
import ch.sponsorplatz.shared.exception.GlobalExceptionHandler;

import ch.sponsorplatz.shared.config.SecurityConfig;
import ch.sponsorplatz.benutzer.SponsorplatzUserDetailsService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = OrganisationController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class})
@ActiveProfiles("dev")
class OrganisationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private OrganisationService service;

    @MockitoBean
    private SponsorplatzUserDetailsService userDetailsService;

    @MockitoBean
    private AccessControl accessControl;

    @MockitoBean
    private OrgHierarchieService hierarchieService;

    @MockitoBean
    private ch.sponsorplatz.benutzer.AppUserService appUserService;

    @MockitoBean
    private MitgliedschaftService mitgliedschaftService;

    @MockitoBean
    private ch.sponsorplatz.shared.medien.OrganisationLogoLookup logoLookup;

    /** ORG-08: GET /organisationen → 200 + Liste. */
    @Test
    @WithMockUser
    void listeWirdAngezeigt() throws Exception {
        when(service.alleViews()).thenReturn(List.of(OrganisationView.von(testOrg())));

        mockMvc.perform(get("/organisationen"))
            .andExpect(status().isOk())
            .andExpect(view().name("organisation/organisationen"))
            .andExpect(model().attributeExists("organisationen"));
    }

    /** ORG-17: POST /organisationen (Create) → Redirect auf Detail + automatische ORG_OWNER-Mitgliedschaft. */
    @Test
    @WithMockUser("verein@test.ch")
    void erstellenRedirected() throws Exception {
        Organisation gespeichert = testOrg();
        ch.sponsorplatz.benutzer.AppUser user = new ch.sponsorplatz.benutzer.AppUser();
        user.setId(UUID.randomUUID());
        user.setEmail("verein@test.ch");
        when(appUserService.findeOptionalIdNachEmail("verein@test.ch")).thenReturn(Optional.of(user.getId()));
        when(service.erstelleMitEigentuemerAlsView(any(), eq(user.getId()))).thenReturn(OrganisationView.von(gespeichert));

        mockMvc.perform(post("/organisationen")
                .param("typ", "VEREIN")
                .param("name", "Test-Verein")
                .param("branche", "SPORT")
                .with(csrf()))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrlPattern("/organisationen/*"));
    }

    /** ORG-18: POST /organisationen/{slug} (Update) mit Edit-Recht → Redirect. */
    @Test
    @WithMockUser
    void aktualisierenMitRechtRedirected() throws Exception {
        when(accessControl.kannOrgEditierenNachSlug(eq("fc-test"), any())).thenReturn(true);
        Organisation gespeichert = testOrg();
        when(service.aktualisiereAlsView(eq("fc-test"), any())).thenReturn(OrganisationView.von(gespeichert));

        mockMvc.perform(post("/organisationen/fc-test")
                .param("typ", "VEREIN")
                .param("name", "Test-Verein-Neu")
                .param("branche", "SPORT")
                .with(csrf()))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrlPattern("/organisationen/*"));
    }

    /** ORG-19: POST /organisationen/{slug} ohne Edit-Recht → 403. */
    @Test
    @WithMockUser
    void aktualisierenOhneRechtIst403() throws Exception {
        when(accessControl.kannOrgEditierenNachSlug(eq("fc-test"), any())).thenReturn(false);

        mockMvc.perform(post("/organisationen/fc-test")
                .param("typ", "VEREIN")
                .param("name", "Hijack")
                .with(csrf()))
            .andExpect(status().isForbidden());
    }

    /** ORG-10: GET /organisationen/{slug} → 200 + Detail. */
    @Test
    @WithMockUser
    void detailWirdAngezeigt() throws Exception {
        Organisation org = testOrg();
        when(service.findeViewNachSlug("fc-test")).thenReturn(Optional.of(OrganisationView.von(org)));

        mockMvc.perform(get("/organisationen/fc-test"))
            .andExpect(status().isOk())
            .andExpect(view().name("organisation/organisation-detail"))
            .andExpect(model().attributeExists("org"));
    }

    /** ORG-16: GET /organisationen/{slug} mit unbekanntem Slug → 404 (nicht 400). */
    @Test
    @WithMockUser
    void detailFuerUnbekanntenSlugIst404() throws Exception {
        when(service.findeViewNachSlug("unbekannt")).thenReturn(Optional.empty());

        mockMvc.perform(get("/organisationen/unbekannt"))
            .andExpect(status().isNotFound());
    }

    /** ORG-08a: GET /organisationen/neu ohne typ → Wizard-Schritt 1 (Typ-Auswahl). */
    @Test
    @WithMockUser
    void neuOhneTypZeigtTypAuswahl() throws Exception {
        mockMvc.perform(get("/organisationen/neu"))
            .andExpect(status().isOk())
            .andExpect(view().name("organisation/organisation-typ-waehlen"))
            .andExpect(model().attributeExists("typen"));
    }

    /** ORG-08b: GET /organisationen/neu?typ=VEREIN → Wizard-Schritt 2 (Form). */
    @Test
    @WithMockUser
    void neuMitTypZeigtFormular() throws Exception {
        mockMvc.perform(get("/organisationen/neu").param("typ", "VEREIN"))
            .andExpect(status().isOk())
            .andExpect(view().name("organisation/organisation-form"))
            .andExpect(model().attributeExists("orgForm"))
            .andExpect(model().attributeExists("typen"))
            .andExpect(model().attributeExists("sponsorBranchen"));
    }

    @Test
    @WithMockUser
    void erstellenMitFehlerZeigtFormularErneut() throws Exception {
        mockMvc.perform(post("/organisationen")
                .param("name", "")
                .with(csrf()))
            .andExpect(status().isOk())
            .andExpect(view().name("organisation/organisation-form"));
    }

    /** ORG-12: GET /organisationen/{slug}/bearbeiten ohne Edit-Recht → 403. */
    @Test
    @WithMockUser
    void bearbeitenFormularOhneRechtIst403() throws Exception {
        when(accessControl.kannOrgEditierenNachSlug(eq("fc-test"), any())).thenReturn(false);

        mockMvc.perform(get("/organisationen/fc-test/bearbeiten"))
            .andExpect(status().isForbidden());
    }

    /** ORG-13: GET /organisationen/{slug}/bearbeiten mit Edit-Recht → 200. */
    @Test
    @WithMockUser
    void bearbeitenFormularMitRechtIst200() throws Exception {
        when(accessControl.kannOrgEditierenNachSlug(eq("fc-test"), any())).thenReturn(true);
        OrganisationFormDto form = new OrganisationFormDto();
        form.setTyp(OrgTyp.VEREIN);
        form.setName("Test-Verein");
        form.setBranche(Branche.SPORT);
        when(service.findeFormularNachSlug("fc-test")).thenReturn(form);

        mockMvc.perform(get("/organisationen/fc-test/bearbeiten"))
            .andExpect(status().isOk())
            .andExpect(view().name("organisation/organisation-form"));
    }

    /**
     * ORG-24: Render-Assert — VEREIN-Bearbeiten zeigt das Branche-Select sichtbar im HTML.
     * Defense gegen den SpEL-Enum-Vergleich-Bug (*{typ} == 'VEREIN' liefert false,
     * weil typ ein Enum ist; muss *{typ?.name()} == 'VEREIN' sein).
     */
    @Test
    @WithMockUser
    void editFormZeigtBrancheSelectFuerVerein() throws Exception {
        when(accessControl.kannOrgEditierenNachSlug(eq("fc-test"), any())).thenReturn(true);
        OrganisationFormDto form = new OrganisationFormDto();
        form.setTyp(OrgTyp.VEREIN);
        form.setName("Test-Verein");
        form.setBranche(Branche.SPORT);
        when(service.findeFormularNachSlug("fc-test")).thenReturn(form);

        mockMvc.perform(get("/organisationen/fc-test/bearbeiten"))
            .andExpect(status().isOk())
            .andExpect(content().string(org.hamcrest.Matchers.containsString("id=\"branche\"")));
    }

    /**
     * ORG-24b: Render-Assert — UNTERNEHMEN-Bearbeiten zeigt das Industrie-Select.
     * War vor dem typ?.name()-Fix komplett unsichtbar.
     */
    @Test
    @WithMockUser
    void editFormZeigtIndustrieSelectFuerUnternehmen() throws Exception {
        when(accessControl.kannOrgEditierenNachSlug(eq("css"), any())).thenReturn(true);
        OrganisationFormDto form = new OrganisationFormDto();
        form.setTyp(OrgTyp.UNTERNEHMEN);
        form.setName("CSS Versicherung");
        form.setSponsorBranche(SponsorBranche.VERSICHERUNG);
        when(service.findeFormularNachSlug("css")).thenReturn(form);

        mockMvc.perform(get("/organisationen/css/bearbeiten"))
            .andExpect(status().isOk())
            .andExpect(content().string(org.hamcrest.Matchers.containsString("id=\"sponsorBranche\"")));
    }

    /**
     * ORG-25: Render-Assert — Hierarchie-Select ist im Edit-Form sichtbar.
     * Vor dem Fix gab es gar kein Form-Field für die Eltern-Org.
     */
    @Test
    @WithMockUser
    void editFormZeigtHierarchieSelect() throws Exception {
        when(accessControl.kannOrgEditierenNachSlug(eq("fc-test"), any())).thenReturn(true);
        OrganisationFormDto form = new OrganisationFormDto();
        form.setTyp(OrgTyp.VEREIN);
        form.setName("Test-Verein");
        form.setBranche(Branche.SPORT);
        when(service.findeFormularNachSlug("fc-test")).thenReturn(form);

        mockMvc.perform(get("/organisationen/fc-test/bearbeiten"))
            .andExpect(status().isOk())
            .andExpect(content().string(org.hamcrest.Matchers.containsString("id=\"uebergeordneteOrgId\"")));
    }

    /** ORG-14: POST /organisationen/{slug}/loeschen ohne Verwalten-Recht → 403. */
    @Test
    @WithMockUser
    void loeschenOhneRechtIst403() throws Exception {
        when(accessControl.kannOrgVerwaltenNachSlug(eq("fc-test"), any())).thenReturn(false);

        mockMvc.perform(post("/organisationen/fc-test/loeschen").with(csrf()))
            .andExpect(status().isForbidden());
    }

    /**
     * ORG-26: Render-Assert — Org mit Eltern + Sub-Orgs zeigt die Hierarchie-Sektion
     * mit "diese Org"-Marker und allen Sub-Org-Links im HTML. Der Tree wird durch
     * elternkette + untergeordneteOrgs aus dem Model gespeist; CSS-Klassen
     * h-knoten / h-aktiv sind sichtbar im DOM.
     */
    @Test
    @WithMockUser
    void hierarchieTreeRendert() throws Exception {
        Organisation tochter = testOrg();
        tochter.setName("Tochter GmbH");
        tochter.setSlug("tochter");
        tochter.setTyp(OrgTyp.UNTERNEHMEN);
        tochter.setSponsorBranche(SponsorBranche.VERSICHERUNG);

        Organisation sub = new Organisation();
        sub.setId(UUID.randomUUID());
        sub.setName("Sub-Verein");
        sub.setSlug("sub-verein");
        sub.setTyp(OrgTyp.VEREIN);
        sub.setBranche(Branche.SPORT);
        sub.setStatus(OrgStatus.VERIFIED);
        sub.setRegistriertAm(Instant.now());

        when(service.findeViewNachSlug("tochter")).thenReturn(Optional.of(OrganisationView.von(tochter)));
        when(service.findeUntergeordneteViews(tochter.getId())).thenReturn(List.of(OrganisationView.von(sub)));
        when(hierarchieService.findeElternketteNachSlug("tochter")).thenReturn(List.of(
                new OrgHierarchieService.BrotkrumenEintrag("Konzern AG", "konzern-ag"),
                new OrgHierarchieService.BrotkrumenEintrag("Tochter GmbH", "tochter")
        ));

        mockMvc.perform(get("/organisationen/tochter"))
            .andExpect(status().isOk())
            .andExpect(content().string(org.hamcrest.Matchers.containsString("hierarchie-tree")))
            .andExpect(content().string(org.hamcrest.Matchers.containsString("h-aktiv")))
            .andExpect(content().string(org.hamcrest.Matchers.containsString("Konzern AG")))
            .andExpect(content().string(org.hamcrest.Matchers.containsString("(diese Org)")))
            .andExpect(content().string(org.hamcrest.Matchers.containsString("/organisationen/sub-verein")));
    }

    /** ORG-15: POST /organisationen/{slug}/loeschen mit Verwalten-Recht → 302 Redirect. */
    @Test
    @WithMockUser
    void loeschenMitRechtRedirected() throws Exception {
        when(accessControl.kannOrgVerwaltenNachSlug(eq("fc-test"), any())).thenReturn(true);
        when(service.loescheNachSlug("fc-test")).thenReturn("Test-Verein");

        mockMvc.perform(post("/organisationen/fc-test/loeschen").with(csrf()))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/organisationen"));
    }

    private Organisation testOrg() {
        Organisation org = new Organisation();
        org.setId(UUID.randomUUID());
        org.setName("Test-Verein");
        org.setSlug("fc-test");
        org.setTyp(OrgTyp.VEREIN);
        org.setBranche(Branche.SPORT);
        org.setStatus(OrgStatus.PENDING);
        org.setRegistriertAm(Instant.now());
        return org;
    }
}
