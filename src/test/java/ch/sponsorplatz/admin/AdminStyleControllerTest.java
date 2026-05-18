package ch.sponsorplatz.admin;

import ch.sponsorplatz.benutzer.SponsorplatzUserDetailsService;
import ch.sponsorplatz.shared.config.SecurityConfig;
import ch.sponsorplatz.shared.einstellungen.PlattformEinstellungenService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Tests für {@link AdminStyleController}. Test-IDs: ADM-STYLE-01..04.
 */
@WebMvcTest(controllers = AdminStyleController.class)
@Import(SecurityConfig.class)
@ActiveProfiles("dev")
class AdminStyleControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean private SponsorplatzUserDetailsService userDetailsService;
    @MockitoBean private PlattformEinstellungenService einstellungenService;

    @Test
    @WithMockUser(username = "user@example.ch")
    @DisplayName("ADM-STYLE-01: GET /admin/style ohne PLATFORM_ADMIN → 403")
    void getOhneAdminGibt403() throws Exception {
        mockMvc.perform(get("/admin/style"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "admin@example.ch", roles = "PLATFORM_ADMIN")
    @DisplayName("ADM-STYLE-02: GET /admin/style als Admin → Template + aktueller Style im Model")
    void getAlsAdminZeigtTemplate() throws Exception {
        when(einstellungenService.ladeAktivenStyle()).thenReturn("css-ch");

        mockMvc.perform(get("/admin/style"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/style"))
                .andExpect(model().attribute("aktiverStyle", "css-ch"))
                .andExpect(model().attributeExists("verfuegbareStyles"));
    }

    @Test
    @WithMockUser(username = "admin@example.ch", roles = "PLATFORM_ADMIN")
    @DisplayName("ADM-STYLE-03: POST speichern Happy-Path → setzt Style + Redirect")
    void postHappyPathSetztStyle() throws Exception {
        mockMvc.perform(post("/admin/style/speichern")
                        .param("style", "css-ch")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/style"));

        verify(einstellungenService).setzeAktivenStyle(eq("css-ch"), eq("admin@example.ch"));
    }

    @Test
    @WithMockUser(username = "admin@example.ch", roles = "PLATFORM_ADMIN")
    @DisplayName("ADM-STYLE-04: POST mit ungültigem Style → Fehler-Flash, Redirect ohne Speichern-Effekt")
    void postUngueltigerStyleZeigtFehler() throws Exception {
        doThrow(new IllegalArgumentException("Ungültiger Style: foobar"))
                .when(einstellungenService).setzeAktivenStyle(eq("foobar"), any());

        mockMvc.perform(post("/admin/style/speichern")
                        .param("style", "foobar")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/style"))
                .andExpect(flash().attribute("fehlermeldung", "Ungültiger Style: foobar"));
    }
}
