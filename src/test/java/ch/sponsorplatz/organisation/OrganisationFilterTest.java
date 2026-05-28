package ch.sponsorplatz.organisation;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test-IDs ORG-FILTER-01..09 — Filter-Record auf der Org-Liste.
 *
 * <p>Pure Unit-Tests gegen die Match-Logik. Service-Integration ist
 * trivial (Stream + Predicate).
 */
class OrganisationFilterTest {

    private static OrganisationView view(OrgTyp typ, OrgStatus status,
                                          Branche branche, SponsorBranche sponsorBranche,
                                          String name) {
        return new OrganisationView(
                UUID.randomUUID(), name, name.toLowerCase().replace(' ', '-'),
                typ, status, null, branche, sponsorBranche, null, null,
                Instant.now(), null, null, null, null, null);
    }

    @Test
    @DisplayName("ORG-FILTER-01: leer-Filter matcht jede Org")
    void leerMatcht() {
        OrganisationFilter filter = new OrganisationFilter(null, null, null, null);
        assertThat(filter.istLeer()).isTrue();
        assertThat(filter.matcht(view(OrgTyp.VEREIN, OrgStatus.ACTIVE, Branche.SPORT, null, "FC Test"))).isTrue();
    }

    @Test
    @DisplayName("ORG-FILTER-02: leerstring-Felder zählen als leer")
    void leerstringIstLeer() {
        OrganisationFilter filter = new OrganisationFilter(null, null, "", "   ");
        assertThat(filter.istLeer()).isTrue();
    }

    @Test
    @DisplayName("ORG-FILTER-03: Typ-Filter — nur passender Typ")
    void typFilter() {
        OrganisationFilter filter = new OrganisationFilter(OrgTyp.UNTERNEHMEN, null, null, null);
        assertThat(filter.matcht(view(OrgTyp.UNTERNEHMEN, OrgStatus.ACTIVE, null, SponsorBranche.BANK, "Bank AG"))).isTrue();
        assertThat(filter.matcht(view(OrgTyp.VEREIN, OrgStatus.ACTIVE, Branche.SPORT, null, "FC Test"))).isFalse();
    }

    @Test
    @DisplayName("ORG-FILTER-04: Status-Filter — nur passender Status")
    void statusFilter() {
        OrganisationFilter filter = new OrganisationFilter(null, OrgStatus.PENDING, null, null);
        assertThat(filter.matcht(view(OrgTyp.VEREIN, OrgStatus.PENDING, Branche.SPORT, null, "FC Neu"))).isTrue();
        assertThat(filter.matcht(view(OrgTyp.VEREIN, OrgStatus.ACTIVE, Branche.SPORT, null, "FC Alt"))).isFalse();
    }

    @Test
    @DisplayName("ORG-FILTER-05: Branche-Filter matcht Vereins-Branche")
    void brancheVerein() {
        OrganisationFilter filter = new OrganisationFilter(null, null, "SPORT", null);
        assertThat(filter.matcht(view(OrgTyp.VEREIN, OrgStatus.ACTIVE, Branche.SPORT, null, "FC Test"))).isTrue();
        assertThat(filter.matcht(view(OrgTyp.VEREIN, OrgStatus.ACTIVE, Branche.REHA, null, "Reha-Verein"))).isFalse();
    }

    @Test
    @DisplayName("ORG-FILTER-06: Branche-Filter matcht Sponsor-Branche")
    void brancheSponsor() {
        OrganisationFilter filter = new OrganisationFilter(null, null, "BANK", null);
        assertThat(filter.matcht(view(OrgTyp.UNTERNEHMEN, OrgStatus.ACTIVE, null, SponsorBranche.BANK, "Bank AG"))).isTrue();
        assertThat(filter.matcht(view(OrgTyp.UNTERNEHMEN, OrgStatus.ACTIVE, null, SponsorBranche.PHARMA, "Pharma AG"))).isFalse();
    }

    @Test
    @DisplayName("ORG-FILTER-07: Suche — case-insensitive Substring auf Name")
    void sucheCaseInsensitive() {
        OrganisationFilter filter = new OrganisationFilter(null, null, null, "fc");
        assertThat(filter.matcht(view(OrgTyp.VEREIN, OrgStatus.ACTIVE, Branche.SPORT, null, "FC Test"))).isTrue();
        assertThat(filter.matcht(view(OrgTyp.VEREIN, OrgStatus.ACTIVE, Branche.REHA, null, "Reha Bern"))).isFalse();
    }

    @Test
    @DisplayName("ORG-FILTER-08: Kombi Typ + Branche + Suche — alle müssen matchen")
    void kombiUnd() {
        OrganisationFilter filter = new OrganisationFilter(OrgTyp.VEREIN, null, "SPORT", "Test");
        assertThat(filter.matcht(view(OrgTyp.VEREIN, OrgStatus.ACTIVE, Branche.SPORT, null, "FC Test"))).isTrue();
        assertThat(filter.matcht(view(OrgTyp.UNTERNEHMEN, OrgStatus.ACTIVE, null, SponsorBranche.SPORTARTIKEL, "Test AG"))).isFalse();
        assertThat(filter.matcht(view(OrgTyp.VEREIN, OrgStatus.ACTIVE, Branche.REHA, null, "FC Test"))).isFalse();
    }

    @Test
    @DisplayName("ORG-FILTER-09: Org ohne Branche matcht nicht wenn Branche-Filter gesetzt")
    void ohneBrancheKeinMatch() {
        OrganisationFilter filter = new OrganisationFilter(null, null, "SPORT", null);
        assertThat(filter.matcht(view(OrgTyp.ANDERE, OrgStatus.ACTIVE, null, null, "Privat"))).isFalse();
    }
}
