package ch.sponsorplatz.shared.config;

import ch.sponsorplatz.home.HomeController;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * SEED-02: Demo-Disclaimer-Banner wird bei aktivem demo-modus gerendert.
 */
@WebMvcTest(controllers = HomeController.class)
@Import({SecurityConfig.class, DemoModusAdvice.class})
@ActiveProfiles("dev")
@TestPropertySource(properties = "sponsorplatz.demo-modus=true")
class DemoModusAdviceTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("SEED-02: Demo-Disclaimer rendert bei sponsorplatz.demo-modus=true")
    void disclaimerRendertBeiDemoModus() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("demoModus", true))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("DEMO")));
    }
}
