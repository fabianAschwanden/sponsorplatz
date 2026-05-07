package ch.sponsorplatz.controller;

import ch.sponsorplatz.shared.config.SecurityConfig;
import ch.sponsorplatz.model.Organisation;
import ch.sponsorplatz.model.OrgTyp;
import ch.sponsorplatz.model.Projekt;
import ch.sponsorplatz.model.Sichtbarkeit;
import ch.sponsorplatz.service.MedienAssetService;
import ch.sponsorplatz.service.ProjektService;
import ch.sponsorplatz.service.SponsorplatzUserDetailsService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = MarktplatzController.class)
@Import(SecurityConfig.class)
@ActiveProfiles("dev")
class MarktplatzControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ProjektService projektService;

    @MockBean
    private MedienAssetService medienAssetService;

    @MockBean
    private SponsorplatzUserDetailsService userDetailsService;

    private Projekt testProjekt() {
        Organisation org = new Organisation();
        org.setId(UUID.randomUUID());
        org.setName("FC Muster");
        org.setSlug("fc-muster");
        org.setTyp(OrgTyp.VEREIN);

        Projekt p = new Projekt();
        p.setId(UUID.randomUUID());
        p.setOrg(org);
        p.setName("Sommerfest");
        p.setSlug("sommerfest");
        p.setSichtbarkeit(Sichtbarkeit.OEFFENTLICH);
        p.setKategorie("Sport");
        p.setOrt("Zürich");
        return p;
    }

    /** MKT-01: Marktplatz-Seite ist öffentlich erreichbar. */
    @Test
    void marktplatzIstPublic() throws Exception {
        when(projektService.findeOeffentliche()).thenReturn(List.of());

        mockMvc.perform(get("/marktplatz"))
                .andExpect(status().isOk())
                .andExpect(view().name("marktplatz"))
                .andExpect(model().attributeExists("projekte"));
    }

    /** MKT-02: Marktplatz zeigt veröffentlichte Projekte. */
    @Test
    void marktplatzZeigtProjekte() throws Exception {
        Projekt p = testProjekt();
        when(projektService.findeOeffentliche()).thenReturn(List.of(p));

        mockMvc.perform(get("/marktplatz"))
                .andExpect(status().isOk())
                .andExpect(model().attributeExists("projekte"));
    }

    /** MKT-03: Filter nach Kategorie. */
    @Test
    void filterNachKategorie() throws Exception {
        Projekt p = testProjekt();
        when(projektService.findeOeffentliche()).thenReturn(List.of(p));

        mockMvc.perform(get("/marktplatz").param("kategorie", "Sport"))
                .andExpect(status().isOk())
                .andExpect(model().attributeExists("projekte"));

        mockMvc.perform(get("/marktplatz").param("kategorie", "Kultur"))
                .andExpect(status().isOk())
                .andExpect(model().attributeExists("projekte"));
    }

    /** MKT-04: Filter nach Ort. */
    @Test
    void filterNachOrt() throws Exception {
        Projekt p = testProjekt();
        when(projektService.findeOeffentliche()).thenReturn(List.of(p));

        mockMvc.perform(get("/marktplatz").param("ort", "Zürich"))
                .andExpect(status().isOk())
                .andExpect(model().attributeExists("projekte"));

        mockMvc.perform(get("/marktplatz").param("ort", "Bern"))
                .andExpect(status().isOk())
                .andExpect(model().attributeExists("projekte"));
    }

    /** MKT-05: Detail-Seite eines Projekts. */
    @Test
    void detailSeite() throws Exception {
        Projekt p = testProjekt();
        when(projektService.findeNachSlug("sommerfest")).thenReturn(Optional.of(p));

        mockMvc.perform(get("/marktplatz/sommerfest"))
                .andExpect(status().isOk())
                .andExpect(view().name("marktplatz-detail"))
                .andExpect(model().attributeExists("projekt"));
    }

    /** MKT-06: Volltextsuche mit Parameter q delegiert an ProjektService.suche(). */
    @Test
    void volltextSucheMitParameterQ() throws Exception {
        Projekt p = testProjekt();
        when(projektService.suche("Sommer")).thenReturn(List.of(p));

        mockMvc.perform(get("/marktplatz").param("q", "Sommer"))
                .andExpect(status().isOk())
                .andExpect(model().attributeExists("projekte", "suchbegriff"))
                .andExpect(model().attribute("suchbegriff", "Sommer"));
    }

    /** MKT-07: Leerer Suchbegriff zeigt alle öffentlichen Projekte. */
    @Test
    void leereSucheZeigtAlle() throws Exception {
        when(projektService.findeOeffentliche()).thenReturn(List.of());

        mockMvc.perform(get("/marktplatz").param("q", ""))
                .andExpect(status().isOk())
                .andExpect(model().attributeExists("projekte"));
    }
}

