package ch.sponsorplatz.repository;

import ch.sponsorplatz.model.Branche;
import ch.sponsorplatz.model.OrgStatus;
import ch.sponsorplatz.model.OrgTyp;
import ch.sponsorplatz.model.Organisation;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("dev")
class OrganisationRepositoryTest {

    @Autowired
    private OrganisationRepository repository;

    /** ORG-03: Persistieren und Wiederfinden, Default-Werte werden gesetzt. */
    @Test
    void persistierenUndFinden() {
        Organisation org = neueOrg("FC Beispiel", "fc-beispiel", OrgTyp.VEREIN);

        Organisation gespeichert = repository.save(org);

        assertThat(gespeichert.getId()).isNotNull();
        assertThat(gespeichert.getRegistriertAm()).isNotNull();
        assertThat(gespeichert.getCreatedAt()).isNotNull();
        assertThat(gespeichert.getUpdatedAt()).isNotNull();
        assertThat(gespeichert.getStatus()).isEqualTo(OrgStatus.PENDING);

        Optional<Organisation> wiedergefunden = repository.findBySlug("fc-beispiel");
        assertThat(wiedergefunden).isPresent();
        assertThat(wiedergefunden.get().getName()).isEqualTo("FC Beispiel");
    }

    /** ORG-04: slug ist UNIQUE — Duplikat wirft Exception. */
    @Test
    void slugIstEindeutig() {
        repository.saveAndFlush(neueOrg("Erste Org", "duplikat", OrgTyp.VEREIN));

        Organisation zweite = neueOrg("Zweite Org", "duplikat", OrgTyp.UNTERNEHMEN);

        assertThatThrownBy(() -> repository.saveAndFlush(zweite))
            .isInstanceOf(Exception.class);  // Hibernate wraps DataIntegrityViolationException unterschiedlich
    }

    @Test
    void existsBySlugFunktioniert() {
        repository.saveAndFlush(neueOrg("Test-Org", "test-slug", OrgTyp.VEREIN));

        assertThat(repository.existsBySlug("test-slug")).isTrue();
        assertThat(repository.existsBySlug("nicht-da")).isFalse();
    }

    @Test
    void findAllByOrderByNameAsc() {
        repository.saveAndFlush(neueOrg("Zebra-Verein", "zebra", OrgTyp.VEREIN));
        repository.saveAndFlush(neueOrg("Alpha-Verein", "alpha", OrgTyp.VEREIN));
        repository.saveAndFlush(neueOrg("Mittel-Verein", "mittel", OrgTyp.VEREIN));

        var liste = repository.findAllByOrderByNameAsc();

        assertThat(liste).extracting(Organisation::getName)
            .containsExactly("Alpha-Verein", "Mittel-Verein", "Zebra-Verein");
    }

    private Organisation neueOrg(String name, String slug, OrgTyp typ) {
        Organisation org = new Organisation();
        org.setName(name);
        org.setSlug(slug);
        org.setTyp(typ);
        org.setBranche(Branche.SPORT);
        return org;
    }
}
