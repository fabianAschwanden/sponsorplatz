package ch.sponsorplatz.anfrage;

import ch.sponsorplatz.organisation.Organisation;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit-Tests für {@link VertragView} — DTO-Mapping aus Entity.
 * Test-IDs: VIEW-14, VIEW-15
 */
class VertragViewTest {

    @Test
    @DisplayName("VIEW-14: von() mappt alle Felder korrekt")
    void vonMapptAlleFelder() {
        Organisation org = new Organisation();
        org.setSlug("fc-test");
        Organisation sponsorOrg = new Organisation();
        sponsorOrg.setName("Sponsor AG");

        SponsoringAnfrage anfrage = new SponsoringAnfrage();
        anfrage.setId(UUID.randomUUID());

        Vertrag v = new Vertrag();
        v.setId(UUID.randomUUID());
        v.setAnfrage(anfrage);
        v.setStatus(VertragsStatus.ENTWURF);
        v.setOrgName("FC Test");
        v.setOrg(org);
        v.setSponsorName("Max Sponsor");
        v.setSponsorEmail("max@sponsor.ch");
        v.setSponsorOrg(sponsorOrg);
        v.setPaketName("Gold");
        v.setPaketBeschreibung("Alles inklusive");
        v.setPreisChf(BigDecimal.valueOf(5000));
        v.setLaufzeitVon(LocalDate.of(2026, 1, 1));
        v.setLaufzeitBis(LocalDate.of(2026, 12, 31));
        v.setLeistungVerein("Logo auf Trikot");
        v.setLeistungSponsor("5000 CHF");
        v.setErstelltAm(Instant.now());
        v.setErstelltVon("admin@test.ch");
        v.setUnterzeichnetAm(Instant.now());
        v.setUnterzeichnetVon("owner@test.ch");

        VertragView view = VertragView.von(v);

        assertThat(view.id()).isEqualTo(v.getId());
        assertThat(view.anfrageId()).isEqualTo(anfrage.getId());
        assertThat(view.status()).isEqualTo(VertragsStatus.ENTWURF);
        assertThat(view.orgName()).isEqualTo("FC Test");
        assertThat(view.orgSlug()).isEqualTo("fc-test");
        assertThat(view.sponsorName()).isEqualTo("Max Sponsor");
        assertThat(view.sponsorEmail()).isEqualTo("max@sponsor.ch");
        assertThat(view.sponsorOrgName()).isEqualTo("Sponsor AG");
        assertThat(view.paketName()).isEqualTo("Gold");
        assertThat(view.paketBeschreibung()).isEqualTo("Alles inklusive");
        assertThat(view.preisChf()).isEqualByComparingTo("5000");
        assertThat(view.laufzeitVon()).isEqualTo(LocalDate.of(2026, 1, 1));
        assertThat(view.laufzeitBis()).isEqualTo(LocalDate.of(2026, 12, 31));
        assertThat(view.leistungVerein()).isEqualTo("Logo auf Trikot");
        assertThat(view.leistungSponsor()).isEqualTo("5000 CHF");
        assertThat(view.erstelltAm()).isNotNull();
        assertThat(view.erstelltVon()).isEqualTo("admin@test.ch");
        assertThat(view.unterzeichnetAm()).isNotNull();
        assertThat(view.unterzeichnetVon()).isEqualTo("owner@test.ch");
    }

    @Test
    @DisplayName("VIEW-15: von() mit null-Referenzen wirft keine NPE")
    void vonMitNullReferenzen() {
        Vertrag v = new Vertrag();
        v.setId(UUID.randomUUID());
        v.setStatus(VertragsStatus.ENTWURF);
        v.setOrgName("FC Test");
        v.setPaketName("Silber");
        v.setPreisChf(BigDecimal.ZERO);

        VertragView view = VertragView.von(v);

        assertThat(view.anfrageId()).isNull();
        assertThat(view.orgSlug()).isNull();
        assertThat(view.sponsorOrgName()).isNull();
    }
}

