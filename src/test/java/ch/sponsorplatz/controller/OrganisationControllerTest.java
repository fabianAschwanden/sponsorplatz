package ch.sponsorplatz.controller;

import ch.sponsorplatz.config.SecurityConfig;
import ch.sponsorplatz.model.OrgStatus;
import ch.sponsorplatz.model.OrgTyp;
import ch.sponsorplatz.model.Organisation;
import ch.sponsorplatz.service.AccessControl;
import ch.sponsorplatz.service.OrganisationService;
import ch.sponsorplatz.service.SponsorplatzUserDetailsService;
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

    /** ORG-09: POST /organisationen/speichern → Redirect. */
    @Test
    @WithMockUser
    void speichernRedirected() throws Exception {
        Organisation gespeichert = testOrg();
        when(service.speichere(any())).thenReturn(gespeichert);

        mockMvc.perform(post("/organisationen/speichern")
                .param("typ", "VEREIN")
                .param("name", "Test-Verein")
                .with(csrf()))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrlPattern("/organisationen/*"));
    }

    /** ORG-10: GET /organisationen/{slug} → 200 + Detail. */
    @Test
    void detailWirdAngezeigt() throws Exception {
        Organisation org = testOrg();
        when(service.findeNachSlug("fc-test")).thenReturn(Optional.of(org));

        mockMvc.perform(get("/organisationen/fc-test"))
            .andExpect(status().isOk())
            .andExpect(view().name("organisation-detail"))
            .andExpect(model().attribute("org", org));
    }

    /** ORG-16: GET /organisationen/{slug} mit unbekanntem Slug → 404 (nicht 400). */
    @Test
    void detailFuerUnbekanntenSlugIst404() throws Exception {
        when(service.findeNachSlug("unbekannt")).thenReturn(Optional.empty());

        mockMvc.perform(get("/organisationen/unbekannt"))
            .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser
    void neuesFormularWirdAngezeigt() throws Exception {
        mockMvc.perform(get("/organisationen/neu"))
            .andExpect(status().isOk())
            .andExpect(view().name("organisation-form"))
            .andExpect(model().attributeExists("orgForm"))
            .andExpect(model().attributeExists("typen"));
    }

    @Test
    @WithMockUser
    void speichernMitFehlerZeigtFormularErneut() throws Exception {
        mockMvc.perform(post("/organisationen/speichern")
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
        org.setStatus(OrgStatus.PENDING);
        org.setRegistriertAm(Instant.now());
        return org;
    }
}
