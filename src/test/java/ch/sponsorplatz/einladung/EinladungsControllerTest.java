package ch.sponsorplatz.einladung;
import ch.sponsorplatz.shared.exception.GlobalExceptionHandler;

import ch.sponsorplatz.shared.config.SecurityConfig;
import ch.sponsorplatz.shared.exception.BenutzerNichtRegistriertException;
import ch.sponsorplatz.organisation.Rolle;
import ch.sponsorplatz.benutzer.SponsorplatzUserDetailsService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@WebMvcTest(controllers = EinladungsController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class})
@ActiveProfiles("dev")
class EinladungsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private SponsorplatzUserDetailsService userDetailsService;

    @MockBean
    private EinladungsService einladungsService;

    /** EINL-07: GET zeigt Vorschau-Page, ruft nimmAn NICHT auf (Outlook-/Slack-Crawler-Schutz). */
    @Test
    void getZeigtVorschauOhneStateAenderung() throws Exception {
        EinladungVorschauView vorschau = new EinladungVorschauView(
                "valid-token",
                "FC Beispiel",
                "Admin User",
                Rolle.ORG_EDITOR,
                "neu@example.ch",
                Instant.now().plus(1, ChronoUnit.DAYS));
        when(einladungsService.ladeVorschau("valid-token")).thenReturn(vorschau);

        mockMvc.perform(get("/einladung/annehmen").param("token", "valid-token"))
                .andExpect(status().isOk())
                .andExpect(view().name("einladung-vorschau"))
                .andExpect(model().attributeExists("vorschau"));

        // Kritisch: GET darf nimmAn NIEMALS aufrufen!
        verify(einladungsService, never()).nimmAn(any());
    }

    /** EINL-08: GET mit ungültigem Token → 400 (error.html), keine State-Änderung. */
    @Test
    void getMitUngueltigemTokenGibtFehlerOhneAenderung() throws Exception {
        when(einladungsService.ladeVorschau("bad-token"))
                .thenThrow(new IllegalArgumentException("Einladungs-Token ist ungültig"));

        mockMvc.perform(get("/einladung/annehmen").param("token", "bad-token"))
                .andExpect(status().isBadRequest());

        verify(einladungsService, never()).nimmAn(any());
    }

    /** EINL-05: POST nimmt Einladung an + zeigt Erfolgsseite. */
    @Test
    void postAnnehmenMitGueltigemTokenZeigtErfolg() throws Exception {
        doNothing().when(einladungsService).nimmAn("valid-token");

        mockMvc.perform(post("/einladung/annehmen")
                        .with(csrf())
                        .param("token", "valid-token"))
                .andExpect(status().isOk())
                .andExpect(view().name("einladung-erfolg"));

        verify(einladungsService).nimmAn("valid-token");
    }

    /** EINL-06: POST mit ungültigem Token → 400 (error.html). */
    @Test
    void postAnnehmenMitUngueltigemTokenGibtFehler() throws Exception {
        doThrow(new IllegalArgumentException("Einladungs-Token ist ungültig"))
                .when(einladungsService).nimmAn("bad-token");

        mockMvc.perform(post("/einladung/annehmen")
                        .with(csrf())
                        .param("token", "bad-token"))
                .andExpect(status().isBadRequest());
    }

    /** EINL-16 (M3-Fix): POST bei nicht registriertem User → Redirect zu /registrieren mit pre-filled E-Mail. */
    @Test
    void postAnnehmenWennUserNichtRegistriertRedirectsToRegistrierung() throws Exception {
        doThrow(new BenutzerNichtRegistriertException("eingeladen@example.ch"))
                .when(einladungsService).nimmAn("valid-token");

        mockMvc.perform(post("/einladung/annehmen")
                        .with(csrf())
                        .param("token", "valid-token"))
                .andExpect(status().is3xxRedirection())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers
                        .redirectedUrl("/registrieren?email=eingeladen%40example.ch&einladung=offen"));
    }
}
