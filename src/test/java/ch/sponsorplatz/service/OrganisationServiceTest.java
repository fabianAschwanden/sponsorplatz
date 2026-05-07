package ch.sponsorplatz.service;
import ch.sponsorplatz.shared.util.SlugGenerator;

import ch.sponsorplatz.dto.OrganisationFormDto;
import ch.sponsorplatz.shared.exception.NotFoundException;
import ch.sponsorplatz.model.Branche;
import ch.sponsorplatz.model.OrgStatus;
import ch.sponsorplatz.model.OrgTyp;
import ch.sponsorplatz.model.Organisation;
import ch.sponsorplatz.repository.MitgliedschaftRepository;
import ch.sponsorplatz.repository.OrganisationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class OrganisationServiceTest {

    private OrganisationRepository repository;
    private MitgliedschaftRepository mitgliedschaftRepository;
    private OrganisationService service;

    @BeforeEach
    void setUp() {
        repository = mock(OrganisationRepository.class);
        mitgliedschaftRepository = mock(MitgliedschaftRepository.class);
        service = new OrganisationService(repository, new SlugGenerator(), mitgliedschaftRepository);
    }

    /** ORG-05: Erstellen mit Auto-Slug aus dem Namen. */
    @Test
    void erstelleMitAutoSlug() {
        when(repository.findBySlug("fc-beispiel-zuerich")).thenReturn(Optional.empty());
        when(repository.save(any(Organisation.class))).thenAnswer(inv -> inv.getArgument(0));

        OrganisationFormDto dto = neuesDto("FC Beispiel Zürich", null);

        Organisation gespeichert = service.erstelle(dto);

        assertThat(gespeichert.getSlug()).isEqualTo("fc-beispiel-zuerich");
        assertThat(gespeichert.getName()).isEqualTo("FC Beispiel Zürich");
        assertThat(gespeichert.getTyp()).isEqualTo(OrgTyp.VEREIN);
    }

    /** ORG-06: Erstellen wirft bei Slug-Konflikt. */
    @Test
    void erstelleWirftBeiSlugKonflikt() {
        Organisation andere = new Organisation();
        andere.setId(UUID.randomUUID());
        andere.setSlug("doppelt");
        when(repository.findBySlug("doppelt")).thenReturn(Optional.of(andere));

        OrganisationFormDto dto = neuesDto("Doppelt", "doppelt");

        assertThatThrownBy(() -> service.erstelle(dto))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("bereits vergeben");
    }

    /** ORG-07: Validierung — Name leer wird vom SlugGenerator gefangen. */
    @Test
    void erstelleMitLeeremNameSchlaegtFehl() {
        OrganisationFormDto dto = new OrganisationFormDto();
        dto.setTyp(OrgTyp.VEREIN);
        dto.setName("");

        assertThatThrownBy(() -> service.erstelle(dto))
            .isInstanceOf(IllegalArgumentException.class);
    }

    /** ORG-20: aktualisiere lädt via Slug aus der URL — kein id-Pfad mehr. */
    @Test
    void aktualisiereLaedtViaSlug() {
        UUID id = UUID.randomUUID();
        Organisation bestehende = new Organisation();
        bestehende.setId(id);
        bestehende.setSlug("alt-slug");
        bestehende.setName("Alter Name");
        bestehende.setTyp(OrgTyp.VEREIN);

        when(repository.findBySlug("alt-slug")).thenReturn(Optional.of(bestehende));
        when(repository.save(any(Organisation.class))).thenAnswer(inv -> inv.getArgument(0));

        OrganisationFormDto dto = neuesDto("Neuer Name", "alt-slug");
        Organisation aktualisiert = service.aktualisiere("alt-slug", dto);

        assertThat(aktualisiert.getId()).isEqualTo(id);
        assertThat(aktualisiert.getName()).isEqualTo("Neuer Name");
        assertThat(aktualisiert.getSlug()).isEqualTo("alt-slug");
    }

    /** ORG-21: aktualisiere mit unbekanntem Slug → NotFoundException. */
    @Test
    void aktualisiereWirftBeiUnbekanntemSlug() {
        when(repository.findBySlug("gibts-nicht")).thenReturn(Optional.empty());

        OrganisationFormDto dto = neuesDto("Egal", null);

        assertThatThrownBy(() -> service.aktualisiere("gibts-nicht", dto))
            .isInstanceOf(NotFoundException.class);
    }

    @Test
    void erstelleLeereStringsWerdenNull() {
        when(repository.findBySlug(any())).thenReturn(Optional.empty());
        when(repository.save(any(Organisation.class))).thenAnswer(inv -> inv.getArgument(0));

        OrganisationFormDto dto = neuesDto("Test-Org", null);
        dto.setRechtsform("   ");
        dto.setBeschreibung("  echte Beschreibung  ");

        Organisation gespeichert = service.erstelle(dto);

        assertThat(gespeichert.getRechtsform()).isNull();
        assertThat(gespeichert.getBranche()).isEqualTo(Branche.SPORT);
        assertThat(gespeichert.getBeschreibung()).isEqualTo("echte Beschreibung");
    }

    /** ORG-22: Branche ist Pflicht — null wirft IllegalArgumentException (Health-Fokus). */
    @Test
    void erstelleOhneBrancheSchlaegtFehl() {
        when(repository.findBySlug(any())).thenReturn(Optional.empty());

        OrganisationFormDto dto = neuesDto("Ohne Branche", null);
        dto.setBranche(null);

        assertThatThrownBy(() -> service.erstelle(dto))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Branche");
    }

    /** ORG-23: Health-Branchen werden akzeptiert — alle Enum-Werte. */
    @Test
    void erstelleAkzeptiertAlleHealthBranchen() {
        when(repository.findBySlug(any())).thenReturn(Optional.empty());
        when(repository.save(any(Organisation.class))).thenAnswer(inv -> inv.getArgument(0));

        for (Branche branche : Branche.values()) {
            OrganisationFormDto dto = neuesDto("Test " + branche.name(), branche.name().toLowerCase());
            dto.setBranche(branche);

            Organisation gespeichert = service.erstelle(dto);

            assertThat(gespeichert.getBranche()).isEqualTo(branche);
        }
    }

    /** ORG-11: loesche wirft IllegalStateException wenn Mitgliedschaften vorhanden. */
    @Test
    void loescheWirftBeiVorhandenenMitgliedschaften() {
        UUID orgId = UUID.randomUUID();
        when(repository.existsById(orgId)).thenReturn(true);
        when(mitgliedschaftRepository.existsByOrgId(orgId)).thenReturn(true);

        assertThatThrownBy(() -> service.loesche(orgId))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Mitgliedschaft");
    }

    /** Löschen ohne Mitgliedschaften funktioniert. */
    @Test
    void loescheOhneMitgliedschaftenErfolgreich() {
        UUID orgId = UUID.randomUUID();
        when(repository.existsById(orgId)).thenReturn(true);
        when(mitgliedschaftRepository.existsByOrgId(orgId)).thenReturn(false);

        service.loesche(orgId); // kein Fehler
    }

    /** ADM-05: verifiziere setzt Status VERIFIED + verifziertAm. */
    @Test
    void verifiziereSetzStatusUndZeitstempel() {
        UUID id = UUID.randomUUID();
        Organisation org = new Organisation();
        org.setId(id);
        org.setStatus(OrgStatus.PENDING);
        when(repository.findById(id)).thenReturn(Optional.of(org));
        when(repository.save(any(Organisation.class))).thenAnswer(inv -> inv.getArgument(0));

        Organisation verifiziert = service.verifiziere(id);

        assertThat(verifiziert.getStatus()).isEqualTo(OrgStatus.VERIFIED);
        assertThat(verifiziert.getVerifiziertAm()).isNotNull();
    }

    /** ADM-06: verifiziere bei nicht-PENDING → IllegalStateException. */
    @Test
    void verifiziereWirftBeiNichtPending() {
        UUID id = UUID.randomUUID();
        Organisation org = new Organisation();
        org.setId(id);
        org.setStatus(OrgStatus.VERIFIED);
        when(repository.findById(id)).thenReturn(Optional.of(org));

        assertThatThrownBy(() -> service.verifiziere(id))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("PENDING");
    }

    /** ADM-07: suspendiere setzt Status SUSPENDED. */
    @Test
    void suspendiereSetzStatusSuspended() {
        UUID id = UUID.randomUUID();
        Organisation org = new Organisation();
        org.setId(id);
        org.setStatus(OrgStatus.ACTIVE);
        when(repository.findById(id)).thenReturn(Optional.of(org));
        when(repository.save(any(Organisation.class))).thenAnswer(inv -> inv.getArgument(0));

        Organisation suspendiert = service.suspendiere(id);

        assertThat(suspendiert.getStatus()).isEqualTo(OrgStatus.SUSPENDED);
    }

    /** ADM-08: findePending gibt nur PENDING-Orgs zurück. */
    @Test
    void findePendingGibtNurPendingZurueck() {
        Organisation pending = new Organisation();
        pending.setStatus(OrgStatus.PENDING);
        when(repository.findByStatusOrderByCreatedAtAsc(OrgStatus.PENDING))
                .thenReturn(List.of(pending));

        List<Organisation> ergebnis = service.findePending();

        assertThat(ergebnis).hasSize(1);
        assertThat(ergebnis.get(0).getStatus()).isEqualTo(OrgStatus.PENDING);
    }

    private OrganisationFormDto neuesDto(String name, String slug) {
        OrganisationFormDto dto = new OrganisationFormDto();
        dto.setTyp(OrgTyp.VEREIN);
        dto.setName(name);
        dto.setSlug(slug);
        dto.setBranche(Branche.SPORT);
        return dto;
    }
}
