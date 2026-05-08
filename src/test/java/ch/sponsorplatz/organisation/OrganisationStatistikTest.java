package ch.sponsorplatz.organisation;

import ch.sponsorplatz.projekt.Projekt;
import ch.sponsorplatz.projekt.ProjektRepository;
import ch.sponsorplatz.projekt.Sichtbarkeit;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integrationstests für die Aggregat-Queries der Marken-Landing-Page.
 *
 * <p>Im Gegensatz zum {@code StatistikServiceTest} (Service mit gemocktem Repo)
 * spielt dieser Test die Queries gegen ein echtes H2 (PostgreSQL-Mode) und
 * verifiziert die WHERE-Clauses und das GROUP BY auf SQL-Ebene. Konsistent zu
 * {@link OrganisationRepositoryTest}: dev-Profil + nicht-ersetzte Datasource,
 * damit Flyway-Migrations (mit {@code gen_random_uuid()}) sauber laufen.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("dev")
class OrganisationStatistikTest {

    @Autowired
    private OrganisationRepository organisationRepository;

    @Autowired
    private ProjektRepository projektRepository;

    @Autowired
    private TestEntityManager em;

    /**
     * MARK-04: zaehleVereineNachBranche filtert auf typ=VEREIN + status IN (...)
     * und gruppiert korrekt nach Branche. ({@code branche IS NOT NULL} bleibt
     * im Query als Defense — die Spalte ist NOT NULL, aber die Klausel schützt
     * gegen künftige Migrationen, die das lockern könnten.)
     */
    @Test
    @DisplayName("MARK-04: Aggregat-Query filtert nur VEREIN + VERIFIED/ACTIVE, gruppiert korrekt")
    void zaehleVereineNachBrancheFiltertUndGruppiertKorrekt() {
        // Soll mitgezählt werden:
        persistOrg("FC Sport A", "fc-sport-a", OrgTyp.VEREIN, OrgStatus.VERIFIED, Branche.SPORT);
        persistOrg("FC Sport B", "fc-sport-b", OrgTyp.VEREIN, OrgStatus.ACTIVE,   Branche.SPORT);
        persistOrg("Reha-Verein",  "reha-v",     OrgTyp.VEREIN, OrgStatus.VERIFIED, Branche.REHA);

        // Soll NICHT mitgezählt werden:
        persistOrg("Sport-Pending", "sport-pend", OrgTyp.VEREIN,    OrgStatus.PENDING,   Branche.SPORT);
        persistOrg("Sport-Susp",    "sport-susp", OrgTyp.VEREIN,    OrgStatus.SUSPENDED, Branche.SPORT);
        persistOrg("Marken-AG",     "marken-ag",  OrgTyp.UNTERNEHMEN, OrgStatus.VERIFIED, Branche.SPORT);
        persistOrg("Stiftung XY",   "stift-xy",   OrgTyp.STIFTUNG,    OrgStatus.VERIFIED, Branche.SPORT);

        em.flush();
        em.clear();

        List<Object[]> rows = organisationRepository.zaehleVereineNachBranche(
                Set.of(OrgStatus.VERIFIED, OrgStatus.ACTIVE));

        var byBranche = rows.stream()
                .collect(Collectors.toMap(r -> (Branche) r[0], r -> (Long) r[1]));

        assertThat(byBranche).containsEntry(Branche.SPORT, 2L);
        assertThat(byBranche).containsEntry(Branche.REHA, 1L);
        assertThat(byBranche).hasSize(2); // weder Pending/Suspended noch Unternehmen/Stiftung gezählt
    }

    /**
     * MARK-05: countBySichtbarkeit zählt nur OEFFENTLICH, ignoriert ENTWURF und ARCHIVIERT.
     */
    @Test
    @DisplayName("MARK-05: countBySichtbarkeit zählt nur OEFFENTLICH (nicht ENTWURF/ARCHIVIERT)")
    void countBySichtbarkeitNurOeffentlich() {
        Organisation host = persistOrg("Host AG", "host-ag", OrgTyp.UNTERNEHMEN, OrgStatus.VERIFIED, Branche.SPORT);

        persistProjekt("Marathon",   "marathon-26",   host, Sichtbarkeit.OEFFENTLICH);
        persistProjekt("Lauf-Camp",  "lauf-camp",     host, Sichtbarkeit.OEFFENTLICH);
        persistProjekt("Idee",       "idee",          host, Sichtbarkeit.ENTWURF);
        persistProjekt("Alt 2024",   "alt-2024",      host, Sichtbarkeit.ARCHIVIERT);

        em.flush();

        long anzahl = projektRepository.countBySichtbarkeit(Sichtbarkeit.OEFFENTLICH);

        assertThat(anzahl).isEqualTo(2);
    }

    private Organisation persistOrg(String name, String slug, OrgTyp typ, OrgStatus status, Branche branche) {
        Organisation o = new Organisation();
        o.setName(name);
        o.setSlug(slug);
        o.setTyp(typ);
        o.setStatus(status);
        o.setBranche(branche);
        return em.persistAndFlush(o);
    }

    private Projekt persistProjekt(String name, String slug, Organisation org, Sichtbarkeit sichtbarkeit) {
        Projekt p = new Projekt();
        p.setName(name);
        p.setSlug(slug);
        p.setOrg(org);
        p.setSichtbarkeit(sichtbarkeit);
        return em.persistAndFlush(p);
    }
}
