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
 * Test-IDs: STAT-CTRL-01..04.
 *
 * <p>Der Controller bedient seit Phase 5.C beide Sichten in einem Endpoint
 * {@code /statistiken} — Template {@code statistik.html} rendert die zur
 * Mitgliedschaft passende Sektion (Verein, Sponsor oder beide).
 */
@WebMvcTest(controllers = SponsorStatistikController.class)
@Import(SecurityConfig.class)
@ActiveProfiles("dev")
class SponsorStatistikControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean private SponsorStatistikService sponsorStatistikService;
    @MockitoBean private VereinStatistikService vereinStatistikService;
    @MockitoBean private SponsorplatzUserDetailsService userDetailsService;

    @Test
    @DisplayName("STAT-CTRL-01: GET /statistiken anonym → Redirect zu /login")
    void anonymRedirect() throws Exception {
        mockMvc.perform(get("/statistiken"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
    }

    @Test
    @WithMockUser("ohne-org@test.ch")
    @DisplayName("STAT-CTRL-02: User ohne Org → beide DTOs leer, View statistik (Template zeigt Empty-State)")
    void ohneOrgLiefertLeereDtos() throws Exception {
        when(sponsorStatistikService.fuerUser(anyString())).thenReturn(SponsorStatistik.leer());
        when(vereinStatistikService.fuerUser(anyString())).thenReturn(VereinStatistik.leer());

        mockMvc.perform(get("/statistiken"))
                .andExpect(status().isOk())
                .andExpect(view().name("statistik"))
                .andExpect(model().attributeExists("sponsorStatistik"))
                .andExpect(model().attributeExists("vereinStatistik"));
    }

    @Test
    @WithMockUser("sponsor@css.test")
    @DisplayName("STAT-CTRL-03: User mit Sponsor-Org → sponsorStatistik gefüllt, vereinStatistik leer")
    void mitSponsorOrgLiefertSponsorStatistik() throws Exception {
        SponsorStatistik stat = new SponsorStatistik(
                1, 5, 0, new BigDecimal("25000.00"),
                3, 8, 2,
                4, 12, 1,
                Map.of(), List.of("CSS Versicherung"));
        when(sponsorStatistikService.fuerUser("sponsor@css.test")).thenReturn(stat);
        when(vereinStatistikService.fuerUser("sponsor@css.test")).thenReturn(VereinStatistik.leer());

        mockMvc.perform(get("/statistiken"))
                .andExpect(status().isOk())
                .andExpect(view().name("statistik"))
                .andExpect(model().attribute("sponsorStatistik", stat));
    }

    @Test
    @WithMockUser("vorstand@verein.test")
    @DisplayName("STAT-CTRL-04: User mit Verein-Org → vereinStatistik gefüllt, sponsorStatistik leer (Bug-Fix)")
    void mitVereinOrgLiefertVereinStatistik() throws Exception {
        VereinStatistik vereinStat = new VereinStatistik(
                3, 5,
                2, 8, 1,
                0, 1, 0,
                1, 4, 0, new BigDecimal("12000.00"),
                2, 6, 0,
                List.of("FC Beispiel"));
        when(sponsorStatistikService.fuerUser("vorstand@verein.test")).thenReturn(SponsorStatistik.leer());
        when(vereinStatistikService.fuerUser("vorstand@verein.test")).thenReturn(vereinStat);

        mockMvc.perform(get("/statistiken"))
                .andExpect(status().isOk())
                .andExpect(view().name("statistik"))
                .andExpect(model().attribute("vereinStatistik", vereinStat));
    }
}
