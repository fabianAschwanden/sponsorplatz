package ch.sponsorplatz.home;

import ch.sponsorplatz.shared.config.SecurityConfig;
import ch.sponsorplatz.benutzer.AppUserService;
import ch.sponsorplatz.benutzer.SponsorplatzUserDetailsService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.mock;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

/**
 * Tests für statische Public-Pages (Impressum, Datenschutz, AGB).
 *
 * Test-IDs: INFO-01, INFO-02, PUB-03, PUB-04 in {@code specs/TESTSTRATEGIE.md}.
 */
@WebMvcTest(InfoController.class)
@Import({SecurityConfig.class, InfoControllerTest.MockBeans.class})
@ActiveProfiles("dev")
class InfoControllerTest {

    @TestConfiguration
    static class MockBeans {
        @Bean
        AppUserService appUserService() { return mock(AppUserService.class); }
        @Bean
        SponsorplatzUserDetailsService userDetailsService() { return mock(SponsorplatzUserDetailsService.class); }
    }

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("INFO-01: GET /impressum → 200 + impressum-Template, public erreichbar")
    @WithAnonymousUser
    void impressumOeffentlichErreichbar() throws Exception {
        mockMvc.perform(get("/impressum"))
                .andExpect(status().isOk())
                .andExpect(view().name("home/impressum"));
    }

    @Test
    @DisplayName("INFO-02: GET /datenschutz → 200 + datenschutz-Template, public erreichbar")
    @WithAnonymousUser
    void datenschutzOeffentlichErreichbar() throws Exception {
        mockMvc.perform(get("/datenschutz"))
                .andExpect(status().isOk())
                .andExpect(view().name("home/datenschutz"));
    }

    @Test
    @DisplayName("PUB-03: GET /agb → 200 + agb-Template, public erreichbar")
    @WithAnonymousUser
    void agbOeffentlichErreichbar() throws Exception {
        mockMvc.perform(get("/agb"))
                .andExpect(status().isOk())
                .andExpect(view().name("home/agb"));
    }

    @Test
    @DisplayName("PUB-04: Datenschutz-Seite dokumentiert Cookie-Banner-Verzicht (nur technische Cookies)")
    @WithAnonymousUser
    void datenschutzEnthaeltCookieBannerHinweis() throws Exception {
        mockMvc.perform(get("/datenschutz"))
                .andExpect(status().isOk())
                .andExpect(content().string(
                        org.hamcrest.Matchers.containsString("kein Tracking")));
    }
}
