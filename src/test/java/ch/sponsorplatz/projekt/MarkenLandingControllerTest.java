package ch.sponsorplatz.projekt;

import  ch.sponsorplatz.organisation.Branche;

import ch.sponsorplatz.shared.config.SecurityConfig;
import ch.sponsorplatz.benutzer.SponsorplatzUserDetailsService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = MarkenLandingController.class)
@Import(SecurityConfig.class)
@ActiveProfiles("dev")
class MarkenLandingControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private StatistikService statistikService;

    @MockitoBean
    private SponsorplatzUserDetailsService userDetailsService;

    /** MARK-01: Marken-Landing-Page rendert ohne Login. */
    @Test
    void markenLandingRendert() throws Exception {
        when(statistikService.vereineProBranche()).thenReturn(Map.of(Branche.SPORT, 5L));
        when(statistikService.anzahlAktiveProjekte()).thenReturn(10L);

        mockMvc.perform(get("/fuer-marken"))
                .andExpect(status().isOk())
                .andExpect(view().name("marken-landing"))
                .andExpect(model().attributeExists("vereineProBranche", "anzahlProjekte"));
    }

    /** MARK-02: Statistik-Werte sind korrekt im Model. */
    @Test
    void statistikWerteImModel() throws Exception {
        Map<Branche, Long> stats = Map.of(Branche.SPORT, 5L, Branche.REHA, 3L);
        when(statistikService.vereineProBranche()).thenReturn(stats);
        when(statistikService.anzahlAktiveProjekte()).thenReturn(15L);

        mockMvc.perform(get("/fuer-marken"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("anzahlProjekte", 15L))
                .andExpect(model().attribute("vereineProBranche", stats));
    }

    /** MARK-03: CTA-Link zu Sponsor-Registrierung ist vorhanden. */
    @Test
    void ctaLinkVorhanden() throws Exception {
        when(statistikService.vereineProBranche()).thenReturn(Map.of());
        when(statistikService.anzahlAktiveProjekte()).thenReturn(0L);

        mockMvc.perform(get("/fuer-marken"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("/sponsor/registrieren")));
    }

    /**
     * MARK-06: Render-Assertion — Trust-Indikatoren ("Kuratiert", "Lokal",
     * "Messbar") landen tatsächlich im HTML. Defense gegen Template-Regression:
     * jemand kürzt das `<section class="trust-indikatoren">` und merkt's nicht.
     */
    @Test
    void trustIndikatorenImHtml() throws Exception {
        when(statistikService.vereineProBranche()).thenReturn(Map.of());
        when(statistikService.anzahlAktiveProjekte()).thenReturn(0L);

        mockMvc.perform(get("/fuer-marken"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Kuratiert")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Lokal")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Messbar")));
    }
}

