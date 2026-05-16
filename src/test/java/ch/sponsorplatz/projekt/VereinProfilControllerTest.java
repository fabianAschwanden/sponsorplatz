package ch.sponsorplatz.projekt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import ch.sponsorplatz.benutzer.SponsorplatzUserDetailsService;
import ch.sponsorplatz.organisation.Branche;
import ch.sponsorplatz.organisation.OrgTyp;
import ch.sponsorplatz.organisation.Organisation;
import ch.sponsorplatz.organisation.OrganisationService;
import ch.sponsorplatz.organisation.OrganisationView;
import ch.sponsorplatz.shared.config.SecurityConfig;
import ch.sponsorplatz.shared.exception.GlobalExceptionHandler;
import org.springframework.security.test.context.support.WithMockUser;

@WebMvcTest(controllers = VereinProfilController.class)
@Import({ SecurityConfig.class, GlobalExceptionHandler.class })
@ActiveProfiles("dev")
@WithMockUser
class VereinProfilControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private OrganisationService orgService;

    @MockitoBean
    private ProjektService projektService;

    @MockitoBean
    private SponsorplatzUserDetailsService userDetailsService;

    /** VP-01: Vereinsprofil ist öffentlich erreichbar. */
    @Test
    void profilIstPublic() throws Exception {
        Organisation org = new Organisation();
        org.setId(UUID.randomUUID());
        org.setName("FC Muster");
        org.setSlug("fc-muster");
        org.setTyp(OrgTyp.VEREIN);

        when(orgService.findeViewNachSlug("fc-muster")).thenReturn(Optional.of(OrganisationView.von(org)));
        when(projektService.findeNachOrg(org.getId())).thenReturn(List.of());

        mockMvc.perform(get("/vereine/fc-muster"))
                .andExpect(status().isOk())
                .andExpect(view().name("projekt/verein-profil"))
                .andExpect(model().attributeExists("org", "projekte"));
    }

    /** VP-02: Unbekannter Verein → 404. */
    @Test
    void unbekannterVereinIst404() throws Exception {
        when(orgService.findeViewNachSlug("gibts-nicht")).thenReturn(Optional.empty());

        mockMvc.perform(get("/vereine/gibts-nicht"))
                .andExpect(status().isNotFound());
    }

    /**
     * VP-03: Branche-Chip wird im Model als Teil der OrganisationView gerendert.
     */
    @Test
    void brancheChipImProfil() throws Exception {
        Organisation org = new Organisation();
        org.setId(UUID.randomUUID());
        org.setName("FC Sportverein");
        org.setSlug("fc-sportverein");
        org.setTyp(OrgTyp.VEREIN);
        org.setBranche(Branche.SPORT);

        when(orgService.findeViewNachSlug("fc-sportverein")).thenReturn(Optional.of(OrganisationView.von(org)));
        when(projektService.findeNachOrg(org.getId())).thenReturn(List.of());

        mockMvc.perform(get("/vereine/fc-sportverein"))
                .andExpect(status().isOk())
                .andExpect(view().name("projekt/verein-profil"))
                .andExpect(result -> {
                    var mv = result.getModelAndView();
                    assertThat(mv).isNotNull();
                    OrganisationView view = (OrganisationView) mv.getModel().get("org");
                    assertThat(view).isNotNull();
                    assertThat(view.branche()).isEqualTo(Branche.SPORT);
                    assertThat(view.branche().getAnzeige()).isEqualTo("Sport");
                });
    }

    /** VP-04: Branche-Beschreibung ist als Subhead-Text verfügbar. */
    @Test
    void brancheBeschreibungAlsSubhead() throws Exception {
        Organisation org = new Organisation();
        org.setId(UUID.randomUUID());
        org.setName("Reha Zentrum");
        org.setSlug("reha-zentrum");
        org.setTyp(OrgTyp.VEREIN);
        org.setBranche(Branche.REHA);

        when(orgService.findeViewNachSlug("reha-zentrum")).thenReturn(Optional.of(OrganisationView.von(org)));
        when(projektService.findeNachOrg(org.getId())).thenReturn(List.of());

        mockMvc.perform(get("/vereine/reha-zentrum"))
                .andExpect(status().isOk())
                .andExpect(result -> {
                    var mv = result.getModelAndView();
                    assertThat(mv).isNotNull();
                    OrganisationView view = (OrganisationView) mv.getModel().get("org");
                    assertThat(view.branche().getBeschreibung())
                            .as("Subhead enthält Branche-Beschreibung")
                            .contains("Rehabilitation");
                });
    }

    /**
     * VP-05: Render-Assertion — der Branche-Chip kommt mit der CSS-Klasse
     * {@code health-hero-chip} im HTML an und verlinkt auf den Marktplatz-Filter.
     * Defense gegen Template-Regression: das Model kann korrekt sein während
     * das Template fehlerhaft Branche überhaupt nicht rendert.
     */
    @Test
    void heroChipImGerendertenHtml() throws Exception {
        Organisation org = new Organisation();
        org.setId(UUID.randomUUID());
        org.setName("FC Sportverein");
        org.setSlug("fc-sportverein");
        org.setTyp(OrgTyp.VEREIN);
        org.setBranche(Branche.SPORT);

        when(orgService.findeViewNachSlug("fc-sportverein")).thenReturn(Optional.of(OrganisationView.von(org)));
        when(projektService.findeNachOrg(org.getId())).thenReturn(List.of());

        mockMvc.perform(get("/vereine/fc-sportverein"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("health-hero-chip")))
                .andExpect(content().string(containsString("/marktplatz?branche=SPORT")))
                .andExpect(content().string(containsString(">Sport<")))
                // health-subhead enthält die Branche-Beschreibung
                .andExpect(content().string(containsString("class=\"health-subhead\"")));
    }

    /**
     * VP-06: Render-Assertion — Branche erscheint NICHT mehr doppelt in der
     * &lt;dl&gt;-Detail-Liste. Verhindert, dass jemand die alte
     * {@code
     * <dt>Branche</dt>}-Zeile versehentlich wieder einfügt.
     */
    @Test
    void brancheNichtDoppeltImDetail() throws Exception {
        Organisation org = new Organisation();
        org.setId(UUID.randomUUID());
        org.setName("FC Sportverein");
        org.setSlug("fc-sportverein");
        org.setTyp(OrgTyp.VEREIN);
        org.setBranche(Branche.SPORT);

        when(orgService.findeViewNachSlug("fc-sportverein")).thenReturn(Optional.of(OrganisationView.von(org)));
        when(projektService.findeNachOrg(org.getId())).thenReturn(List.of());

        mockMvc.perform(get("/vereine/fc-sportverein"))
                .andExpect(status().isOk())
                .andExpect(content().string(not(containsString("<dt>Branche</dt>"))));
    }
}
