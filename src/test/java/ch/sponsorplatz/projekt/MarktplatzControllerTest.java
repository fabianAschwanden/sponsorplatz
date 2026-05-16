package ch.sponsorplatz.projekt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.servlet.ModelAndView;

import ch.sponsorplatz.benutzer.SponsorplatzUserDetailsService;
import ch.sponsorplatz.organisation.Branche;
import ch.sponsorplatz.shared.config.SecurityConfig;
import org.springframework.security.test.context.support.WithMockUser;

@WebMvcTest(controllers = MarktplatzController.class)
@Import(SecurityConfig.class)
@ActiveProfiles("dev")
@WithMockUser
class MarktplatzControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ProjektService projektService;

    @MockitoBean
    private MedienAssetService medienAssetService;

    @MockitoBean
    private SponsoringPaketService paketService;

    @MockitoBean
    private SponsorplatzUserDetailsService userDetailsService;

    @BeforeEach
    void setUp() {
        when(projektService.findeNeuesteOeffentlicheAlsViews(3)).thenReturn(List.of());
    }

    private ProjektView testProjektView() {
        return testProjektView(UUID.randomUUID(), "Sommerfest", "sommerfest", "Sport", "Zürich", null);
    }

    private ProjektView testProjektView(UUID id, String name, String slug,
                                        String kategorie, String ort, Branche branche) {
        ProjektView.OrganisationKurzView org = new ProjektView.OrganisationKurzView(
                UUID.randomUUID(), "FC Muster", "fc-muster", branche);
        return new ProjektView(id, name, slug, Sichtbarkeit.OEFFENTLICH,
                kategorie, ort, null, null, null, null, org, null);
    }

    /** MKT-01: Marktplatz-Seite ist öffentlich erreichbar. */
    @Test
    void marktplatzIstPublic() throws Exception {
        when(projektService.findeOeffentlicheAlsViews()).thenReturn(List.of());

        mockMvc.perform(get("/marktplatz"))
                .andExpect(status().isOk())
                .andExpect(view().name("projekt/marktplatz"))
                .andExpect(model().attributeExists("projekte"));
    }

    /** MKT-02: Marktplatz zeigt veröffentlichte Projekte. */
    @Test
    void marktplatzZeigtProjekte() throws Exception {
        when(projektService.findeOeffentlicheAlsViews())
                .thenReturn(List.of(testProjektView()));

        mockMvc.perform(get("/marktplatz"))
                .andExpect(status().isOk())
                .andExpect(model().attributeExists("projekte"));
    }

    /**
     * MKT-02b: In der ungefilterten Startansicht erscheinen die als
     * "Neueste" gezeigten Projekte NICHT zusätzlich in der Hauptliste,
     * sonst sieht der User dieselbe Karte zweimal.
     */
    @Test
    void neuesteProjekteWerdenAusHauptlisteHerausgefiltert() throws Exception {
        ProjektView neu = testProjektView();
        ProjektView alt = testProjektView(UUID.randomUUID(), "Älteres Projekt",
                "aelteres-projekt", "Sport", "Zürich", null);

        when(projektService.findeOeffentlicheAlsViews()).thenReturn(List.of(neu, alt));
        when(projektService.findeNeuesteOeffentlicheAlsViews(3)).thenReturn(List.of(neu));

        org.springframework.test.web.servlet.MvcResult result = mockMvc.perform(get("/marktplatz"))
                .andExpect(status().isOk())
                .andReturn();

        @SuppressWarnings("unchecked")
        java.util.List<ProjektView> hauptliste = (java.util.List<ProjektView>)
                result.getModelAndView().getModel().get("projekte");
        @SuppressWarnings("unchecked")
        java.util.List<ProjektView> neueste = (java.util.List<ProjektView>)
                result.getModelAndView().getModel().get("neueste");

        assertThat(neueste).extracting(ProjektView::id).containsExactly(neu.id());
        assertThat(hauptliste).extracting(ProjektView::id).containsExactly(alt.id());
    }

    /**
     * MKT-02c: Edge-Case zum MKT-02b-Fix — wenn die Plattform nur 1–3 Projekte
     * hat, landen ALLE in `neueste` und `projekte` ist nach Dedup leer.
     * Der „Keine Projekte gefunden"-Empty-State darf dann NICHT erscheinen,
     * weil die Neueste-Preview die Projekte ja zeigt.
     */
    @Test
    void neuesteProjekteNonLeer_aberHauptlisteLeer_keinEmptyState() throws Exception {
        ProjektView einziges = testProjektView();
        when(projektService.findeOeffentlicheAlsViews()).thenReturn(List.of(einziges));
        when(projektService.findeNeuesteOeffentlicheAlsViews(3)).thenReturn(List.of(einziges));

        org.springframework.test.web.servlet.MvcResult result = mockMvc.perform(get("/marktplatz"))
                .andExpect(status().isOk())
                .andReturn();

        @SuppressWarnings("unchecked")
        java.util.List<ProjektView> hauptliste = (java.util.List<ProjektView>)
                result.getModelAndView().getModel().get("projekte");
        @SuppressWarnings("unchecked")
        java.util.List<ProjektView> neueste = (java.util.List<ProjektView>)
                result.getModelAndView().getModel().get("neueste");

        assertThat(hauptliste).as("nach Dedup leer").isEmpty();
        assertThat(neueste).as("Preview zeigt das einzige Projekt").hasSize(1);

        String html = result.getResponse().getContentAsString();
        assertThat(html)
                .as("Empty-State darf nicht erscheinen, wenn Neueste-Preview gefüllt ist")
                .doesNotContain("Keine Projekte gefunden")
                .doesNotContain("keineGefunden");
    }

    /** MKT-03: Filter nach Kategorie. */
    @Test
    void filterNachKategorie() throws Exception {
        when(projektService.findeOeffentlicheAlsViews()).thenReturn(List.of(testProjektView()));

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
        when(projektService.findeOeffentlicheAlsViews()).thenReturn(List.of(testProjektView()));

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
        ProjektView view = testProjektView();
        when(projektService.findeViewNachSlugOderWirf("sommerfest")).thenReturn(view);
        when(paketService.findeAktiveViewsNachProjekt(view.id())).thenReturn(List.of());
        when(medienAssetService.findeAnhaengeViews(EntityTyp.PROJEKT, view.id())).thenReturn(List.of());
        when(medienAssetService.findeGalerieViews(EntityTyp.PROJEKT, view.id())).thenReturn(List.of());
        when(medienAssetService.findeCoverUrl(EntityTyp.PROJEKT, view.id())).thenReturn(Optional.empty());

        mockMvc.perform(get("/marktplatz/sommerfest"))
                .andExpect(status().isOk())
                .andExpect(view().name("projekt/marktplatz-detail"))
                .andExpect(model().attributeExists("projekt", "pakete", "anhaenge", "galerie"));
    }

    /**
     * MKT-05b: Detail-Seite reicht Cover, Galerie und Anhänge ans Template durch.
     * Cover landet als coverUrl auf der ProjektView; Galerie und Anhänge separat.
     */
    @Test
    void detailSeiteMitMedien() throws Exception {
        ProjektView view = testProjektView();
        UUID coverId = UUID.randomUUID();
        UUID galerieId = UUID.randomUUID();
        UUID anhangId = UUID.randomUUID();

        when(projektService.findeViewNachSlugOderWirf("sommerfest")).thenReturn(view);
        when(paketService.findeAktiveViewsNachProjekt(view.id())).thenReturn(List.of());
        when(medienAssetService.findeCoverUrl(EntityTyp.PROJEKT, view.id()))
                .thenReturn(Optional.of("/medien/" + coverId));
        when(medienAssetService.findeGalerieViews(EntityTyp.PROJEKT, view.id()))
                .thenReturn(List.of(new MedienAssetView(galerieId, "bild-1.jpg",
                        "image/jpeg", AssetTyp.GALERIE.name(), "/medien/" + galerieId, 0L)));
        when(medienAssetService.findeAnhaengeViews(EntityTyp.PROJEKT, view.id()))
                .thenReturn(List.of(new MedienAssetView(anhangId, "pitch.pdf",
                        "application/pdf", AssetTyp.ANHANG.name(), "/medien/" + anhangId, 0L)));

        ModelAndView mv = mockMvc.perform(get("/marktplatz/sommerfest"))
                .andExpect(status().isOk())
                .andReturn().getModelAndView();

        ProjektView projektView = (ProjektView) mv.getModel().get("projekt");
        assertThat(projektView.coverUrl()).isEqualTo("/medien/" + coverId);

        @SuppressWarnings("unchecked")
        List<MedienAssetView> galerie = (List<MedienAssetView>) mv.getModel().get("galerie");
        assertThat(galerie).hasSize(1).first()
                .extracting(MedienAssetView::dateiname).isEqualTo("bild-1.jpg");

        @SuppressWarnings("unchecked")
        List<MedienAssetView> anhaenge = (List<MedienAssetView>) mv.getModel().get("anhaenge");
        assertThat(anhaenge).hasSize(1).first()
                .extracting(MedienAssetView::dateiname).isEqualTo("pitch.pdf");
    }

    /**
     * MKT-06: Volltextsuche mit Parameter q delegiert an ProjektService.suche().
     */
    @Test
    void volltextSucheMitParameterQ() throws Exception {
        when(projektService.sucheAlsViews("Sommer")).thenReturn(List.of(testProjektView()));

        mockMvc.perform(get("/marktplatz").param("q", "Sommer"))
                .andExpect(status().isOk())
                .andExpect(model().attributeExists("projekte", "suchbegriff"))
                .andExpect(model().attribute("suchbegriff", "Sommer"));
    }

    /** MKT-07: Leerer Suchbegriff zeigt alle öffentlichen Projekte. */
    @Test
    void leereSucheZeigtAlle() throws Exception {
        when(projektService.findeOeffentlicheAlsViews()).thenReturn(List.of());

        mockMvc.perform(get("/marktplatz").param("q", ""))
                .andExpect(status().isOk())
                .andExpect(model().attributeExists("projekte"));
    }

    // -----------------------------------------------------------------
    // Phase 7.1 — Marktplatz-Branche-Filter (Health-Story sichtbar machen)
    // -----------------------------------------------------------------

    /** MKT-08: ?branche=SPORT reduziert die Liste auf Projekte mit org.branche=SPORT. */
    @Test
    void filterNachBranche() throws Exception {
        ProjektView sport = testProjektView(UUID.randomUUID(), "Sportverein",
                "sport-sommerfest", null, null, Branche.SPORT);
        ProjektView reha = testProjektView(UUID.randomUUID(), "Reha-Zentrum",
                "reha-bewegungstag", null, null, Branche.REHA);
        when(projektService.findeOeffentlicheAlsViews()).thenReturn(List.of(sport, reha));

        mockMvc.perform(get("/marktplatz").param("branche", "SPORT"))
                .andExpect(status().isOk())
                .andExpect(view().name("projekt/marktplatz"))
                .andExpect(result -> {
                    ModelAndView mv = result.getModelAndView();
                    assertThat(mv).isNotNull();
                    @SuppressWarnings("unchecked")
                    List<ProjektView> projekte = (List<ProjektView>) mv.getModel().get("projekte");
                    assertThat(projekte)
                            .as("Nur Sport-Projekt soll nach SPORT-Filter durchkommen")
                            .hasSize(1)
                            .extracting(ProjektView::slug)
                            .containsExactly("sport-sommerfest");

                    @SuppressWarnings("unchecked")
                    Collection<Branche> aktive = (Collection<Branche>) mv.getModel().get("filterBranchen");
                    assertThat(aktive)
                            .as("filterBranchen-Modell hält die aktive Auswahl für aktive Chips")
                            .containsExactly(Branche.SPORT);
                });
    }

    /** MKT-09: Ohne branche-Param → Liste unverändert; Model exposed alleBranchen + leeres filterBranchen. */
    @Test
    void ohneBrancheZeigtAlleBranchen() throws Exception {
        ProjektView sport = testProjektView(UUID.randomUUID(), "Sportverein", "sport",
                null, null, Branche.SPORT);
        ProjektView reha = testProjektView(UUID.randomUUID(), "Reha", "reha",
                null, null, Branche.REHA);
        when(projektService.findeOeffentlicheAlsViews()).thenReturn(List.of(sport, reha));

        mockMvc.perform(get("/marktplatz"))
                .andExpect(status().isOk())
                .andExpect(model().attributeExists("alleBranchen", "filterBranchen", "projekte"))
                .andExpect(result -> {
                    ModelAndView mv = result.getModelAndView();
                    assertThat(mv).isNotNull();

                    Branche[] alle = (Branche[]) mv.getModel().get("alleBranchen");
                    assertThat(alle)
                            .as("Chip-Cloud rendert alle elf Health-Branchen")
                            .containsExactly(Branche.values())
                            .hasSize(11);

                    @SuppressWarnings("unchecked")
                    Collection<Branche> aktive = (Collection<Branche>) mv.getModel().get("filterBranchen");
                    assertThat(aktive)
                            .as("Kein Filter aktiv — leeres Set, niemals null")
                            .isNotNull()
                            .isEmpty();

                    @SuppressWarnings("unchecked")
                    List<ProjektView> projekte = (List<ProjektView>) mv.getModel().get("projekte");
                    assertThat(projekte)
                            .as("Ohne Filter werden alle öffentlichen Projekte gezeigt")
                            .hasSize(2);
                });
    }

    /** MKT-10: Multi-Select branche=SPORT&branche=REHA — beide aktiv, beide Projekte. */
    @Test
    void multiSelectFilterUndStatePersistenz() throws Exception {
        ProjektView sport = testProjektView(UUID.randomUUID(), "Sportverein", "sport",
                null, null, Branche.SPORT);
        ProjektView reha = testProjektView(UUID.randomUUID(), "Reha-Zentrum", "reha",
                null, null, Branche.REHA);
        ProjektView mental = testProjektView(UUID.randomUUID(), "Mental Health", "mental",
                null, null, Branche.MENTAL_HEALTH);
        when(projektService.findeOeffentlicheAlsViews()).thenReturn(List.of(sport, reha, mental));

        mockMvc.perform(get("/marktplatz")
                .param("branche", "SPORT")
                .param("branche", "REHA"))
                .andExpect(status().isOk())
                .andExpect(result -> {
                    ModelAndView mv = result.getModelAndView();
                    assertThat(mv).isNotNull();

                    @SuppressWarnings("unchecked")
                    Collection<Branche> aktive = (Collection<Branche>) mv.getModel().get("filterBranchen");
                    assertThat(aktive)
                            .as("Beide gewählten Branchen müssen im filterBranchen-Modell sein")
                            .containsExactlyInAnyOrder(Branche.SPORT, Branche.REHA);

                    @SuppressWarnings("unchecked")
                    List<ProjektView> projekte = (List<ProjektView>) mv.getModel().get("projekte");
                    assertThat(projekte)
                            .as("MENTAL_HEALTH-Projekt darf NICHT durchkommen")
                            .hasSize(2)
                            .extracting(ProjektView::slug)
                            .containsExactlyInAnyOrder("sport", "reha");
                });
    }

    /** MKT-11: Ungefilterte Startseite enthält „neueste" Attribut mit max. 3 Projekten. */
    @Test
    void startseitenPreviewZeigtNeueste() throws Exception {
        ProjektView p = testProjektView();
        when(projektService.findeOeffentlicheAlsViews()).thenReturn(List.of(p));
        when(projektService.findeNeuesteOeffentlicheAlsViews(3)).thenReturn(List.of(p));

        mockMvc.perform(get("/marktplatz"))
                .andExpect(status().isOk())
                .andExpect(model().attributeExists("neueste"))
                .andExpect(result -> {
                    ModelAndView mv = result.getModelAndView();
                    assertThat(mv).isNotNull();
                    @SuppressWarnings("unchecked")
                    List<ProjektView> neueste = (List<ProjektView>) mv.getModel().get("neueste");
                    assertThat(neueste).hasSize(1);
                    assertThat(neueste.get(0).name()).isEqualTo("Sommerfest");
                });
    }

    /** MKT-12: Gefilterte Ansicht (z.B. ?q=xyz) zeigt KEINE Preview-Sektion. */
    @Test
    void gefilterteAnsichtOhnePreview() throws Exception {
        when(projektService.sucheAlsViews("Test")).thenReturn(List.of());

        mockMvc.perform(get("/marktplatz").param("q", "Test"))
                .andExpect(status().isOk())
                .andExpect(result -> {
                    ModelAndView mv = result.getModelAndView();
                    assertThat(mv).isNotNull();
                    @SuppressWarnings("unchecked")
                    List<ProjektView> neueste = (List<ProjektView>) mv.getModel().get("neueste");
                    assertThat(neueste).isEmpty();
                });
    }
}
