package ch.sponsorplatz.benutzer;

import ch.sponsorplatz.shared.config.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Tests für Registrierungs-Controller (REG-01..04).
 */
@WebMvcTest(controllers = RegistrierungController.class)
@Import(SecurityConfig.class)
@ActiveProfiles("dev")
class RegistrierungControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AppUserService appUserService;

    @MockBean
    private SponsorplatzUserDetailsService userDetailsService;

    /** REG-01: GET /registrieren → 200 + Formular. */
    @Test
    void formularWirdAngezeigt() throws Exception {
        mockMvc.perform(get("/registrieren"))
                .andExpect(status().isOk())
                .andExpect(view().name("registrieren"))
                .andExpect(model().attributeExists("userForm"));
    }

    /** REG-02: POST /registrieren mit gültigen Daten → Redirect zu /login. */
    @Test
    void registrierungErfolgreich() throws Exception {
        AppUser neuerUser = new AppUser();
        neuerUser.setEmail("neu@example.com");
        neuerUser.setAnzeigename("Neuer User");
        when(appUserService.registriere(any(AppUserFormDto.class))).thenReturn(neuerUser);

        mockMvc.perform(post("/registrieren")
                        .param("email", "neu@example.com")
                        .param("anzeigename", "Neuer User")
                        .param("passwort", "sicheres-passwort")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?registriert"));
    }

    /** REG-03: POST /registrieren mit doppelter E-Mail → Fehler im Formular. */
    @Test
    void registrierungMitDoppelterEmail() throws Exception {
        when(appUserService.registriere(any(AppUserFormDto.class)))
                .thenThrow(new IllegalArgumentException("E-Mail ist bereits vergeben"));

        mockMvc.perform(post("/registrieren")
                        .param("email", "gibt-es@example.com")
                        .param("anzeigename", "Egal")
                        .param("passwort", "passwort123")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(view().name("registrieren"))
                .andExpect(model().attributeExists("fehlermeldung"));
    }

    /** REG-04: POST /registrieren mit leerem Passwort → Validierungsfehler. */
    @Test
    void registrierungMitLeeremPasswort() throws Exception {
        mockMvc.perform(post("/registrieren")
                        .param("email", "ok@example.com")
                        .param("anzeigename", "Ok User")
                        .param("passwort", "")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(view().name("registrieren"));
    }
}

