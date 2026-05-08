package ch.sponsorplatz.organisation;
import ch.sponsorplatz.shared.exception.GlobalExceptionHandler;

import ch.sponsorplatz.shared.config.SecurityConfig;
import ch.sponsorplatz.benutzer.SponsorplatzUserDetailsService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
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

    @MockBean
    private OrganisationService service;

    @MockBean
    private SponsorplatzUserDetailsService userDetailsService;

    @MockBean
    private AccessControl accessControl;

    /** ORG-08: GET /organisationen → 200 + Liste. */
    @Test
    void listeWirdAngezeigt() throws Exception {
        when(service.alle()).thenReturn(List.of(testOrg()));

        mockMvc.perform(get("/organisationen"))
            .andExpect(status().isOk())
            .andExpect(view().name("organisationen"))
            .andExpect(model().attributeExists("organisationen"));
    }

    /** ORG-17: POST /organisationen (Create) → Redirect auf Detail. */
    @Test
    @WithMockUser
    void erstellenRedirected() throws Exception {
        Organisation gespeichert = testOrg();
        when(service.erstelle(any())).thenReturn(gespeichert);

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
        when(service.aktualisiere(eq("fc-test"), any())).thenReturn(gespeichert);

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
    void detailWirdAngezeigt() throws Exception {
        Organisation org = testOrg();
        when(service.findeNachSlug("fc-test")).thenReturn(Optional.of(org));

        mockMvc.perform(get("/organisationen/fc-test"))
            .andExpect(status().isOk())
            .andExpect(view().name("organisation-detail"))
            .andExpect(model().attributeExists("org"));
    }

    /** ORG-16: GET /organisationen/{slug} mit unbekanntem Slug → 404 (nicht 400). */
    @Test
    void detailFuerUnbekanntenSlugIst404() throws Exception {
        when(service.findeNachSlug("unbekannt")).thenReturn(Optional.empty());

        mockMvc.perform(get("/organisationen/unbekannt"))
            .andExpect(status().isNotFound());
    }

    /** ORG-08a: GET /organisationen/neu ohne typ → Wizard-Schritt 1 (Typ-Auswahl). */
    @Test
    @WithMockUser
    void neuOhneTypZeigtTypAuswahl() throws Exception {
        mockMvc.perform(get("/organisationen/neu"))
            .andExpect(status().isOk())
            .andExpect(view().name("organisation-typ-waehlen"))
            .andExpect(model().attributeExists("typen"));
    }

    /** ORG-08b: GET /organisationen/neu?typ=VEREIN → Wizard-Schritt 2 (Form). */
    @Test
    @WithMockUser
    void neuMitTypZeigtFormular() throws Exception {
        mockMvc.perform(get("/organisationen/neu").param("typ", "VEREIN"))
            .andExpect(status().isOk())
            .andExpect(view().name("organisation-form"))
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
            .andExpect(view().name("organisation-form"));
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
        when(service.findeNachSlug("fc-test")).thenReturn(Optional.of(testOrg()));

        mockMvc.perform(get("/organisationen/fc-test/bearbeiten"))
            .andExpect(status().isOk())
            .andExpect(view().name("organisation-form"));
    }

    /** ORG-14: POST /organisationen/{slug}/loeschen ohne Verwalten-Recht → 403. */
    @Test
    @WithMockUser
    void loeschenOhneRechtIst403() throws Exception {
        when(accessControl.kannOrgVerwaltenNachSlug(eq("fc-test"), any())).thenReturn(false);

        mockMvc.perform(post("/organisationen/fc-test/loeschen").with(csrf()))
            .andExpect(status().isForbidden());
    }

    /** ORG-15: POST /organisationen/{slug}/loeschen mit Verwalten-Recht → 302 Redirect. */
    @Test
    @WithMockUser
    void loeschenMitRechtRedirected() throws Exception {
        when(accessControl.kannOrgVerwaltenNachSlug(eq("fc-test"), any())).thenReturn(true);
        when(service.findeNachSlug("fc-test")).thenReturn(Optional.of(testOrg()));

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
