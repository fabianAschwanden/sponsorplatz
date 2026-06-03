package ch.sponsorplatz.home;

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

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

/**
 * Tests für {@link HilfeController}.
 * Test-IDs: HILFE-01..02 in {@code specs/TESTSTRATEGIE.md}.
 */
@WebMvcTest(HilfeController.class)
@Import(SecurityConfig.class)
@ActiveProfiles("dev")
class HilfeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ch.sponsorplatz.benutzer.SponsorplatzUserDetailsService userDetailsService;

    /** HILFE-01: /hilfe ohne Auth → Redirect auf Login (anyRequest().authenticated()). */
    @Test
    @DisplayName("HILFE-01: /hilfe anonym → Redirect zu /login")
    void ohneAuthRedirect() throws Exception {
        mockMvc.perform(get("/hilfe"))
                .andExpect(status().is3xxRedirection());
    }

    /** HILFE-02: /hilfe mit Auth → 200, rendert das Karten-Grid. */
    @Test
    @WithMockUser("user@test.ch")
    @DisplayName("HILFE-02: /hilfe authentifiziert → 200 + Feature-Karten")
    void mitAuthRendertKarten() throws Exception {
        mockMvc.perform(get("/hilfe"))
                .andExpect(status().isOk())
                .andExpect(view().name("home/hilfe"))
                .andExpect(content().string(containsString("hilfe-grid")));
    }
}
