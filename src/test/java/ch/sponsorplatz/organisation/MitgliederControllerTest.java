package ch.sponsorplatz.organisation;

import ch.sponsorplatz.shared.config.SecurityConfig;
import ch.sponsorplatz.benutzer.AppUser;
import ch.sponsorplatz.benutzer.AppUserRepository;
import ch.sponsorplatz.benutzer.AppUserService;
import ch.sponsorplatz.benutzer.SponsorplatzUserDetailsService;
import ch.sponsorplatz.einladung.EinladungsService;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = MitgliederController.class)
@Import(SecurityConfig.class)
@ActiveProfiles("dev")
class MitgliederControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private OrganisationService organisationService;

    @MockitoBean
    private MitgliedschaftService mitgliedschaftService;

    @MockitoBean
    private AppUserService appUserService;

    @MockitoBean
    private AppUserRepository appUserRepository;

    @MockitoBean
    private EinladungsService einladungsService;

    @MockitoBean
    private SponsorplatzUserDetailsService userDetailsService;

    @MockitoBean
    private AccessControl accessControl;

    /** MGCTRL-01: GET /organisationen/{slug}/mitglieder anonym → Redirect zu /login. */
    @Test
    void mitgliederAnonymRedirectZuLogin() throws Exception {
        mockMvc.perform(get("/organisationen/fc-test/mitglieder"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrlPattern("**/login"));
    }

    /** MGCTRL-02: GET /organisationen/{slug}/mitglieder ohne Edit-Recht → 403. */
    @Test
    @WithMockUser
    void mitgliederOhneEditRechtIst403() throws Exception {
        when(accessControl.kannOrgEditierenNachSlug(eq("fc-test"), any())).thenReturn(false);

        mockMvc.perform(get("/organisationen/fc-test/mitglieder"))
            .andExpect(status().isForbidden());
    }

    /** MGCTRL-03: POST .../hinzufuegen ohne Verwalten-Recht → 403. */
    @Test
    @WithMockUser
    void hinzufuegenOhneVerwaltenRechtIst403() throws Exception {
        when(accessControl.kannOrgVerwaltenNachSlug(eq("fc-test"), any())).thenReturn(false);

        mockMvc.perform(post("/organisationen/fc-test/mitglieder/hinzufuegen")
                .with(csrf())
                .param("email", "user@example.com")
                .param("rolle", "ORG_EDITOR"))
            .andExpect(status().isForbidden());
    }

    /** MGCTRL-04: POST .../{id}/entfernen ohne Verwalten-Recht → 403. */
    @Test
    @WithMockUser
    void entfernenOhneVerwaltenRechtIst403() throws Exception {
        when(accessControl.kannOrgVerwaltenNachSlug(eq("fc-test"), any())).thenReturn(false);
        UUID mitgliedschaftId = UUID.randomUUID();

        mockMvc.perform(post("/organisationen/fc-test/mitglieder/" + mitgliedschaftId + "/entfernen")
                .with(csrf()))
            .andExpect(status().isForbidden());
    }

    /**
     * MGCTRL-05: POST .../hinzufuegen mit existierender E-Mail
     * → Mitgliedschaft wird angelegt, KEINE Einladung versendet.
     */
    @Test
    @WithMockUser(username = "owner@example.com")
    void hinzufuegenMitExistierenderEmail() throws Exception {
        when(accessControl.kannOrgVerwaltenNachSlug(eq("fc-test"), any())).thenReturn(true);
        Organisation org = testOrg();
        when(organisationService.findeNachSlug("fc-test")).thenReturn(Optional.of(org));
        AppUser eingeladener = new AppUser();
        eingeladener.setId(UUID.randomUUID());
        eingeladener.setEmail("user@example.com");
        eingeladener.setAnzeigename("User");
        when(appUserService.findeNachEmail("user@example.com")).thenReturn(Optional.of(eingeladener));

        mockMvc.perform(post("/organisationen/fc-test/mitglieder/hinzufuegen")
                .with(csrf())
                .param("email", "user@example.com")
                .param("rolle", "ORG_EDITOR"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/organisationen/fc-test/mitglieder"));

        verify(mitgliedschaftService).fuegeHinzu(eq(org.getId()), eq(eingeladener.getId()), eq(Rolle.ORG_EDITOR), any());
        verify(einladungsService, never()).erstelleEinladung(any(), any(), any(), any());
    }

    /**
     * MGCTRL-06: POST .../hinzufuegen mit unbekannter E-Mail
     * → Einladung wird erstellt, KEINE Mitgliedschaft, Redirect zurück auf die Liste.
     */
    @Test
    @WithMockUser(username = "owner@example.com")
    void hinzufuegenMitUnbekannterEmailErzeugtEinladung() throws Exception {
        when(accessControl.kannOrgVerwaltenNachSlug(eq("fc-test"), any())).thenReturn(true);
        Organisation org = testOrg();
        when(organisationService.findeNachSlug("fc-test")).thenReturn(Optional.of(org));
        when(appUserService.findeNachEmail("neu@example.com")).thenReturn(Optional.empty());
        AppUser owner = new AppUser();
        owner.setId(UUID.randomUUID());
        owner.setEmail("owner@example.com");
        when(appUserRepository.findByEmail("owner@example.com")).thenReturn(Optional.of(owner));

        mockMvc.perform(post("/organisationen/fc-test/mitglieder/hinzufuegen")
                .with(csrf())
                .param("email", "neu@example.com")
                .param("rolle", "ORG_VIEWER"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/organisationen/fc-test/mitglieder"));

        verify(einladungsService).erstelleEinladung(eq(org.getId()), eq("neu@example.com"),
                eq(Rolle.ORG_VIEWER), eq(owner.getId()));
        verify(mitgliedschaftService, never()).fuegeHinzu(any(), any(), any(), any());
    }

    private Organisation testOrg() {
        Organisation org = new Organisation();
        org.setId(UUID.randomUUID());
        org.setName("FC Test");
        org.setSlug("fc-test");
        org.setTyp(OrgTyp.VEREIN);
        return org;
    }
}
