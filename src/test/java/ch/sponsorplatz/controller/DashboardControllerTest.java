package ch.sponsorplatz.controller;

import ch.sponsorplatz.shared.config.SecurityConfig;
import ch.sponsorplatz.dto.DashboardDaten;
import ch.sponsorplatz.benutzer.AppUserService;
import ch.sponsorplatz.service.DashboardService;
import ch.sponsorplatz.service.MatchingService;
import ch.sponsorplatz.benutzer.SponsorplatzUserDetailsService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
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

    @MockBean
    private DashboardService dashboardService;

    @MockBean
    private MatchingService matchingService;

    @MockBean
    private AppUserService appUserService;

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
        when(dashboardService.ladeDashboardDaten(anyString())).thenReturn(DashboardDaten.leer());
        when(appUserService.findeNachEmail(anyString())).thenReturn(Optional.empty());

        mockMvc.perform(get("/dashboard"))
            .andExpect(status().isOk())
            .andExpect(view().name("dashboard"));
    }

    /** DASH-03: Model enthält die Dashboard-Attribute. */
    @Test
    @WithMockUser
    void dashboardModelEnthaeltAttribute() throws Exception {
        when(dashboardService.ladeDashboardDaten(anyString()))
            .thenReturn(DashboardDaten.von(3, 5, 12, 4));
        when(appUserService.findeNachEmail(anyString())).thenReturn(Optional.empty());

        mockMvc.perform(get("/dashboard"))
            .andExpect(model().attributeExists(
                "aktiveSeite",
                "aktuellerMonat",
                "aktuelleKw",
                "anzahlOrganisationen",
                "anzahlProjekte",
                "anzahlAnfragen",
                "anzahlOffeneAnfragen",
                "empfehlungen"))
            .andExpect(model().attribute("anzahlOrganisationen", 3L))
            .andExpect(model().attribute("anzahlProjekte", 5L))
            .andExpect(model().attribute("anzahlAnfragen", 12L))
            .andExpect(model().attribute("anzahlOffeneAnfragen", 4L));
    }

    /** DASH-04: Werte kommen aus DashboardService.ladeDashboardDaten(email). */
    @Test
    @WithMockUser(username = "test@example.ch")
    void dashboardRuftServiceMitEmailAuf() throws Exception {
        when(dashboardService.ladeDashboardDaten("test@example.ch"))
            .thenReturn(DashboardDaten.leer());
        when(appUserService.findeNachEmail(anyString())).thenReturn(Optional.empty());

        mockMvc.perform(get("/dashboard"))
            .andExpect(status().isOk());

        verify(dashboardService).ladeDashboardDaten("test@example.ch");
    }
}
