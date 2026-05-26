package ch.sponsorplatz.admin;

import ch.sponsorplatz.audit.AuditAktion;
import ch.sponsorplatz.audit.AuditService;
import ch.sponsorplatz.benutzer.AdminBenutzerView;
import ch.sponsorplatz.benutzer.AppUserService;
import ch.sponsorplatz.benutzer.PlatformRolle;
import ch.sponsorplatz.benutzer.SponsorplatzUserDetailsService;
import ch.sponsorplatz.benutzer.TwoFaService;
import ch.sponsorplatz.organisation.OrganisationService;
import ch.sponsorplatz.shared.config.SecurityConfig;
import ch.sponsorplatz.shared.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.DisplayName;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Controller-Tests für {@link AdminBenutzerController}.
 * Test-IDs: AUSER-01..06
 */
@WebMvcTest(controllers = AdminBenutzerController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class})
@ActiveProfiles("dev")
class AdminBenutzerControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AppUserService appUserService;

    @MockitoBean
    private OrganisationService organisationService;

    @MockitoBean
    private AuditService auditService;

    @MockitoBean
    private TwoFaService twoFaService;

    @MockitoBean
    private SponsorplatzUserDetailsService userDetailsService;

    private static final UUID USER_ID = UUID.randomUUID();

    private AdminBenutzerView testView() {
        return new AdminBenutzerView(
                USER_ID, "test@example.ch", "Test User",
                PlatformRolle.PLATFORM_ADMIN, true, true,
                Instant.now(), null, false,
                true, java.util.List.of()
        );
    }

    @Test
    @WithMockUser(roles = "PLATFORM_ADMIN")
    @DisplayName("AUSER-01: GET /admin/benutzer zeigt Benutzerliste")
    void benutzerListe() throws Exception {
        when(appUserService.findeAlleAdminViews()).thenReturn(List.of(testView()));

        mockMvc.perform(get("/admin/benutzer"))
                .andExpect(status().isOk())
                .andExpect(model().attributeExists("benutzer", "rollen"))
                .andExpect(view().name("admin/benutzer"));
    }

    @Test
    @WithMockUser(roles = "PLATFORM_ADMIN")
    @DisplayName("AUSER-02: POST /admin/benutzer/{id}/sperren deaktiviert User + Audit")
    void benutzerSperren() throws Exception {
        when(appUserService.setzeAktiv(USER_ID, false)).thenReturn(testView());

        mockMvc.perform(post("/admin/benutzer/{id}/sperren", USER_ID).with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attributeExists("erfolgsMeldung"));

        verify(appUserService).setzeAktiv(USER_ID, false);
        verify(auditService).protokolliere(eq(AuditAktion.GESPERRT), eq("BENUTZER"),
                eq(USER_ID), eq("AppUser"), eq("test@example.ch"));
    }

    @Test
    @WithMockUser(roles = "PLATFORM_ADMIN")
    @DisplayName("AUSER-03: POST /admin/benutzer/{id}/entsperren aktiviert User + Audit")
    void benutzerEntsperren() throws Exception {
        when(appUserService.setzeAktiv(USER_ID, true)).thenReturn(testView());

        mockMvc.perform(post("/admin/benutzer/{id}/entsperren", USER_ID).with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attributeExists("erfolgsMeldung"));

        verify(appUserService).setzeAktiv(USER_ID, true);
        verify(auditService).protokolliere(eq(AuditAktion.ENTSPERRT), eq("BENUTZER"),
                eq(USER_ID), eq("AppUser"), eq("test@example.ch"));
    }

    @Test
    @WithMockUser(roles = "PLATFORM_ADMIN")
    @DisplayName("AUSER-04: POST /admin/benutzer/{id}/rolle ändert Platform-Rolle + Audit")
    void rolleAendern() throws Exception {
        when(appUserService.findeAdminViewNachId(USER_ID)).thenReturn(testView());
        when(appUserService.setzePlatformRolle(eq(USER_ID), any())).thenReturn(testView());

        mockMvc.perform(post("/admin/benutzer/{id}/rolle", USER_ID)
                        .param("rolle", "PLATFORM_ADMIN")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attributeExists("erfolgsMeldung"));

        verify(appUserService).setzePlatformRolle(USER_ID, PlatformRolle.PLATFORM_ADMIN);
        // Audit-Detail: "<alteRolle> → <neueRolle>". testView() ist PLATFORM_ADMIN,
        // also bleibt der String identisch — der Test deckt trotzdem ab, dass
        // die Audit-Protokollierung getriggert wird.
        verify(auditService).protokolliere(eq(AuditAktion.ROLLE_GEAENDERT), eq("BENUTZER"),
                eq(USER_ID), eq("AppUser"), eq("PLATFORM_ADMIN → PLATFORM_ADMIN"));
    }

    @Test
    @WithMockUser(roles = "PLATFORM_ADMIN")
    @DisplayName("AUSER-05: GET /admin/organisationen ruft OrganisationService.alle()")
    void orgListe() throws Exception {
        when(organisationService.alle()).thenReturn(List.of());

        mockMvc.perform(get("/admin/organisationen"))
                .andExpect(status().isOk())
                .andExpect(model().attributeExists("organisationen"))
                .andExpect(view().name("admin/organisationen"));

        // Pinnt den Service-Call: falls jemand auf alleViews() migriert (ARCH-02-
        // konformer), schlägt der Test laut fehl statt still grün zu bleiben
        // (Mockito default = null für nicht-gemockte Methoden).
        verify(organisationService).alle();
    }

    @Test
    @WithMockUser
    @DisplayName("AUSER-06: Nicht-Admin bekommt 403")
    void nichtAdminVerboten() throws Exception {
        mockMvc.perform(get("/admin/benutzer"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "PLATFORM_ADMIN")
    @DisplayName("AUSER-07: POST /admin/benutzer/{id}/2fa-reset ruft TwoFaService + Audit, Flash-Erfolg")
    void zweiFaktorZuruecksetzenErfolgreich() throws Exception {
        when(twoFaService.adminResetFuerUser(USER_ID))
                .thenReturn(java.util.Optional.of(new TwoFaService.AdminResetErgebnis("ziel@sp.ch", true)));

        mockMvc.perform(post("/admin/benutzer/" + USER_ID + "/2fa-reset").with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/benutzer"))
                .andExpect(flash().attributeExists("erfolgsMeldung"));

        verify(twoFaService).adminResetFuerUser(USER_ID);
        verify(auditService).protokolliere(
                eq(AuditAktion.TOTP_RECOVERY_DURCH_ADMIN), eq("BENUTZER"),
                eq(USER_ID), eq("AppUser"), any());
    }

    @Test
    @WithMockUser(roles = "PLATFORM_ADMIN")
    @DisplayName("AUSER-08: 2FA-Reset für unbekannten User → Fehler-Flash, kein Audit")
    void zweiFaktorZuruecksetzenUnbekannt() throws Exception {
        when(twoFaService.adminResetFuerUser(USER_ID)).thenReturn(java.util.Optional.empty());

        mockMvc.perform(post("/admin/benutzer/" + USER_ID + "/2fa-reset").with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/benutzer"))
                .andExpect(flash().attributeExists("fehlermeldung"));

        verify(auditService, org.mockito.Mockito.never()).protokolliere(
                eq(AuditAktion.TOTP_RECOVERY_DURCH_ADMIN), any(), any(), any(), any());
    }
}

