package ch.sponsorplatz.controller;

import ch.sponsorplatz.config.SecurityConfig;
import ch.sponsorplatz.model.AppUser;
import ch.sponsorplatz.repository.AppUserRepository;
import ch.sponsorplatz.service.DatenExportService;
import ch.sponsorplatz.service.SponsorplatzUserDetailsService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = EinstellungenController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class})
@ActiveProfiles("dev")
class EinstellungenControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private DatenExportService datenExportService;

    @MockBean
    private AppUserRepository appUserRepository;

    @MockBean
    private SponsorplatzUserDetailsService userDetailsService;

    /** EINST-01: Datenexport liefert JSON mit Content-Disposition. */
    @Test
    @WithMockUser(username = "max@example.com")
    void datenExportLiefertJson() throws Exception {
        AppUser user = new AppUser();
        user.setId(UUID.randomUUID());
        user.setEmail("max@example.com");

        when(appUserRepository.findByEmail("max@example.com")).thenReturn(Optional.of(user));
        when(datenExportService.exportiere(user.getId()))
                .thenReturn(Map.of("benutzer", Map.of("email", "max@example.com")));

        mockMvc.perform(get("/einstellungen/datenexport"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/json"))
                .andExpect(header().string("Content-Disposition", "attachment; filename=\"sponsorplatz-export.json\""))
                .andExpect(jsonPath("$.benutzer.email").value("max@example.com"));
    }

    /** EINST-02: Datenexport ohne Login → Redirect. */
    @Test
    void datenExportOhneLoginRedirected() throws Exception {
        mockMvc.perform(get("/einstellungen/datenexport"))
                .andExpect(status().is3xxRedirection());
    }
}

