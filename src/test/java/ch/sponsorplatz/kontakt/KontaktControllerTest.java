package ch.sponsorplatz.kontakt;

import ch.sponsorplatz.benutzer.SponsorplatzUserDetailsService;
import ch.sponsorplatz.shared.config.SecurityConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Tests für {@link KontaktController} — öffentlicher Anfrage-Funnel.
 * Test-IDs: KONT-01..05 in {@code specs/TESTSTRATEGIE.md}.
 */
@WebMvcTest(controllers = KontaktController.class)
@Import(SecurityConfig.class)
@ActiveProfiles("dev")
class KontaktControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private KontaktService kontaktService;

    @MockitoBean
    private SponsorplatzUserDetailsService userDetailsService;

    @Test
    @DisplayName("KONT-01: GET /kontakt ist anonym erreichbar und rendert Formular")
    void formularIstPublic() throws Exception {
        mockMvc.perform(get("/kontakt"))
                .andExpect(status().isOk())
                .andExpect(view().name("kontakt"))
                .andExpect(model().attributeExists("kontaktForm"));
    }

    @Test
    @DisplayName("KONT-02: POST /kontakt happy path → Service-Call + Flash + Redirect")
    void happyPath() throws Exception {
        org.mockito.Mockito.when(kontaktService.erfolgsMeldung(org.mockito.ArgumentMatchers.any()))
                .thenReturn("Danke");

        mockMvc.perform(post("/kontakt")
                        .param("name", "Max Muster")
                        .param("email", "max@example.com")
                        .param("betreff", "Sponsoring-Interesse")
                        .param("nachricht", "Wir möchten mehr über die Plattform erfahren.")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/kontakt"))
                .andExpect(flash().attribute("erfolgsMeldung", "Danke"));

        verify(kontaktService).verarbeite(any(KontaktFormDto.class));
    }

    @Test
    @DisplayName("KONT-03: Validierung fehlerhaft → bleibt auf Formular, kein Service-Call")
    void validierungFehlerBleibtAufFormular() throws Exception {
        mockMvc.perform(post("/kontakt")
                        .param("name", "")
                        .param("email", "keine-email")
                        .param("betreff", "")
                        .param("nachricht", "")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(view().name("kontakt"));

        verify(kontaktService, never()).verarbeite(any());
    }

    @Test
    @DisplayName("KONT-04: Honeypot-Feld ausgefüllt → Silent-Success, kein Service-Call")
    void honeypotSchluckt() throws Exception {
        mockMvc.perform(post("/kontakt")
                        .param("name", "Bot")
                        .param("email", "bot@spam.com")
                        .param("betreff", "Spam")
                        .param("nachricht", "Spam-Inhalt")
                        .param("homepage", "https://spam.example") // Honeypot
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/kontakt"));

        verify(kontaktService, never()).verarbeite(any());
    }

    @Test
    @DisplayName("KONT-05: POST ohne CSRF → 403")
    void csrfFehltGibt403() throws Exception {
        mockMvc.perform(post("/kontakt")
                        .param("name", "Max")
                        .param("email", "m@x.ch")
                        .param("betreff", "Hi")
                        .param("nachricht", "Test"))
                .andExpect(status().isForbidden());
    }
}
