package ch.sponsorplatz.projekt;

import ch.sponsorplatz.shared.config.SecurityConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = OgImageController.class)
@Import(SecurityConfig.class)
@ActiveProfiles("dev")
class OgImageControllerTest {

    @Autowired private MockMvc mockMvc;
    @MockBean private OgImageRenderer renderer;

    @Test
    @DisplayName("OG-01: GET /og/verein/{slug}.png → 200 + image/png")
    void vereinCard() throws Exception {
        when(renderer.rendereVerein("fc-beispiel-zuerich")).thenReturn(new byte[]{1, 2, 3});

        mockMvc.perform(get("/og/verein/fc-beispiel-zuerich.png"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("image/png"))
                .andExpect(header().string("Cache-Control", containsString("max-age=3600")));
    }

    @Test
    @DisplayName("OG-02: GET /og/projekt/{slug}.png → 200 + image/png")
    void projektCard() throws Exception {
        when(renderer.rendereProjekt("sommerfest-2026")).thenReturn(new byte[]{4, 5, 6});

        mockMvc.perform(get("/og/projekt/sommerfest-2026.png"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("image/png"))
                .andExpect(header().string("Cache-Control", containsString("max-age=3600")));
    }

    @Test
    @DisplayName("OG-03: Cache-Control enthält public UND max-age=3600 (nicht nur eines davon)")
    void cacheHeader() throws Exception {
        when(renderer.rendereVerein("test")).thenReturn(new byte[]{0});

        mockMvc.perform(get("/og/verein/test.png"))
                .andExpect(header().string("Cache-Control",
                        allOf(containsString("public"), containsString("max-age=3600"))));
    }
}

