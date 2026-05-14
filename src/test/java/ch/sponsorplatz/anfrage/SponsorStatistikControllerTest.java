package ch.sponsorplatz.anfrage;

import ch.sponsorplatz.benutzer.SponsorplatzUserDetailsService;
import ch.sponsorplatz.shared.config.SecurityConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

/**
 * Tests für {@link SponsorStatistikController}.
 * Test-IDs: STAT-CTRL-01..03.
 */
@WebMvcTest(controllers = SponsorStatistikController.class)
@Import(SecurityConfig.class)
@ActiveProfiles("dev")
class SponsorStatistikControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean private SponsorStatistikService statistikService;
    @MockitoBean private SponsorplatzUserDetailsService userDetailsService;

    @Test
    @DisplayName("STAT-CTRL-01: GET /statistiken anonym → Redirect zu /login")
    void anonymRedirect() throws Exception {
        mockMvc.perform(get("/statistiken"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
    }

    @Test
    @WithMockUser("nicht-sponsor@test.ch")
    @DisplayName("STAT-CTRL-02: User ohne Sponsor-Org → 200 + leeres DTO (Empty-State im Template)")
    void ohneSponsorOrgLiefertLeeresDto() throws Exception {
        when(statistikService.fuerUser(anyString())).thenReturn(SponsorStatistik.leer());

        mockMvc.perform(get("/statistiken"))
                .andExpect(status().isOk())
                .andExpect(view().name("sponsor-statistik"))
                .andExpect(model().attributeExists("statistik"));
    }

    @Test
    @WithMockUser("sponsor@css.test")
    @DisplayName("STAT-CTRL-03: User mit Sponsor-Org → DTO mit Org-Namen + Kennzahlen")
    void mitSponsorOrgLiefertKennzahlen() throws Exception {
        SponsorStatistik stat = new SponsorStatistik(
                1, 5, 0, new BigDecimal("25000.00"),
                3, 8, 2,
                4, 12, 1,
                Map.of(), List.of("CSS Versicherung"));
        when(statistikService.fuerUser("sponsor@css.test")).thenReturn(stat);

        mockMvc.perform(get("/statistiken"))
                .andExpect(status().isOk())
                .andExpect(view().name("sponsor-statistik"))
                .andExpect(model().attribute("statistik", stat));
    }
}
