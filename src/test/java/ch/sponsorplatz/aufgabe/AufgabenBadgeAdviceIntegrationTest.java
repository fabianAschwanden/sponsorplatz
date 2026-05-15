package ch.sponsorplatz.aufgabe;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration: prüft im vollen Spring-Kontext, dass {@link AufgabenBadgeAdvice}
 * geladen wird und {@code badgeAufgaben} ans Sidebar-Template durchreicht.
 * Test-IDs: AUFG-BADGE-INT-01..02.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@ActiveProfiles("dev")
class AufgabenBadgeAdviceIntegrationTest {

    @Autowired
    private WebApplicationContext context;

    private MockMvc mockMvc;

    @TestConfiguration
    static class MockAufgabenServiceConfig {
        @Bean
        @Primary
        AufgabenService aufgabenService() {
            AufgabenService mock = mock(AufgabenService.class);
            when(mock.zaehleMeineOffenen(eq("u@test.ch"))).thenReturn(3L);
            when(mock.zaehleMeineOffenen(eq("leer@test.ch"))).thenReturn(0L);
            return mock;
        }
    }

    @org.junit.jupiter.api.BeforeEach
    void setUp() {
        this.mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(springSecurity())
                .build();
    }

    /** AUFG-BADGE-INT-01: User mit 3 offenen Aufgaben → Badge im HTML + Modell. */
    @Test
    @WithMockUser("u@test.ch")
    @DisplayName("AUFG-BADGE-INT-01: 3 offene Aufgaben → Badge erscheint im Sidebar-HTML")
    void mitOffenenAufgabenZeigtBadge() throws Exception {
        mockMvc.perform(get("/aufgaben"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("badgeAufgaben", 3L))
                .andExpect(content().string(org.hamcrest.Matchers.containsString(">3<")));
    }

    /** AUFG-BADGE-INT-02: User ohne Aufgaben → Modell-Attribut null, kein Badge-Element. */
    @Test
    @WithMockUser("leer@test.ch")
    @DisplayName("AUFG-BADGE-INT-02: Keine Aufgaben → badgeAufgaben=null, kein Badge")
    void ohneAufgabenKeinBadge() throws Exception {
        mockMvc.perform(get("/aufgaben"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("badgeAufgaben", org.hamcrest.Matchers.nullValue()));
    }

    /**
     * AUFG-BADGE-INT-03: Advice greift auch auf {@code /dashboard} — nicht
     * nur auf der Aufgaben-Seite selbst. Stellt sicher, dass die Sidebar auf
     * allen authentifizierten Seiten den Badge ziehen kann.
     */
    @Test
    @WithMockUser("u@test.ch")
    @DisplayName("AUFG-BADGE-INT-03: badgeAufgaben auch auf /dashboard verfügbar")
    void badgeAuchAufDashboard() throws Exception {
        mockMvc.perform(get("/dashboard"))
                .andExpect(model().attribute("badgeAufgaben", 3L));
    }
}
