package ch.sponsorplatz.controller;

import ch.sponsorplatz.config.SecurityConfig;
import ch.sponsorplatz.service.SponsorplatzUserDetailsService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@WebMvcTest(controllers = DashboardController.class)
@Import(SecurityConfig.class)
@ActiveProfiles("dev")
class DashboardControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private SponsorplatzUserDetailsService userDetailsService;

    /** DASH-01: GET /dashboard anonym → Redirect zu /login. */
    @Test
    void dashboardAnonymRedirectZuLogin() throws Exception {
        mockMvc.perform(get("/dashboard"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrlPattern("**/login"));
    }

    /** DASH-02: GET /dashboard eingeloggt → 200 + View dashboard. */
    @Test
    @WithMockUser
    void dashboardEingeloggtIst200() throws Exception {
        mockMvc.perform(get("/dashboard"))
            .andExpect(status().isOk())
            .andExpect(view().name("dashboard"));
    }

    /** DASH-03: Model enthält die Platzhalter-Attribute. */
    @Test
    @WithMockUser
    void dashboardModelEnthaeltAttribute() throws Exception {
        mockMvc.perform(get("/dashboard"))
            .andExpect(model().attributeExists(
                "aktiveSeite",
                "aktuellerMonat",
                "aktuelleKw",
                "anzahlOrganisationen",
                "anzahlProjekte",
                "anzahlAnfragen",
                "anzahlOffeneAnfragen"));
    }
}
