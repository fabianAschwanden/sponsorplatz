package ch.sponsorplatz.home;

import ch.sponsorplatz.anfrage.EngagementService;
import ch.sponsorplatz.anfrage.EngagementView;
import ch.sponsorplatz.organisation.Branche;
import ch.sponsorplatz.shared.config.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * SP-02/SP-03: Startseite rendert + zeigt den Engagement-Teaser.
 */
@WebMvcTest(controllers = HomeController.class)
@Import(SecurityConfig.class)
@ActiveProfiles("dev")
class HomeControllerTest {

    @Autowired MockMvc mockMvc;
    @MockitoBean EngagementService engagementService;

    @Test
    void rootGibtIndexZurueck() throws Exception {
        mockMvc.perform(get("/"))
            .andExpect(status().isOk())
            .andExpect(view().name("home/index"))
            .andExpect(model().attribute("aktiveSeite", "home"))
            .andExpect(model().attributeExists("featuredEngagements"));
    }

    /** SP-03: Engagement-Teaser rendert mit Verein-Karte + Link ins Marken-Schaufenster. */
    @Test
    void engagementTeaserRendert() throws Exception {
        EngagementView ev = new EngagementView(UUID.randomUUID(), "CSS Versicherung", "css-versicherung",
                "FC Beispiel", "fc-beispiel", Branche.SPORT, null, "Sommerfest", "sommerfest", "Gold",
                "Zürich", Instant.now());
        when(engagementService.findeNeuesteEngagements(anyInt())).thenReturn(List.of(ev));

        mockMvc.perform(get("/"))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("pe-grid")))
            .andExpect(content().string(containsString("FC Beispiel")))
            .andExpect(content().string(containsString("/marken/css-versicherung/engagements")));
    }
}
