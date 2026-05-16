package ch.sponsorplatz.projekt;

import ch.sponsorplatz.shared.config.SecurityConfig;
import ch.sponsorplatz.organisation.Organisation;
import ch.sponsorplatz.organisation.OrgTyp;
import ch.sponsorplatz.organisation.AccessControl;
import ch.sponsorplatz.organisation.OrganisationService;
import ch.sponsorplatz.benutzer.SponsorplatzUserDetailsService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = ProjektController.class)
@Import(SecurityConfig.class)
@ActiveProfiles("dev")
class ProjektControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ProjektService projektService;

    @MockitoBean
    private SponsoringPaketService paketService;

    @MockitoBean
    private OrganisationService orgService;

    @MockitoBean
    private SponsorplatzUserDetailsService userDetailsService;

    @MockitoBean
    private AccessControl accessControl;

    @MockitoBean
    private MedienAssetService medienAssetService;

    private Organisation testOrg() {
        Organisation org = new Organisation();
        org.setId(UUID.randomUUID());
        org.setName("FC Test");
        org.setSlug("fc-test");
        org.setTyp(OrgTyp.VEREIN);
        return org;
    }

    private Projekt testProjekt(Organisation org) {
        Projekt p = new Projekt();
        p.setId(UUID.randomUUID());
        p.setOrg(org);
        p.setName("Sommerfest 2026");
        p.setSlug("sommerfest-2026");
        p.setSichtbarkeit(Sichtbarkeit.ENTWURF);
        return p;
    }

    /** PCTRL-01: Projekt-Liste wird angezeigt. */
    @Test
    @WithMockUser
    void listeWirdAngezeigt() throws Exception {
        Organisation org = testOrg();
        when(accessControl.kannOrgEditierenNachSlug(eq("fc-test"), any())).thenReturn(true);
        when(orgService.findeViewNachSlug("fc-test")).thenReturn(Optional.of(ch.sponsorplatz.organisation.OrganisationView.von(org)));
        when(projektService.findeNachOrg(org.getId())).thenReturn(List.of());

        mockMvc.perform(get("/organisationen/fc-test/projekte"))
                .andExpect(status().isOk())
                .andExpect(view().name("projekt/projekt-liste"))
                .andExpect(model().attributeExists("projekte"));
    }

    /** PCTRL-02: Neues-Formular wird angezeigt. */
    @Test
    @WithMockUser
    void neuesFormularWirdAngezeigt() throws Exception {
        Organisation org = testOrg();
        when(accessControl.kannOrgEditierenNachSlug(eq("fc-test"), any())).thenReturn(true);
        when(orgService.findeViewNachSlug("fc-test")).thenReturn(Optional.of(ch.sponsorplatz.organisation.OrganisationView.von(org)));

        mockMvc.perform(get("/organisationen/fc-test/projekte/neu"))
                .andExpect(status().isOk())
                .andExpect(view().name("projekt/projekt-form"))
                .andExpect(model().attributeExists("projektForm"));
    }

    /** PCTRL-03: Projekt speichern redirected. */
    @Test
    @WithMockUser
    void speichernRedirected() throws Exception {
        Organisation org = testOrg();
        Projekt projekt = testProjekt(org);
        when(accessControl.kannOrgEditierenNachSlug(eq("fc-test"), any())).thenReturn(true);
        when(orgService.findeViewNachSlug("fc-test")).thenReturn(Optional.of(ch.sponsorplatz.organisation.OrganisationView.von(org)));
        when(projektService.erstelleAusFormAlsView(any(), eq("Sommerfest 2026"), any(), any(), any(), any(), any()))
                .thenReturn(ProjektView.von(projekt));

        mockMvc.perform(post("/organisationen/fc-test/projekte/speichern")
                        .with(csrf())
                        .param("name", "Sommerfest 2026")
                        .param("beschreibung", "Tolles Fest"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/organisationen/fc-test/projekte/sommerfest-2026"));
    }

    /** PCTRL-04: Detail zeigt Projekt und Pakete. */
    @Test
    @WithMockUser
    void detailZeigtProjektUndPakete() throws Exception {
        Organisation org = testOrg();
        Projekt projekt = testProjekt(org);
        when(accessControl.kannOrgEditierenNachSlug(eq("fc-test"), any())).thenReturn(true);
        when(orgService.findeViewNachSlug("fc-test")).thenReturn(Optional.of(ch.sponsorplatz.organisation.OrganisationView.von(org)));
        when(projektService.findeViewNachSlugOderWirf("sommerfest-2026")).thenReturn(ProjektView.von(projekt));
        when(paketService.findeViewsNachProjekt(projekt.getId())).thenReturn(List.of());
        when(medienAssetService.findeBilderUndAnhaengeViews(EntityTyp.PROJEKT, projekt.getId()))
                .thenReturn(new MedienAssetService.BilderUndAnhaenge(List.of(), List.of()));

        mockMvc.perform(get("/organisationen/fc-test/projekte/sommerfest-2026"))
                .andExpect(status().isOk())
                .andExpect(view().name("projekt/projekt-detail"))
                .andExpect(model().attributeExists("projekt", "pakete"));
    }

    /** PCTRL-05: Speichern mit leerem Namen zeigt Formular erneut. */
    @Test
    @WithMockUser
    void speichernMitFehlerZeigtFormular() throws Exception {
        Organisation org = testOrg();
        when(accessControl.kannOrgEditierenNachSlug(eq("fc-test"), any())).thenReturn(true);
        when(orgService.findeViewNachSlug("fc-test")).thenReturn(Optional.of(ch.sponsorplatz.organisation.OrganisationView.von(org)));

        mockMvc.perform(post("/organisationen/fc-test/projekte/speichern")
                        .with(csrf())
                        .param("name", ""))
                .andExpect(status().isOk())
                .andExpect(view().name("projekt/projekt-form"));
    }

    /** PCTRL-06: POST .../speichern ohne Edit-Recht → 403. */
    @Test
    @WithMockUser
    void speichernOhneEditRechtIst403() throws Exception {
        when(accessControl.kannOrgEditierenNachSlug(eq("fc-test"), any())).thenReturn(false);

        mockMvc.perform(post("/organisationen/fc-test/projekte/speichern")
                        .with(csrf())
                        .param("name", "Sommerfest 2026"))
                .andExpect(status().isForbidden());
    }

    /** PCTRL-07: POST .../{projektSlug}/veroeffentlichen ohne Edit-Recht → 403. */
    @Test
    @WithMockUser
    void veroeffentlichenOhneEditRechtIst403() throws Exception {
        when(accessControl.kannOrgEditierenNachSlug(eq("fc-test"), any())).thenReturn(false);

        mockMvc.perform(post("/organisationen/fc-test/projekte/sommerfest-2026/veroeffentlichen")
                        .with(csrf()))
                .andExpect(status().isForbidden());
    }

    /** PCTRL-08: POST .../{projektSlug}/pakete/speichern ohne Edit-Recht → 403. */
    @Test
    @WithMockUser
    void paketSpeichernOhneEditRechtIst403() throws Exception {
        when(accessControl.kannOrgEditierenNachSlug(eq("fc-test"), any())).thenReturn(false);

        mockMvc.perform(post("/organisationen/fc-test/projekte/sommerfest-2026/pakete/speichern")
                        .with(csrf())
                        .param("name", "Gold")
                        .param("preisChf", "1000"))
                .andExpect(status().isForbidden());
    }
}

