package ch.sponsorplatz.service;

import ch.sponsorplatz.dto.OrganisationFormDto;
import ch.sponsorplatz.exception.NotFoundException;
import ch.sponsorplatz.model.OrgTyp;
import ch.sponsorplatz.model.Organisation;
import ch.sponsorplatz.repository.MitgliedschaftRepository;
import ch.sponsorplatz.repository.OrganisationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

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

    /** ORG-05: Speichern mit Auto-Slug aus dem Namen. */
    @Test
    void speichernMitAutoSlug() {
        when(repository.findBySlug("fc-beispiel-zuerich")).thenReturn(Optional.empty());
        when(repository.save(any(Organisation.class))).thenAnswer(inv -> inv.getArgument(0));

        OrganisationFormDto dto = neuesDto("FC Beispiel Zürich", null);

        Organisation gespeichert = service.speichere(dto);

        assertThat(gespeichert.getSlug()).isEqualTo("fc-beispiel-zuerich");
        assertThat(gespeichert.getName()).isEqualTo("FC Beispiel Zürich");
        assertThat(gespeichert.getTyp()).isEqualTo(OrgTyp.VEREIN);
    }

    /** ORG-06: Speichern wirft bei Slug-Konflikt. */
    @Test
    void speichernWirftBeiSlugKonflikt() {
        Organisation andere = new Organisation();
        andere.setId(UUID.randomUUID());
        andere.setSlug("doppelt");
        when(repository.findBySlug("doppelt")).thenReturn(Optional.of(andere));

        OrganisationFormDto dto = neuesDto("Doppelt", "doppelt");

        assertThatThrownBy(() -> service.speichere(dto))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("bereits vergeben");
    }

    /** ORG-07: Validierung — Name zu kurz / leer wird über DTO-Bean-Validation gefangen,
     *  aber wenn doch bis zum Service durchkommt, muss SlugGenerator scheitern. */
    @Test
    void leererNameSchlaegtFehl() {
        OrganisationFormDto dto = new OrganisationFormDto();
        dto.setTyp(OrgTyp.VEREIN);
        dto.setName("");

        assertThatThrownBy(() -> service.speichere(dto))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void aktualisierungBestehenderOrganisation() {
        UUID id = UUID.randomUUID();
        Organisation bestehende = new Organisation();
        bestehende.setId(id);
        bestehende.setSlug("alt-slug");
        bestehende.setName("Alter Name");
        bestehende.setTyp(OrgTyp.VEREIN);

        when(repository.findById(id)).thenReturn(Optional.of(bestehende));
        when(repository.findBySlug("alt-slug")).thenReturn(Optional.of(bestehende));
        when(repository.save(any(Organisation.class))).thenAnswer(inv -> inv.getArgument(0));

        OrganisationFormDto dto = neuesDto("Neuer Name", "alt-slug");
        dto.setId(id);
        Organisation aktualisiert = service.speichere(dto);

        assertThat(aktualisiert.getName()).isEqualTo("Neuer Name");
        assertThat(aktualisiert.getSlug()).isEqualTo("alt-slug");
    }

    @Test
    void leereStringsWerdenNull() {
        when(repository.findBySlug(any())).thenReturn(Optional.empty());
        when(repository.save(any(Organisation.class))).thenAnswer(inv -> inv.getArgument(0));

        OrganisationFormDto dto = neuesDto("Test-Org", null);
        dto.setRechtsform("   ");
        dto.setBranche("");
        dto.setBeschreibung("  echte Beschreibung  ");

        Organisation gespeichert = service.speichere(dto);

        assertThat(gespeichert.getRechtsform()).isNull();
        assertThat(gespeichert.getBranche()).isNull();
        assertThat(gespeichert.getBeschreibung()).isEqualTo("echte Beschreibung");
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

    private OrganisationFormDto neuesDto(String name, String slug) {
        OrganisationFormDto dto = new OrganisationFormDto();
        dto.setTyp(OrgTyp.VEREIN);
        dto.setName(name);
        dto.setSlug(slug);
        return dto;
    }
}
