package ch.sponsorplatz.anfrage;

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
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import ch.sponsorplatz.shared.config.SecurityConfig;
import ch.sponsorplatz.benutzer.AppUser;
import ch.sponsorplatz.organisation.Branche;
import ch.sponsorplatz.organisation.OrgTyp;
import ch.sponsorplatz.organisation.Organisation;
import ch.sponsorplatz.projekt.SponsoringPaket;
import ch.sponsorplatz.organisation.AccessControl;
import ch.sponsorplatz.benutzer.AppUserService;
import ch.sponsorplatz.organisation.OrganisationService;
import ch.sponsorplatz.benutzer.SponsorplatzUserDetailsService;

/**
 * Tests für NachrichtController (MSG-05..08).
 */
@WebMvcTest(controllers = NachrichtController.class)
@Import(SecurityConfig.class)
@ActiveProfiles("dev")
class NachrichtControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private NachrichtService nachrichtService;
    @MockitoBean
    private OrganisationService organisationService;
    @MockitoBean
    private SponsoringAnfrageService anfrageService;
    @MockitoBean
    private AppUserService appUserService;
    @MockitoBean
    private AccessControl accessControl;
    @MockitoBean
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
        when(organisationService.findeViewNachSlug(ORG_SLUG))
                .thenReturn(Optional.of(ch.sponsorplatz.organisation.OrganisationView.von(testOrg())));
        when(anfrageService.findeViewNachId(ANFRAGE_ID)).thenReturn(AnfrageView.von(testAnfrage()));
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
