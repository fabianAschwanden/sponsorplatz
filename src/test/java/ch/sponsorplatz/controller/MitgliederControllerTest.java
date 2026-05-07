package ch.sponsorplatz.controller;

import ch.sponsorplatz.shared.config.SecurityConfig;
import ch.sponsorplatz.model.Organisation;
import ch.sponsorplatz.model.OrgTyp;
import ch.sponsorplatz.service.AccessControl;
import ch.sponsorplatz.service.AppUserService;
import ch.sponsorplatz.service.MitgliedschaftService;
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

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = MitgliederController.class)
@Import(SecurityConfig.class)
@ActiveProfiles("dev")
class MitgliederControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private OrganisationService organisationService;

    @MockBean
    private MitgliedschaftService mitgliedschaftService;

    @MockBean
    private AppUserService appUserService;

    @MockBean
    private SponsorplatzUserDetailsService userDetailsService;

    @MockBean
    private AccessControl accessControl;

    /** MGCTRL-01: GET /organisationen/{slug}/mitglieder anonym → Redirect zu /login. */
    @Test
    void mitgliederAnonymRedirectZuLogin() throws Exception {
        mockMvc.perform(get("/organisationen/fc-test/mitglieder"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrlPattern("**/login"));
    }

    /** MGCTRL-02: GET /organisationen/{slug}/mitglieder ohne Edit-Recht → 403. */
    @Test
    @WithMockUser
    void mitgliederOhneEditRechtIst403() throws Exception {
        when(accessControl.kannOrgEditierenNachSlug(eq("fc-test"), any())).thenReturn(false);

        mockMvc.perform(get("/organisationen/fc-test/mitglieder"))
            .andExpect(status().isForbidden());
    }

    /** MGCTRL-03: POST .../hinzufuegen ohne Verwalten-Recht → 403. */
    @Test
    @WithMockUser
    void hinzufuegenOhneVerwaltenRechtIst403() throws Exception {
        when(accessControl.kannOrgVerwaltenNachSlug(eq("fc-test"), any())).thenReturn(false);

        mockMvc.perform(post("/organisationen/fc-test/mitglieder/hinzufuegen")
                .with(csrf())
                .param("email", "user@example.com")
                .param("rolle", "ORG_EDITOR"))
            .andExpect(status().isForbidden());
    }

    /** MGCTRL-04: POST .../{id}/entfernen ohne Verwalten-Recht → 403. */
    @Test
    @WithMockUser
    void entfernenOhneVerwaltenRechtIst403() throws Exception {
        when(accessControl.kannOrgVerwaltenNachSlug(eq("fc-test"), any())).thenReturn(false);
        UUID mitgliedschaftId = UUID.randomUUID();

        mockMvc.perform(post("/organisationen/fc-test/mitglieder/" + mitgliedschaftId + "/entfernen")
                .with(csrf()))
            .andExpect(status().isForbidden());
    }

    /** MGCTRL-05: POST .../hinzufuegen mit Verwalten-Recht → 302 Redirect. */
    @Test
    @WithMockUser
    void hinzufuegenMitRechtRedirected() throws Exception {
        when(accessControl.kannOrgVerwaltenNachSlug(eq("fc-test"), any())).thenReturn(true);
        when(organisationService.findeNachSlug("fc-test")).thenReturn(Optional.of(testOrg()));
        when(appUserService.findeNachEmail("user@example.com")).thenReturn(Optional.empty());
        when(mitgliedschaftService.findeNachOrg(any())).thenReturn(List.of());

        mockMvc.perform(post("/organisationen/fc-test/mitglieder/hinzufuegen")
                .with(csrf())
                .param("email", "user@example.com")
                .param("rolle", "ORG_EDITOR"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/organisationen/fc-test/mitglieder"));
    }

    private Organisation testOrg() {
        Organisation org = new Organisation();
        org.setId(UUID.randomUUID());
        org.setName("FC Test");
        org.setSlug("fc-test");
        org.setTyp(OrgTyp.VEREIN);
        return org;
    }
}
