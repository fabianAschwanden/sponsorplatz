package ch.sponsorplatz.shared.config;

import ch.sponsorplatz.benutzer.LoginController;
import ch.sponsorplatz.organisation.OrganisationController;
import ch.sponsorplatz.benutzer.RegistrierungController;
import ch.sponsorplatz.organisation.AccessControl;
import ch.sponsorplatz.benutzer.AppUserService;
import ch.sponsorplatz.organisation.OrganisationService;
import ch.sponsorplatz.benutzer.SponsorplatzUserDetailsService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Tests für Security-Konfiguration (SEC-01..06).
 */
@WebMvcTest(controllers = {RegistrierungController.class, LoginController.class, OrganisationController.class})
@Import(SecurityConfig.class)
@ActiveProfiles("dev")
class SecurityConfigTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AppUserService appUserService;

    @MockBean
    private SponsorplatzUserDetailsService userDetailsService;

    @MockBean
    private OrganisationService organisationService;

    @MockBean
    private AccessControl accessControl;

    /** SEC-01: GET /login → 200. */
    @Test
    void loginSeiteErreichbar() throws Exception {
        mockMvc.perform(get("/login"))
                .andExpect(status().isOk());
    }

    /** SEC-02: GET /organisationen ohne Login → 200 (public). */
    @Test
    void organisationenListeIstPublic() throws Exception {
        mockMvc.perform(get("/organisationen"))
                .andExpect(status().isOk());
    }

    /** SEC-03: GET /organisationen/{slug}/bearbeiten ohne Login → Redirect zu /login. */
    @Test
    void bearbeitenOhneLoginRedirectZuLogin() throws Exception {
        mockMvc.perform(get("/organisationen/test-org/bearbeiten"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
    }

    /** SEC-04: POST /logout → Redirect. */
    @Test
    @WithMockUser
    void logoutRedirected() throws Exception {
        mockMvc.perform(post("/logout").with(csrf()))
                .andExpect(status().is3xxRedirection());
    }

    /** SEC-05: Login mit gültigen Credentials → Redirect. */
    @Test
    void loginMitGueltigemUser() throws Exception {
        mockMvc.perform(post("/login")
                        .param("username", "test@example.com")
                        .param("password", "passwort123")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection());
    }

    /** SEC-06: Login mit falschen Credentials → zurück zu /login?error. */
    @Test
    void loginMitFalschenCredentials() throws Exception {
        mockMvc.perform(post("/login")
                        .param("username", "falsch@example.com")
                        .param("password", "falsch")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?error"));
    }
}

