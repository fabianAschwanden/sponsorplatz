package ch.sponsorplatz.home;

import ch.sponsorplatz.shared.config.SecurityConfig;
import ch.sponsorplatz.shared.mail.MailService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Tests für {@link SupportController}.
 * Test-IDs: SUP-01..03 in {@code specs/TESTSTRATEGIE.md}.
 */
@WebMvcTest(SupportController.class)
@Import(SecurityConfig.class)
@ActiveProfiles("dev")
class SupportControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private MailService mailService;

    @MockitoBean
    private ch.sponsorplatz.benutzer.SponsorplatzUserDetailsService userDetailsService;

    @Test
    @DisplayName("SUP-01: /support ohne Auth → Redirect auf Login")
    void ohneAuthRedirect() throws Exception {
        mockMvc.perform(get("/support"))
                .andExpect(status().is3xxRedirection());
    }

    @Test
    @WithMockUser("user@test.ch")
    @DisplayName("SUP-02: /support mit Auth → 200 + Formular")
    void mitAuthZeigtFormular() throws Exception {
        mockMvc.perform(get("/support"))
                .andExpect(status().isOk())
                .andExpect(view().name("support"))
                .andExpect(model().attributeExists("supportForm"));
    }

    @Test
    @WithMockUser("user@test.ch")
    @DisplayName("SUP-03: POST /support sendet Mail und redirectet")
    void absendenSendetMail() throws Exception {
        when(mailService.effektiverAbsender()).thenReturn("noreply@sponsorplatz.ch");

        mockMvc.perform(post("/support")
                        .param("betreff", "Frage zur Verifizierung")
                        .param("nachricht", "Wie lange dauert die Verifizierung meines Vereins?")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/support"));

        verify(mailService).sendePlain(anyString(), anyString(), anyString());
    }

    @Test
    @WithMockUser("user@test.ch")
    @DisplayName("SUP-04: POST /support mit leerem Betreff → Validierungsfehler")
    void leererBetreffZeigtFehler() throws Exception {
        mockMvc.perform(post("/support")
                        .param("betreff", "")
                        .param("nachricht", "Eine ausreichend lange Nachricht für die Validierung.")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(view().name("support"))
                .andExpect(model().attributeHasFieldErrors("supportForm", "betreff"));
    }
}

