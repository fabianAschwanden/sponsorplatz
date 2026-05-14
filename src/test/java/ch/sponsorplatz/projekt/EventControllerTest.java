package ch.sponsorplatz.projekt;

import ch.sponsorplatz.organisation.AccessControl;
import ch.sponsorplatz.organisation.Organisation;
import ch.sponsorplatz.organisation.OrganisationService;
import ch.sponsorplatz.shared.config.SecurityConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = EventController.class)
@Import(SecurityConfig.class)
@ActiveProfiles("dev")
class EventControllerTest {

    @Autowired private MockMvc mockMvc;
    @MockitoBean private EventService eventService;
    @MockitoBean private OrganisationService organisationService;
    @MockitoBean private AccessControl accessControl;

    @Test
    @DisplayName("EVT-02: POST .../events/speichern ohne Edit-Recht -> 403")
    @WithMockUser(username = "user@test.ch")
    void speichernOhneRechtVerboten() throws Exception {
        Organisation org = new Organisation();
        org.setId(UUID.randomUUID());
        org.setSlug("fc-test");
        when(organisationService.findeNachSlug("fc-test")).thenReturn(Optional.of(org));
        when(accessControl.kannOrgEditieren(any(), any())).thenReturn(false);

        mockMvc.perform(post("/organisationen/fc-test/events/speichern")
                        .param("name", "Sommerfest")
                        .param("datum", "2026-07-15")
                        .with(csrf()))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("EVT-02b: POST .../events/speichern mit Edit-Recht -> 302 Redirect")
    @WithMockUser(username = "user@test.ch")
    void speichernMitRechtErlaubt() throws Exception {
        Organisation org = new Organisation();
        org.setId(UUID.randomUUID());
        org.setSlug("fc-test");
        when(organisationService.findeNachSlug("fc-test")).thenReturn(Optional.of(org));
        when(accessControl.kannOrgEditieren(any(), any())).thenReturn(true);

        mockMvc.perform(post("/organisationen/fc-test/events/speichern")
                        .param("name", "Sommerfest")
                        .param("datum", "2026-07-15")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection());
    }
}

