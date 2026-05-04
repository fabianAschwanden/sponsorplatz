package ch.sponsorplatz.controller;

import ch.sponsorplatz.config.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * SP-02: GET / liefert 200 und das index-Template.
 */
@WebMvcTest(controllers = HomeController.class)
@Import(SecurityConfig.class)
@ActiveProfiles("dev")
class HomeControllerTest {

    @Autowired
    MockMvc mockMvc;

    @Test
    void rootGibtIndexZurueck() throws Exception {
        mockMvc.perform(get("/"))
            .andExpect(status().isOk())
            .andExpect(view().name("index"))
            .andExpect(model().attribute("aktiveSeite", "home"));
    }
}
