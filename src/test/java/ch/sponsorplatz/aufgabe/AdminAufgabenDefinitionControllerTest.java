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
 * AUFGDEF-CTRL-01..02 — Admin-Aufgaben-Definitionen mit leichter Listen-UX
 * (Zähler + sortierbare Spalten, server-seitig).
 */
@WebMvcTest(controllers = AdminAufgabenDefinitionController.class)
@Import(SecurityConfig.class)
@ActiveProfiles("dev")
class AdminAufgabenDefinitionControllerTest {

    @Autowired private MockMvc mockMvc;
    @MockitoBean private AufgabenDefinitionService service;
    @MockitoBean private ch.sponsorplatz.benutzer.SponsorplatzUserDetailsService userDetailsService;

    /** AUFGDEF-CTRL-01: Liste rendert mit Zähler + sortierbarem Spaltenkopf. */
    @Test
    @WithMockUser(roles = "PLATFORM_ADMIN")
    void listeListenUx() throws Exception {
        when(service.alle()).thenReturn(List.of(definition("Alpha"), definition("Zeta")));

        mockMvc.perform(get("/admin/aufgaben-definitionen"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("liste-anzahl")))
                .andExpect(content().string(containsString("liste-sort")));
    }

    /** AUFGDEF-CTRL-02: Sortierung nach Titel absteigend server-seitig. */
    @Test
    @WithMockUser(roles = "PLATFORM_ADMIN")
    void sortTitelAbsteigend() throws Exception {
        when(service.alle()).thenReturn(List.of(definition("Alpha"), definition("Zeta")));

        var html = mockMvc.perform(get("/admin/aufgaben-definitionen")
                        .param("sort", "titel").param("dir", "desc"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertThat(html.indexOf("Zeta")).isLessThan(html.indexOf("Alpha"));
    }

    private AufgabenDefinition definition(String titel) {
        AufgabenDefinition d = new AufgabenDefinition();
        d.setId(UUID.randomUUID());
        d.setTitel(titel);
        d.setTriggerEntityTyp(TriggerEntityTyp.ORG);
        d.setTriggerStatus("PENDING");
        d.setAssigneeRegel(AssigneeRegel.PLATFORM_ADMIN);
        d.setAktiv(true);
        d.setSystemDefinition(false);
        d.setErstelltAm(Instant.now());
        return d;
    }
}
