package ch.sponsorplatz.benutzer;

import ch.sponsorplatz.shared.config.SecurityConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = PasswortResetController.class)
@Import(SecurityConfig.class)
@ActiveProfiles("dev")
class PasswortResetControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PasswortResetService resetService;

    @MockitoBean
    private SponsorplatzUserDetailsService userDetailsService;

    @Test
    @DisplayName("PWRCTRL-01: GET /passwort-vergessen ist public, zeigt Formular")
    void passwortVergessenFormular() throws Exception {
        mockMvc.perform(get("/passwort-vergessen"))
                .andExpect(status().isOk())
                .andExpect(view().name("benutzer/passwort-vergessen"));
    }

    @Test
    @DisplayName("PWRCTRL-02: POST /passwort-vergessen ruft Service auf")
    void passwortVergessenPost() throws Exception {
        mockMvc.perform(post("/passwort-vergessen")
                        .param("email", "test@sp.ch")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(model().attribute("gesendet", true));

        verify(resetService).sendeResetMail("test@sp.ch");
    }

    @Test
    @DisplayName("PWRCTRL-03: GET /passwort-reset mit gültigem Token zeigt Formular")
    void resetFormularGueltig() throws Exception {
        AppUser user = new AppUser();
        when(resetService.validiereToken("abc")).thenReturn(user);

        mockMvc.perform(get("/passwort-reset").param("token", "abc"))
                .andExpect(status().isOk())
                .andExpect(view().name("benutzer/passwort-reset"))
                .andExpect(model().attribute("token", "abc"));
    }

    @Test
    @DisplayName("PWRCTRL-04: GET /passwort-reset mit ungültigem Token → Fehler")
    void resetFormularUngueltig() throws Exception {
        when(resetService.validiereToken("bad")).thenThrow(new IllegalArgumentException("ungültig"));

        mockMvc.perform(get("/passwort-reset").param("token", "bad"))
                .andExpect(status().isOk())
                .andExpect(view().name("benutzer/passwort-vergessen"))
                .andExpect(model().attributeExists("fehlermeldung"));
    }

    @Test
    @DisplayName("PWRCTRL-05: POST /passwort-reset → Redirect zu Login")
    void resetPasswortErfolgreich() throws Exception {
        mockMvc.perform(post("/passwort-reset")
                        .param("token", "valid")
                        .param("neuesPasswort", "sicheres-pw")
                        .param("passwortBestaetigung", "sicheres-pw")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"))
                .andExpect(flash().attributeExists("erfolgsMeldung"));

        verify(resetService).setzeNeuesPasswort("valid", "sicheres-pw");
    }

    @Test
    @DisplayName("PWRCTRL-06: POST /passwort-reset mit nicht übereinstimmenden PW → Fehler")
    void resetPasswortMismatch() throws Exception {
        mockMvc.perform(post("/passwort-reset")
                        .param("token", "t")
                        .param("neuesPasswort", "passwort1")
                        .param("passwortBestaetigung", "passwort2")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(view().name("benutzer/passwort-reset"))
                .andExpect(model().attribute("fehlermeldung", "Passwörter stimmen nicht überein"));
    }
}

