package ch.sponsorplatz.controller;
import ch.sponsorplatz.shared.exception.GlobalExceptionHandler;

import ch.sponsorplatz.shared.config.SecurityConfig;
import ch.sponsorplatz.organisation.Organisation;
import ch.sponsorplatz.organisation.OrgTyp;
import ch.sponsorplatz.organisation.AccessControl;
import ch.sponsorplatz.organisation.OrganisationService;
import ch.sponsorplatz.service.SponsoringAnfrageService;
import ch.sponsorplatz.benutzer.SponsorplatzUserDetailsService;
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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = AnfragenController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class})
@ActiveProfiles("dev")
class AnfragenControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private SponsoringAnfrageService anfrageService;

    @MockBean
    private OrganisationService orgService;

    @MockBean
    private AccessControl accessControl;

    @MockBean
    private SponsorplatzUserDetailsService userDetailsService;

    private Organisation testOrg() {
        Organisation org = new Organisation();
        org.setId(UUID.randomUUID());
        org.setName("FC Test");
        org.setSlug("fc-test");
        org.setTyp(OrgTyp.VEREIN);
        return org;
    }

    /** ANFCTRL-01: Anfragen-Liste mit Edit-Recht → 200. */
    @Test
    @WithMockUser
    void listeMitRechtIst200() throws Exception {
        Organisation org = testOrg();
        when(accessControl.kannOrgEditierenNachSlug(eq("fc-test"), any())).thenReturn(true);
        when(orgService.findeNachSlug("fc-test")).thenReturn(Optional.of(org));
        when(anfrageService.findeEingehende(org.getId())).thenReturn(List.of());

        mockMvc.perform(get("/organisationen/fc-test/anfragen"))
                .andExpect(status().isOk())
                .andExpect(view().name("anfragen-liste"))
                .andExpect(model().attributeExists("anfragen"));
    }

    /** ANFCTRL-02: Anfragen-Liste ohne Recht → 403. */
    @Test
    @WithMockUser
    void listeOhneRechtIst403() throws Exception {
        when(accessControl.kannOrgEditierenNachSlug(eq("fc-test"), any())).thenReturn(false);

        mockMvc.perform(get("/organisationen/fc-test/anfragen"))
                .andExpect(status().isForbidden());
    }

    /** ANFCTRL-03: Annehmen → Redirect. */
    @Test
    @WithMockUser
    void annehmenRedirected() throws Exception {
        when(accessControl.kannOrgEditierenNachSlug(eq("fc-test"), any())).thenReturn(true);
        UUID anfrageId = UUID.randomUUID();

        mockMvc.perform(post("/organisationen/fc-test/anfragen/" + anfrageId + "/annehmen")
                        .with(csrf())
                        .param("antwort", "Gerne!"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/organisationen/fc-test/anfragen"));
    }

    /** ANFCTRL-04: Ablehnen → Redirect. */
    @Test
    @WithMockUser
    void ablehnenRedirected() throws Exception {
        when(accessControl.kannOrgEditierenNachSlug(eq("fc-test"), any())).thenReturn(true);
        UUID anfrageId = UUID.randomUUID();

        mockMvc.perform(post("/organisationen/fc-test/anfragen/" + anfrageId + "/ablehnen")
                        .with(csrf())
                        .param("antwort", "Leider nein"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/organisationen/fc-test/anfragen"));
    }
}

