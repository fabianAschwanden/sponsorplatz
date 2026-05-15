package ch.sponsorplatz.projekt;
import ch.sponsorplatz.shared.util.SlugGenerator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ch.sponsorplatz.organisation.Organisation;

class ProjektServiceTest {

    private ProjektRepository repository;
    private VolltextSucheService volltext;
    private ProjektService service;

    @BeforeEach
    void setUp() {
        repository = mock(ProjektRepository.class);
        volltext = mock(VolltextSucheService.class);
        service = new ProjektService(repository, new SlugGenerator(), volltext,
                mock(ch.sponsorplatz.organisation.OrganisationRepository.class));
    }

    /** PRJ-01: Projekt erstellen mit gültigem Namen. */
    @Test
    void erstelleMitGueltigemNamen() {
        Organisation org = new Organisation();
        org.setId(UUID.randomUUID());
        when(repository.existsBySlug(any())).thenReturn(false);
        when(repository.save(any(Projekt.class))).thenAnswer(inv -> inv.getArgument(0));

        Projekt projekt = service.erstelle(org, "Sommerfest 2026", "Unser jährliches Fest");

        assertThat(projekt.getName()).isEqualTo("Sommerfest 2026");
        assertThat(projekt.getSlug()).isEqualTo("sommerfest-2026");
        assertThat(projekt.getSichtbarkeit()).isEqualTo(Sichtbarkeit.ENTWURF);
        assertThat(projekt.getOrg()).isEqualTo(org);
    }

    /** PRJ-02: Erstellen mit leerem Namen wirft. */
    @Test
    void erstelleOhneNameWirft() {
        Organisation org = new Organisation();
        assertThatThrownBy(() -> service.erstelle(org, "", null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    /** PRJ-03: Veröffentlichen setzt Sichtbarkeit und Zeitstempel. */
    @Test
    void veroeffentlicheSetzt() {
        Projekt projekt = new Projekt();
        projekt.setId(UUID.randomUUID());
        projekt.setSichtbarkeit(Sichtbarkeit.ENTWURF);
        when(repository.findById(projekt.getId())).thenReturn(Optional.of(projekt));
        when(repository.save(any(Projekt.class))).thenAnswer(inv -> inv.getArgument(0));

        Projekt result = service.veroeffentliche(projekt.getId());

        assertThat(result.getSichtbarkeit()).isEqualTo(Sichtbarkeit.OEFFENTLICH);
        assertThat(result.getVeroeffentlichtAm()).isNotNull();
    }

    /** PRJ-04: Bereits öffentliches Projekt erneut veröffentlichen wirft. */
    @Test
    void veroeffentlicheBereitsOeffentlichWirft() {
        Projekt projekt = new Projekt();
        projekt.setId(UUID.randomUUID());
        projekt.setSichtbarkeit(Sichtbarkeit.OEFFENTLICH);
        when(repository.findById(projekt.getId())).thenReturn(Optional.of(projekt));

        assertThatThrownBy(() -> service.veroeffentliche(projekt.getId()))
                .isInstanceOf(IllegalStateException.class);
    }

    /** VTS-01: suche delegiert an VolltextSucheService (Dialect-Routing dort). */
    @Test
    void sucheDelegiertAnVolltextService() {
        Projekt p = new Projekt();
        p.setName("Sommerfest");
        when(volltext.suchen("Sommer")).thenReturn(List.of(p));

        List<Projekt> ergebnis = service.suche("Sommer");

        assertThat(ergebnis).hasSize(1);
        verify(volltext).suchen("Sommer");
    }

    /**
     * VTS-02: leerer Suchbegriff wird auch durch VolltextSucheService entschieden.
     */
    @Test
    void sucheLeerDelegiertEbenfalls() {
        when(volltext.suchen("")).thenReturn(List.of());

        List<Projekt> ergebnis = service.suche("");

        assertThat(ergebnis).isEmpty();
        verify(volltext).suchen("");
    }

    /** PRJ-05: findeNachOrgIds delegiert an Repository. */
    @Test
    void findeNachOrgIdsDelegiertAnRepo() {
        UUID orgA = UUID.randomUUID();
        UUID orgB = UUID.randomUUID();
        Projekt p1 = new Projekt();
        p1.setName("Projekt A");
        Projekt p2 = new Projekt();
        p2.setName("Projekt B");
        when(repository.findByOrgIdInOrderByCreatedAtDesc(List.of(orgA, orgB)))
                .thenReturn(List.of(p1, p2));

        List<Projekt> ergebnis = service.findeNachOrgIds(List.of(orgA, orgB));

        assertThat(ergebnis).extracting(Projekt::getName).containsExactly("Projekt A", "Projekt B");
    }

    /** PRJ-06: findeNachOrgIds gibt bei leerer Collection direkt leere Liste zurück, ohne Repo-Aufruf. */
    @Test
    void findeNachOrgIdsLeereCollectionGibtLeereListe() {
        List<Projekt> ergebnis = service.findeNachOrgIds(List.of());

        assertThat(ergebnis).isEmpty();
        org.mockito.Mockito.verifyNoInteractions(repository);
    }
}
