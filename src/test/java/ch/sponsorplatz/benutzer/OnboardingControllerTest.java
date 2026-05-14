package ch.sponsorplatz.benutzer;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import ch.sponsorplatz.organisation.MitgliedschaftService;
import ch.sponsorplatz.organisation.Organisation;
import ch.sponsorplatz.organisation.OrganisationService;
import ch.sponsorplatz.shared.config.SecurityConfig;

/**
 * Tests für {@link OnboardingController}.
 * Test-IDs: ONB-01..05.
 */
@WebMvcTest(OnboardingController.class)
@Import(SecurityConfig.class)
@ActiveProfiles("dev")
class OnboardingControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AppUserService appUserService;
    @MockitoBean
    private MitgliedschaftService mitgliedschaftService;
    @MockitoBean
    private OrganisationService organisationService;
    @MockitoBean
    private SponsorplatzUserDetailsService userDetailsService;

    @Test
    @DisplayName("ONB-01: /onboarding ohne Auth → Redirect auf Login")
    void ohneAuthRedirect() throws Exception {
        mockMvc.perform(get("/onboarding"))
                .andExpect(status().is3xxRedirection());
    }

    @Test
    @WithMockUser("neu@test.ch")
    @DisplayName("ONB-02: /onboarding ohne Mitgliedschaften → zeigt Onboarding-Seite und markiert es als gesehen")
    void ohneOrgZeigtOnboarding() throws Exception {
        AppUser user = testUser();
        when(appUserService.findeOnboardingSnapshotNachEmail("neu@test.ch"))
                .thenReturn(Optional.of(new AppUserService.OnboardingSnapshot(user.getId(), false, false)));
        when(mitgliedschaftService.findeOrgIdsVonUser(user.getId())).thenReturn(List.of());

        mockMvc.perform(get("/onboarding"))
                .andExpect(status().isOk())
                .andExpect(view().name("onboarding"))
                .andExpect(model().attributeExists("vereinForm", "branchen"));

        verify(appUserService).markiereOnboardingGesehen(user.getId());
    }

    @Test
    @WithMockUser("admin@test.ch")
    @DisplayName("ONB-06: Plattform-Admin → /onboarding redirected aufs Dashboard, ohne Flag-Update")
    void plattformAdminWirdAufDashboardUmgeleitet() throws Exception {
        AppUser admin = testUser();
        when(appUserService.findeOnboardingSnapshotNachEmail("admin@test.ch"))
                .thenReturn(Optional.of(new AppUserService.OnboardingSnapshot(admin.getId(), true, false)));

        mockMvc.perform(get("/onboarding"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/dashboard"));

        org.mockito.Mockito.verify(appUserService, org.mockito.Mockito.never()).markiereOnboardingGesehen(org.mockito.ArgumentMatchers.any());
    }

    @Test
    @WithMockUser("zurueck@test.ch")
    @DisplayName("ONB-07: User mit Flag=true und ohne Org darf /onboarding manuell erneut aufrufen — kein zweites save")
    void onboardingErneutOhneFlagUpdate() throws Exception {
        AppUser user = testUser();
        when(appUserService.findeOnboardingSnapshotNachEmail("zurueck@test.ch"))
                .thenReturn(Optional.of(new AppUserService.OnboardingSnapshot(user.getId(), false, true)));
        when(mitgliedschaftService.findeOrgIdsVonUser(user.getId())).thenReturn(List.of());

        mockMvc.perform(get("/onboarding"))
                .andExpect(status().isOk())
                .andExpect(view().name("onboarding"));

        org.mockito.Mockito.verify(appUserService, org.mockito.Mockito.never()).markiereOnboardingGesehen(org.mockito.ArgumentMatchers.any());
    }

    @Test
    @WithMockUser("alt@test.ch")
    @DisplayName("ONB-03: /onboarding mit Mitgliedschaften → Redirect auf Dashboard")
    void mitOrgRedirectAufDashboard() throws Exception {
        AppUser user = testUser();
        when(appUserService.findeOnboardingSnapshotNachEmail("alt@test.ch"))
                .thenReturn(Optional.of(new AppUserService.OnboardingSnapshot(user.getId(), false, true)));
        when(mitgliedschaftService.findeOrgIdsVonUser(user.getId()))
                .thenReturn(List.of(UUID.randomUUID()));

        mockMvc.perform(get("/onboarding"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/dashboard"));
    }

    @Test
    @WithMockUser("neu@test.ch")
    @DisplayName("ONB-04: POST /onboarding/verein-erstellen erstellt Org + Redirect auf Dashboard")
    void vereinErstellenErfolgreich() throws Exception {
        AppUser user = testUser();
        when(appUserService.findeIdNachEmail("neu@test.ch")).thenReturn(user.getId());

        Organisation org = new Organisation();
        org.setId(UUID.randomUUID());
        org.setName("FC Test");
        org.setSlug("fc-test");
        when(organisationService.erstelleMitEigentuemerAlsView(any(), eq(user.getId())))
                .thenReturn(ch.sponsorplatz.organisation.OrganisationView.von(org));

        mockMvc.perform(post("/onboarding/verein-erstellen")
                .param("vereinName", "FC Test")
                .param("branche", "SPORT")
                .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/dashboard"));

        verify(organisationService).erstelleMitEigentuemerAlsView(any(), eq(user.getId()));
    }

    @Test
    @WithMockUser("neu@test.ch")
    @DisplayName("ONB-05: POST /onboarding/verein-erstellen ohne Name → Validierungsfehler")
    void vereinOhneNameZeigtFehler() throws Exception {
        AppUser user = testUser();
        when(appUserService.findeNachEmail("neu@test.ch")).thenReturn(Optional.of(user));
        when(mitgliedschaftService.findeOrgIdsVonUser(user.getId())).thenReturn(List.of());

        mockMvc.perform(post("/onboarding/verein-erstellen")
                .param("vereinName", "")
                .param("branche", "SPORT")
                .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(view().name("onboarding"))
                .andExpect(model().attributeHasFieldErrors("vereinForm", "vereinName"));
    }

    private AppUser testUser() {
        AppUser user = new AppUser();
        user.setId(UUID.randomUUID());
        user.setEmail("neu@test.ch");
        user.setAnzeigename("Test User");
        return user;
    }
}
