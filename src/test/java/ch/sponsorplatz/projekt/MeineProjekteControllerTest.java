package ch.sponsorplatz.projekt;

import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
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

import ch.sponsorplatz.benutzer.AppUser;
import ch.sponsorplatz.benutzer.AppUserRepository;
import ch.sponsorplatz.benutzer.SponsorplatzUserDetailsService;
import ch.sponsorplatz.organisation.MitgliedschaftRepository;
import ch.sponsorplatz.organisation.Organisation;
import ch.sponsorplatz.shared.config.SecurityConfig;

/**
 * Tests für {@link MeineProjekteController}.
 * Test-IDs: MPR-01..03.
 */
@WebMvcTest(controllers = MeineProjekteController.class)
@Import(SecurityConfig.class)
@ActiveProfiles("dev")
class MeineProjekteControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private SponsorplatzUserDetailsService userDetailsService;

    @MockitoBean
    private AppUserRepository appUserRepository;

    @MockitoBean
    private MitgliedschaftRepository mitgliedschaftRepository;

    @MockitoBean
    private ProjektService projektService;

    @Test
    @DisplayName("MPR-01: /meine-projekte ohne Auth → Redirect auf Login")
    void anonymRedirect() throws Exception {
        mockMvc.perform(get("/meine-projekte"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
    }

    @Test
    @WithMockUser("user@test.ch")
    @DisplayName("MPR-02: eingeloggter User sieht Projekte seiner Orgs")
    void eingeloggtZeigtProjekte() throws Exception {
        AppUser user = new AppUser();
        user.setId(UUID.randomUUID());
        user.setEmail("user@test.ch");
        UUID orgA = UUID.randomUUID();
        UUID orgB = UUID.randomUUID();
        when(appUserRepository.findByEmail("user@test.ch")).thenReturn(Optional.of(user));
        when(mitgliedschaftRepository.findOrgIdsByUserId(user.getId()))
                .thenReturn(List.of(orgA, orgB));

        Organisation org = new Organisation();
        org.setId(orgA);
        org.setName("FC Test");
        org.setSlug("fc-test");
        Projekt projekt = new Projekt();
        projekt.setId(UUID.randomUUID());
        projekt.setName("Sommerfest");
        projekt.setSlug("sommerfest");
        projekt.setSichtbarkeit(Sichtbarkeit.ENTWURF);
        projekt.setOrg(org);
        when(projektService.findeNachOrgIds(List.of(orgA, orgB)))
                .thenReturn(List.of(projekt));

        mockMvc.perform(get("/meine-projekte"))
                .andExpect(status().isOk())
                .andExpect(view().name("meine-projekte"))
                .andExpect(model().attributeExists("projekte"))
                .andExpect(model().attribute("aktiveSeite", "projekte"));
    }

    @Test
    @WithMockUser("ohne@test.ch")
    @DisplayName("MPR-03: User ohne Orgs sieht leere Liste — keine 500, keine Redirects")
    void ohneOrgsLeereListe() throws Exception {
        AppUser user = new AppUser();
        user.setId(UUID.randomUUID());
        user.setEmail("ohne@test.ch");
        when(appUserRepository.findByEmail("ohne@test.ch")).thenReturn(Optional.of(user));
        when(mitgliedschaftRepository.findOrgIdsByUserId(user.getId())).thenReturn(List.of());
        when(projektService.findeNachOrgIds(anyList())).thenReturn(List.of());

        mockMvc.perform(get("/meine-projekte"))
                .andExpect(status().isOk())
                .andExpect(view().name("meine-projekte"));
    }
}
