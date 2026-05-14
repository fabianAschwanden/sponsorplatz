package ch.sponsorplatz.benutzer;
import ch.sponsorplatz.shared.exception.GlobalExceptionHandler;

import ch.sponsorplatz.shared.config.SecurityConfig;
import ch.sponsorplatz.audit.DatenExportService;
import ch.sponsorplatz.projekt.MedienAssetService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = EinstellungenController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class})
@ActiveProfiles("dev")
class EinstellungenControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private DatenExportService datenExportService;

    @MockitoBean
    private AppUserService appUserService;

    @MockitoBean
    private MedienAssetService medienAssetService;

    @MockitoBean
    private SponsorplatzUserDetailsService userDetailsService;

    /** EINST-01: Datenexport liefert JSON mit Content-Disposition. */
    @Test
    @WithMockUser(username = "max@example.com")
    void datenExportLiefertJson() throws Exception {
        AppUser user = new AppUser();
        user.setId(UUID.randomUUID());
        user.setEmail("max@example.com");

        when(appUserService.findeNachEmail("max@example.com")).thenReturn(Optional.of(user));
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

    /** EINST-03: POST /einstellungen/passwort → Redirect mit Erfolgsmeldung. */
    @Test
    @WithMockUser(username = "max@example.com")
    void passwortAendernErfolgreich() throws Exception {
        AppUser user = new AppUser();
        user.setId(UUID.randomUUID());
        user.setEmail("max@example.com");
        when(appUserService.findeNachEmail("max@example.com")).thenReturn(Optional.of(user));

        mockMvc.perform(post("/einstellungen/passwort")
                        .param("altesPasswort", "alt123456")
                        .param("neuesPasswort", "neu123456")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/einstellungen"))
                .andExpect(flash().attributeExists("erfolgsMeldung"));

        verify(appUserService).aenderePasswort(user.getId(), "alt123456", "neu123456");
    }
}

