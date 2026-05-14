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
import ch.sponsorplatz.organisation.OrgTyp;
import ch.sponsorplatz.organisation.Organisation;
import ch.sponsorplatz.shared.config.SecurityConfig;

@WebMvcTest(controllers = MarktplatzController.class)
@Import(SecurityConfig.class)
@ActiveProfiles("dev")
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
        // Default-Stub: findeNeuesteOeffentliche wird im Controller immer aufgerufen
        when(projektService.findeNeuesteOeffentliche(3)).thenReturn(List.of());
    }

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

    /**
     * MKT-02b: In der ungefilterten Startansicht erscheinen die als
     * "Neueste" gezeigten Projekte NICHT zusätzlich in der Hauptliste,
     * sonst sieht der User dieselbe Karte zweimal.
     */
    @Test
    void neuesteProjekteWerdenAusHauptlisteHerausgefiltert() throws Exception {
        Projekt neu = testProjekt();
        Projekt alt = testProjekt();
        alt.setId(UUID.randomUUID());
        alt.setName("Älteres Projekt");
        alt.setSlug("aelteres-projekt");

        when(projektService.findeOeffentliche()).thenReturn(java.util.List.of(neu, alt));
        when(projektService.findeNeuesteOeffentliche(3)).thenReturn(java.util.List.of(neu));

        org.springframework.test.web.servlet.MvcResult result = mockMvc.perform(get("/marktplatz"))
                .andExpect(status().isOk())
                .andReturn();

        @SuppressWarnings("unchecked")
        java.util.List<ProjektView> hauptliste = (java.util.List<ProjektView>)
                result.getModelAndView().getModel().get("projekte");
        @SuppressWarnings("unchecked")
        java.util.List<ProjektView> neueste = (java.util.List<ProjektView>)
                result.getModelAndView().getModel().get("neueste");

        assertThat(neueste).extracting(ProjektView::id).containsExactly(neu.getId());
        assertThat(hauptliste).extracting(ProjektView::id).containsExactly(alt.getId());
    }

    /**
     * MKT-02c: Edge-Case zum MKT-02b-Fix — wenn die Plattform nur 1–3 Projekte
     * hat, landen ALLE in `neueste` und `projekte` ist nach Dedup leer.
     * Der „Keine Projekte gefunden"-Empty-State darf dann NICHT erscheinen,
     * weil die Neueste-Preview die Projekte ja zeigt.
     */
    @Test
    void neuesteProjekteNonLeer_aberHauptlisteLeer_keinEmptyState() throws Exception {
        Projekt einziges = testProjekt();
        when(projektService.findeOeffentliche()).thenReturn(java.util.List.of(einziges));
        when(projektService.findeNeuesteOeffentliche(3)).thenReturn(java.util.List.of(einziges));

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

        // Template-Smoke-Check: Empty-State-Text darf NICHT im gerenderten
        // HTML stehen, wenn die Neueste-Preview Projekte enthält.
        String html = result.getResponse().getContentAsString();
        assertThat(html)
                .as("Empty-State darf nicht erscheinen, wenn Neueste-Preview gefüllt ist")
                .doesNotContain("Keine Projekte gefunden")
                .doesNotContain("keineGefunden");
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
                .andExpect(model().attributeExists("projekt", "pakete", "anhaenge", "galerie"));
    }

    /**
     * MKT-05b: Detail-Seite reicht Cover, Galerie und Anhänge ans Template durch.
     * Cover landet als coverUrl auf der ProjektView; Galerie und Anhänge separat.
     */
    @Test
    void detailSeiteMitMedien() throws Exception {
        Projekt p = testProjekt();
        when(projektService.findeNachSlug("sommerfest")).thenReturn(Optional.of(p));

        MedienAsset cover = new MedienAsset();
        cover.setId(UUID.randomUUID());
        cover.setDateiname("cover.jpg");
        cover.setContentType("image/jpeg");
        cover.setAssetTyp(AssetTyp.COVER);
        when(medienAssetService.findeCover(EntityTyp.PROJEKT, p.getId()))
                .thenReturn(Optional.of(cover));

        MedienAsset galerieBild = new MedienAsset();
        galerieBild.setId(UUID.randomUUID());
        galerieBild.setDateiname("bild-1.jpg");
        galerieBild.setContentType("image/jpeg");
        galerieBild.setAssetTyp(AssetTyp.GALERIE);
        when(medienAssetService.findeGalerie(EntityTyp.PROJEKT, p.getId()))
                .thenReturn(List.of(galerieBild));

        MedienAsset anhang = new MedienAsset();
        anhang.setId(UUID.randomUUID());
        anhang.setDateiname("pitch.pdf");
        anhang.setContentType("application/pdf");
        anhang.setAssetTyp(AssetTyp.ANHANG);
        when(medienAssetService.findeAnhaenge(EntityTyp.PROJEKT, p.getId()))
                .thenReturn(List.of(anhang));

        ModelAndView mv = mockMvc.perform(get("/marktplatz/sommerfest"))
                .andExpect(status().isOk())
                .andReturn().getModelAndView();

        ProjektView projektView = (ProjektView) mv.getModel().get("projekt");
        assertThat(projektView.coverUrl()).isEqualTo("/medien/" + cover.getId());

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

    // -----------------------------------------------------------------
    // Phase 7.1 — Marktplatz-Branche-Filter (Health-Story sichtbar machen)
    //
    // TDD-First: Diese Tests sind initial ROT, bis die Controller-Erweiterung
    // implementiert ist. Erwartete API:
    // - @RequestParam(required=false) Set<Branche> branche
    // - Filter-Logik: behält nur Projekte mit org.branche IN branche-Set
    // - Model-Attribute: "alleBranchen" (= Branche.values()), "filterBranchen"
    // (= aktive Auswahl als Set, nie null)
    // -----------------------------------------------------------------

    /**
     * MKT-08: ?branche=SPORT reduziert die Liste auf Projekte mit
     * org.branche=SPORT.
     */
    @Test
    void filterNachBranche() throws Exception {
        Projekt sport = testProjektMitBranche(Branche.SPORT, "Sportverein", "sport-sommerfest");
        Projekt reha = testProjektMitBranche(Branche.REHA, "Reha-Zentrum", "reha-bewegungstag");
        when(projektService.findeOeffentliche()).thenReturn(List.of(sport, reha));

        mockMvc.perform(get("/marktplatz").param("branche", "SPORT"))
                .andExpect(status().isOk())
                .andExpect(view().name("marktplatz"))
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

    /**
     * MKT-09: Ohne branche-Param → Liste unverändert; Model exposed alleBranchen +
     * leeres filterBranchen.
     */
    @Test
    void ohneBrancheZeigtAlleBranchen() throws Exception {
        Projekt sport = testProjektMitBranche(Branche.SPORT, "Sportverein", "sport");
        Projekt reha = testProjektMitBranche(Branche.REHA, "Reha", "reha");
        when(projektService.findeOeffentliche()).thenReturn(List.of(sport, reha));

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

    /**
     * MKT-10: Multi-Select branche=SPORT&branche=REHA — beide aktiv, beide Projekte
     * erscheinen, State im Modell.
     */
    @Test
    void multiSelectFilterUndStatePersistenz() throws Exception {
        Projekt sport = testProjektMitBranche(Branche.SPORT, "Sportverein", "sport");
        Projekt reha = testProjektMitBranche(Branche.REHA, "Reha-Zentrum", "reha");
        Projekt mental = testProjektMitBranche(Branche.MENTAL_HEALTH, "Mental Health", "mental");
        when(projektService.findeOeffentliche()).thenReturn(List.of(sport, reha, mental));

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

    /**
     * MKT-11: Ungefilterte Startseite enthält „neueste" Attribut mit max. 3 Projekten.
     */
    @Test
    void startseitenPreviewZeigtNeueste() throws Exception {
        Projekt p = testProjekt();
        when(projektService.findeOeffentliche()).thenReturn(List.of(p));
        when(projektService.findeNeuesteOeffentliche(3)).thenReturn(List.of(p));

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

    /**
     * MKT-12: Gefilterte Ansicht (z.B. ?q=xyz) zeigt KEINE Preview-Sektion.
     */
    @Test
    void gefilterteAnsichtOhnePreview() throws Exception {
        when(projektService.suche("Test")).thenReturn(List.of());

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

    /**
     * Helper für Branche-Filter-Tests: erzeugt ein öffentliches Projekt
     * mit Org einer bestimmten Health-Branche.
     */
    private Projekt testProjektMitBranche(Branche branche, String name, String slug) {
        Organisation org = new Organisation();
        org.setId(UUID.randomUUID());
        org.setName("FC " + name);
        org.setSlug("fc-" + slug);
        org.setTyp(OrgTyp.VEREIN);
        org.setBranche(branche);

        Projekt p = new Projekt();
        p.setId(UUID.randomUUID());
        p.setOrg(org);
        p.setName(name);
        p.setSlug(slug);
        p.setSichtbarkeit(Sichtbarkeit.OEFFENTLICH);
        return p;
    }
}
