package ch.sponsorplatz.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import java.util.Collections;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import ch.sponsorplatz.shared.config.SecurityConfig;
import ch.sponsorplatz.model.AnfrageStatus;
import ch.sponsorplatz.model.AppUser;
import ch.sponsorplatz.model.Branche;
import ch.sponsorplatz.model.Nachricht;
import ch.sponsorplatz.model.OrgTyp;
import ch.sponsorplatz.model.Organisation;
import ch.sponsorplatz.model.SponsoringAnfrage;
import ch.sponsorplatz.model.SponsoringPaket;
import ch.sponsorplatz.repository.SponsoringAnfrageRepository;
import ch.sponsorplatz.service.AccessControl;
import ch.sponsorplatz.service.AppUserService;
import ch.sponsorplatz.service.NachrichtService;
import ch.sponsorplatz.service.OrganisationService;
import ch.sponsorplatz.service.SponsorplatzUserDetailsService;

/**
 * Tests für NachrichtController (MSG-05..08).
 */
@WebMvcTest(controllers = NachrichtController.class)
@Import(SecurityConfig.class)
@ActiveProfiles("dev")
class NachrichtControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private NachrichtService nachrichtService;
    @MockBean
    private OrganisationService organisationService;
    @MockBean
    private SponsoringAnfrageRepository anfrageRepository;
    @MockBean
    private AppUserService appUserService;
    @MockBean
    private AccessControl accessControl;
    @MockBean
    private SponsorplatzUserDetailsService userDetailsService;

    private static final String ORG_SLUG = "test-verein";
    private static final UUID ANFRAGE_ID = UUID.randomUUID();

    private Organisation testOrg() {
        Organisation org = new Organisation();
        org.setId(UUID.randomUUID());
        org.setSlug(ORG_SLUG);
        org.setName("Test Verein");
        org.setTyp(OrgTyp.VEREIN);
        org.setBranche(Branche.SPORT);
        return org;
    }

    private SponsoringAnfrage testAnfrage() {
        Organisation org = testOrg();
        SponsoringPaket paket = new SponsoringPaket();
        paket.setName("Gold");

        SponsoringAnfrage anfrage = new SponsoringAnfrage();
        anfrage.setId(ANFRAGE_ID);
        anfrage.setStatus(AnfrageStatus.ANGENOMMEN);
        anfrage.setAnfragenderOrg(org);
        anfrage.setEmpfaengerOrg(org);
        anfrage.setPaket(paket);
        anfrage.setNachricht("Hallo");
        anfrage.setKontaktName("Max");
        return anfrage;
    }

    /** MSG-05: GET .../nachrichten mit Edit-Recht → 200. */
    @Test
    @WithMockUser(username = "user@test.ch")
    @DisplayName("MSG-05: GET nachrichten mit Edit-Recht → 200 + Thread-Ansicht")
    void threadAnzeigen_mitRecht() throws Exception {
        when(accessControl.kannOrgEditierenNachSlug(eq(ORG_SLUG), any())).thenReturn(true);
        when(organisationService.findeNachSlug(ORG_SLUG)).thenReturn(Optional.of(testOrg()));
        when(anfrageRepository.findById(ANFRAGE_ID)).thenReturn(Optional.of(testAnfrage()));
        when(nachrichtService.findeNachAnfrage(ANFRAGE_ID)).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/organisationen/" + ORG_SLUG + "/anfragen/" + ANFRAGE_ID + "/nachrichten"))
                .andExpect(status().isOk())
                .andExpect(view().name("nachrichten-thread"))
                .andExpect(model().attributeExists("nachrichten", "anfrage", "org"));
    }

    /** MSG-06: GET .../nachrichten ohne Recht → 403. */
    @Test
    @WithMockUser(username = "fremd@test.ch")
    @DisplayName("MSG-06: GET nachrichten ohne Recht → 403")
    void threadAnzeigen_ohneRecht() throws Exception {
        when(accessControl.kannOrgEditierenNachSlug(eq(ORG_SLUG), any())).thenReturn(false);

        mockMvc.perform(get("/organisationen/" + ORG_SLUG + "/anfragen/" + ANFRAGE_ID + "/nachrichten"))
                .andExpect(status().isForbidden());
    }

    /** MSG-07: POST .../nachrichten mit gültigem Text → 302 Redirect. */
    @Test
    @WithMockUser(username = "user@test.ch")
    @DisplayName("MSG-07: POST nachrichten mit Text → 302 Redirect")
    void nachrichtSenden_erfolgreich() throws Exception {
        when(accessControl.kannOrgEditierenNachSlug(eq(ORG_SLUG), any())).thenReturn(true);
        AppUser user = new AppUser();
        user.setId(UUID.randomUUID());
        when(appUserService.findeNachEmail("user@test.ch")).thenReturn(Optional.of(user));
        when(nachrichtService.sendeNachricht(any(), any(), any())).thenReturn(new Nachricht());

        mockMvc.perform(post("/organisationen/" + ORG_SLUG + "/anfragen/" + ANFRAGE_ID + "/nachrichten")
                .param("text", "Meine Antwort")
                .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/organisationen/" + ORG_SLUG + "/anfragen/" + ANFRAGE_ID + "/nachrichten"));
    }

    /** MSG-08: POST .../nachrichten mit leerem Text → Redirect mit Fehler. */
    @Test
    @WithMockUser(username = "user@test.ch")
    @DisplayName("MSG-08: POST nachrichten mit leerem Text → Redirect mit Fehlermeldung")
    void nachrichtSenden_leererText() throws Exception {
        when(accessControl.kannOrgEditierenNachSlug(eq(ORG_SLUG), any())).thenReturn(true);
        AppUser user = new AppUser();
        user.setId(UUID.randomUUID());
        when(appUserService.findeNachEmail("user@test.ch")).thenReturn(Optional.of(user));
        when(nachrichtService.sendeNachricht(any(), any(), eq("")))
                .thenThrow(new IllegalArgumentException("Nachricht darf nicht leer sein"));

        mockMvc.perform(post("/organisationen/" + ORG_SLUG + "/anfragen/" + ANFRAGE_ID + "/nachrichten")
                .param("text", "")
                .with(csrf()))
                .andExpect(status().is3xxRedirection());
    }
}
