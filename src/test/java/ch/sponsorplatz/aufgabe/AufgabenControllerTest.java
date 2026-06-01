package ch.sponsorplatz.aufgabe;

import ch.sponsorplatz.shared.config.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * AUFG-CTRL-01..02 — „Meine Aufgaben"-Liste mit neuer Listen-UX (Zähler/Suche/Sort/Pager).
 */
@WebMvcTest(controllers = AufgabenController.class)
@Import(SecurityConfig.class)
@ActiveProfiles("dev")
class AufgabenControllerTest {

    @Autowired private MockMvc mockMvc;
    @MockitoBean private AufgabenService aufgabenService;
    @MockitoBean private ch.sponsorplatz.benutzer.SponsorplatzUserDetailsService userDetailsService;

    /** AUFG-CTRL-01: Liste rendert mit Toolbar/Zähler/Sort. */
    @Test
    @WithMockUser
    void listeListenUx() throws Exception {
        when(aufgabenService.meineOffenen("user")).thenReturn(List.of(aufgabe("Alpha"), aufgabe("Zeta")));

        mockMvc.perform(get("/aufgaben"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("liste-anzahl")))
                .andExpect(content().string(containsString("liste-sort")));
    }

    /** AUFG-CTRL-02: Sortierung nach Titel absteigend server-seitig. */
    @Test
    @WithMockUser
    void sortTitelAbsteigend() throws Exception {
        when(aufgabenService.meineOffenen("user")).thenReturn(List.of(aufgabe("Alpha"), aufgabe("Zeta")));

        var html = mockMvc.perform(get("/aufgaben").param("sort", "titel").param("dir", "desc"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertThat(html.indexOf("Zeta")).isLessThan(html.indexOf("Alpha"));
    }

    private Aufgabe aufgabe(String titel) {
        Aufgabe a = new Aufgabe();
        a.setId(UUID.randomUUID());
        a.setTitel(titel);
        a.setStatus(AufgabenStatus.OFFEN);
        a.setNurPlatformAdmin(true);
        a.setErstelltAm(Instant.now());
        return a;
    }
}
