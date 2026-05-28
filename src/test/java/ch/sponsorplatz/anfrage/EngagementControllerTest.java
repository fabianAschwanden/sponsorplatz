package ch.sponsorplatz.anfrage;

import ch.sponsorplatz.organisation.Branche;
import ch.sponsorplatz.shared.config.SecurityConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = EngagementController.class)
@Import(SecurityConfig.class)
@ActiveProfiles("dev")
@org.springframework.security.test.context.support.WithMockUser
class EngagementControllerTest {

    @Autowired private MockMvc mockMvc;
    @MockitoBean private EngagementService engagementService;

    @Test
    @DisplayName("ENG-01-CTRL: GET /marken/{slug}/engagements → 200 + Schaufenster-View + ansicht")
    void schaufensterRendert() throws Exception {
        when(engagementService.findeSchaufenster(eq("css-versicherung"), isNull(), isNull()))
                .thenReturn(ansicht(null, null));

        mockMvc.perform(get("/marken/css-versicherung/engagements"))
                .andExpect(status().isOk())
                .andExpect(view().name("anfrage/engagement-schaufenster"))
                .andExpect(model().attributeExists("ansicht", "sponsorSlug"))
                // Visuelle Struktur ist gerendert: Hero, Karten-Raster, Branche-Farbklasse.
                .andExpect(content().string(org.hamcrest.Matchers.containsString("sf-hero")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("sf-grid")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("branche-SPORT")));
    }

    @Test
    @DisplayName("ENG-02-CTRL: Region-Filter wird an Service delegiert")
    void regionFilter() throws Exception {
        when(engagementService.findeSchaufenster(eq("css-versicherung"), eq("Zürich"), isNull()))
                .thenReturn(ansicht("Zürich", null));

        mockMvc.perform(get("/marken/css-versicherung/engagements").param("region", "Zürich"))
                .andExpect(status().isOk());
        verify(engagementService).findeSchaufenster("css-versicherung", "Zürich", null);
    }

    @Test
    @DisplayName("ENG-03-CTRL: Branche-Filter wird an Service delegiert")
    void brancheFilter() throws Exception {
        when(engagementService.findeSchaufenster(eq("css-versicherung"), isNull(), eq(Branche.SPORT)))
                .thenReturn(ansicht(null, Branche.SPORT));

        mockMvc.perform(get("/marken/css-versicherung/engagements").param("branche", "SPORT"))
                .andExpect(status().isOk());
        verify(engagementService).findeSchaufenster("css-versicherung", null, Branche.SPORT);
    }

    private SchaufensterAnsicht ansicht(String region, Branche branche) {
        EngagementView ev = new EngagementView(UUID.randomUUID(), "CSS Versicherung", "css-versicherung",
                "FC Beispiel", "fc-beispiel", Branche.SPORT, "Sommerfest", "sommerfest", "Gold",
                "Zürich", Instant.now());
        return SchaufensterAnsicht.erstelle("CSS Versicherung", "css-versicherung", null,
                List.of(ev), region, branche);
    }
}
