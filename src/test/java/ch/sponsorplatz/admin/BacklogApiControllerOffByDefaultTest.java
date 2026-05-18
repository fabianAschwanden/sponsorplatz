package ch.sponsorplatz.admin;

import ch.sponsorplatz.benutzer.SponsorplatzUserDetailsService;
import ch.sponsorplatz.shared.config.ApiKeyFilter;
import ch.sponsorplatz.shared.config.SecurityConfig;
import ch.sponsorplatz.shared.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * BAPI-10: Off-by-default-Sicherheit — bei leerem
 * {@code sponsorplatz.api.key} antwortet der {@link ApiKeyFilter}
 * mit 503 (statt 401), egal welcher Key im Header steht. Damit ist
 * eindeutig: API ist serverseitig nicht freigeschaltet, nicht
 * „falscher Key".
 *
 * <p>Eigene Test-Klasse, weil {@code @TestPropertySource} pro Class
 * geladen wird — die Hauptsuite läuft mit gesetztem Test-Key.
 */
@WebMvcTest(controllers = BacklogApiController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class, ApiKeyFilter.class})
@ActiveProfiles("dev")
@TestPropertySource(properties = "sponsorplatz.api.key=")
class BacklogApiControllerOffByDefaultTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private BacklogService backlogService;

    @MockitoBean
    private SponsorplatzUserDetailsService userDetailsService;

    @Test
    @DisplayName("BAPI-10: API-Key leer → 503 Service Unavailable + Hinweis-JSON")
    void leererKeySperrtApi() throws Exception {
        mockMvc.perform(get("/api/backlog")
                        .header("X-API-Key", "irgendein-key"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.error").value(
                        "API nicht aktiviert — sponsorplatz.api.key muss konfiguriert sein."));
    }
}
