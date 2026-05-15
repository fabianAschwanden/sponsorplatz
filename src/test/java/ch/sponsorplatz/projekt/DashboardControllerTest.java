package ch.sponsorplatz.projekt;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import ch.sponsorplatz.benutzer.AppUserService;
import ch.sponsorplatz.benutzer.AppUserService.OnboardingSnapshot;
import ch.sponsorplatz.benutzer.SponsorplatzUserDetailsService;
import ch.sponsorplatz.einladung.EinladungsService;
import ch.sponsorplatz.organisation.MitgliedschaftService;
import ch.sponsorplatz.shared.config.SecurityConfig;

@WebMvcTest(controllers = DashboardController.class)
@Import(SecurityConfig.class)
@ActiveProfiles("dev")
class DashboardControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private SponsorplatzUserDetailsService userDetailsService;

    @MockitoBean
    private DashboardService dashboardService;

    @MockitoBean
    private MatchingService matchingService;

    @MockitoBean
    private ProjektService projektService;

    @MockitoBean
    private AppUserService appUserService;

    @MockitoBean
    private MitgliedschaftService mitgliedschaftService;

    @MockitoBean
    private EinladungsService einladungsService;

    /**
     * Simuliert einen User mit mindestens einer Org — damit kein
     * Onboarding-Redirect greift.
     */
    private UUID mockUserMitOrg(String email) {
        UUID userId = UUID.randomUUID();
        when(appUserService.findeOnboardingSnapshotNachEmail(email))
                .thenReturn(Optional.of(new OnboardingSnapshot(userId, false, true)));
        when(mitgliedschaftService.findeOrgIdsVonUser(userId))
                .thenReturn(List.of(UUID.randomUUID()));
        return userId;
    }

    /** DASH-01: GET /dashboard anonym → Redirect zu /login. */
    @Test
    void dashboardAnonymRedirectZuLogin() throws Exception {
        mockMvc.perform(get("/dashboard"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
    }

    /** DASH-02: GET /dashboard eingeloggt → 200 + View dashboard. */
    @Test
    @WithMockUser("user@test.ch")
    void dashboardEingeloggtIst200() throws Exception {
        mockUserMitOrg("user@test.ch");
        when(dashboardService.ladeDashboardDaten(anyString())).thenReturn(DashboardDaten.leer());

        mockMvc.perform(get("/dashboard"))
                .andExpect(status().isOk())
                .andExpect(view().name("dashboard"));
    }

    /** DASH-03: Model enthält die Dashboard-Attribute. */
    @Test
    @WithMockUser("user2@test.ch")
    void dashboardModelEnthaeltAttribute() throws Exception {
        mockUserMitOrg("user2@test.ch");
        when(dashboardService.ladeDashboardDaten(anyString()))
                .thenReturn(DashboardDaten.von(3, 5, 12, 4, java.util.List.of()));

        mockMvc.perform(get("/dashboard"))
                .andExpect(model().attributeExists(
                        "aktiveSeite",
                        "aktuellerMonat",
                        "aktuelleKw",
                        "anzahlOrganisationen",
                        "anzahlProjekte",
                        "anzahlAnfragen",
                        "anzahlOffeneAnfragen",
                        "empfehlungen",
                        "naechsteEvents",
                        "meineProjekte",
                        "offeneEinladungen"))
                .andExpect(model().attribute("anzahlOrganisationen", 3L))
                .andExpect(model().attribute("anzahlProjekte", 5L))
                .andExpect(model().attribute("anzahlAnfragen", 12L))
                .andExpect(model().attribute("anzahlOffeneAnfragen", 4L));
    }

    /** DASH-04: Werte kommen aus DashboardService.ladeDashboardDaten(email). */
    @Test
    @WithMockUser(username = "test@example.ch")
    void dashboardRuftServiceMitEmailAuf() throws Exception {
        mockUserMitOrg("test@example.ch");
        when(dashboardService.ladeDashboardDaten("test@example.ch"))
                .thenReturn(DashboardDaten.leer());

        mockMvc.perform(get("/dashboard"))
                .andExpect(status().isOk());
    }

    /** DASH-05: User ohne Mitgliedschaften und Onboarding noch nicht gesehen → Redirect auf Onboarding. */
    @Test
    @WithMockUser("neu@test.ch")
    void ohneOrgRedirectAufOnboarding() throws Exception {
        UUID userId = UUID.randomUUID();
        when(appUserService.findeOnboardingSnapshotNachEmail("neu@test.ch"))
                .thenReturn(Optional.of(new OnboardingSnapshot(userId, false, false)));
        when(mitgliedschaftService.findeOrgIdsVonUser(userId)).thenReturn(List.of());

        mockMvc.perform(get("/dashboard"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/onboarding"));
    }

    /** DASH-06: User ohne Mitgliedschaften, aber Onboarding bereits gesehen → bleibt auf Dashboard. */
    @Test
    @WithMockUser("zurueck@test.ch")
    void ohneOrgAberOnboardingGesehenBleibtAufDashboard() throws Exception {
        UUID userId = UUID.randomUUID();
        when(appUserService.findeOnboardingSnapshotNachEmail("zurueck@test.ch"))
                .thenReturn(Optional.of(new OnboardingSnapshot(userId, false, true)));
        when(mitgliedschaftService.findeOrgIdsVonUser(userId)).thenReturn(List.of());
        when(dashboardService.ladeDashboardDaten(anyString())).thenReturn(DashboardDaten.leer());

        mockMvc.perform(get("/dashboard"))
                .andExpect(status().isOk())
                .andExpect(view().name("dashboard"));
    }

    /** DASH-07: Plattform-Admin ohne Mitgliedschaften → bleibt auf Dashboard (kein Onboarding-Redirect). */
    @Test
    @WithMockUser("admin@test.ch")
    void plattformAdminOhneOrgBleibtAufDashboard() throws Exception {
        UUID adminId = UUID.randomUUID();
        when(appUserService.findeOnboardingSnapshotNachEmail("admin@test.ch"))
                .thenReturn(Optional.of(new OnboardingSnapshot(adminId, true, false)));
        when(mitgliedschaftService.findeOrgIdsVonUser(adminId)).thenReturn(List.of());
        when(dashboardService.ladeDashboardDaten(anyString())).thenReturn(DashboardDaten.leer());

        mockMvc.perform(get("/dashboard"))
                .andExpect(status().isOk())
                .andExpect(view().name("dashboard"));
    }
}
