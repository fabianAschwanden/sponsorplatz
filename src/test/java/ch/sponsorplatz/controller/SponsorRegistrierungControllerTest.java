package ch.sponsorplatz.controller;

import ch.sponsorplatz.shared.config.SecurityConfig;
import ch.sponsorplatz.dto.SponsorRegistrierungFormDto;
import ch.sponsorplatz.service.SponsorRegistrierungService;
import ch.sponsorplatz.benutzer.SponsorplatzUserDetailsService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Tests für Sponsor-Registrierungs-Controller (SR-04..07).
 */
@WebMvcTest(controllers = SponsorRegistrierungController.class)
@Import(SecurityConfig.class)
@ActiveProfiles("dev")
class SponsorRegistrierungControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private SponsorRegistrierungService sponsorRegistrierungService;

    @MockBean
    private SponsorplatzUserDetailsService userDetailsService;

    /** SR-04: GET /sponsor/registrieren → 200 + sponsor-registrieren Template. */
    @Test
    @DisplayName("SR-04: GET /sponsor/registrieren zeigt Formular")
    void formularWirdAngezeigt() throws Exception {
        mockMvc.perform(get("/sponsor/registrieren"))
                .andExpect(status().isOk())
                .andExpect(view().name("sponsor-registrieren"))
                .andExpect(model().attributeExists("sponsorForm"));
    }

    /** SR-05: POST /sponsor/registrieren mit gültigen Daten → Redirect. */
    @Test
    @DisplayName("SR-05: POST /sponsor/registrieren valid → 302 Redirect /login?registriert")
    void registrierungErfolgreich() throws Exception {
        mockMvc.perform(post("/sponsor/registrieren")
                        .param("email", "kontakt@firma.ch")
                        .param("anzeigename", "Max Muster")
                        .param("passwort", "sicheres-passwort")
                        .param("firmenname", "Muster AG")
                        .param("branche", "SPORT")
                        .param("rechtsform", "AG")
                        .param("websiteUrl", "https://muster.ch")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?registriert"));
    }

    /** SR-06: POST /sponsor/registrieren mit Validierungsfehler → bleibt auf Formular. */
    @Test
    @DisplayName("SR-06: POST /sponsor/registrieren mit Validierungsfehler → Formular")
    void registrierungMitValidierungsfehler() throws Exception {
        mockMvc.perform(post("/sponsor/registrieren")
                        .param("email", "")
                        .param("anzeigename", "")
                        .param("passwort", "kurz")
                        .param("firmenname", "")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(view().name("sponsor-registrieren"));
    }

    /** SR-07: POST /sponsor/registrieren mit doppelter E-Mail → Fehlermeldung. */
    @Test
    @DisplayName("SR-07: POST /sponsor/registrieren doppelte E-Mail → Fehlermeldung")
    void registrierungMitDoppelterEmail() throws Exception {
        doThrow(new IllegalArgumentException("E-Mail ist bereits vergeben"))
                .when(sponsorRegistrierungService).registriereSponsor(any(SponsorRegistrierungFormDto.class));

        mockMvc.perform(post("/sponsor/registrieren")
                        .param("email", "gibt-es@firma.ch")
                        .param("anzeigename", "Max Muster")
                        .param("passwort", "sicheres-passwort")
                        .param("firmenname", "Muster AG")
                        .param("branche", "SPORT")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(view().name("sponsor-registrieren"))
                .andExpect(model().attributeExists("fehlermeldung"));
    }
}

