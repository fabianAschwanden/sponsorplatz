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
 * Unit-Tests für {@link RechnungView} — DTO-Mapping aus Entity.
 * Test-IDs: VIEW-16, VIEW-17
 */
class RechnungViewTest {

    @Test
    @DisplayName("VIEW-16: von() mappt alle Felder korrekt")
    void vonMapptAlleFelder() {
        Organisation org = new Organisation();
        org.setName("FC Test");
        org.setSlug("fc-test");

        Vertrag vertrag = new Vertrag();
        vertrag.setId(UUID.randomUUID());

        Rechnung r = new Rechnung();
        r.setId(UUID.randomUUID());
        r.setVertrag(vertrag);
        r.setOrg(org);
        r.setRechnungsnummer("R-2026-00001");
        r.setStatus(RechnungsStatus.OFFEN);
        r.setBetragChf(BigDecimal.valueOf(2500));
        r.setIban("CH93 0076 2011 6238 5295 7");
        r.setQrReferenz("210000000003139471430009017");
        r.setSponsorName("CSS Versicherung");
        r.setSponsorEmail("finance@css.ch");
        r.setSponsorAdresse("Tribschenstrasse 21, 6002 Luzern");
        r.setZahlungszweck("Sponsoring Sommerfest 2026");
        r.setErstelltAm(Instant.now());
        r.setErstelltVon("owner@test.ch");
        r.setFaelligAm(LocalDate.of(2026, 7, 15));
        r.setBezahltAm(Instant.now());
        r.setBezahltVon("finance@css.ch");

        RechnungView view = RechnungView.von(r);

        assertThat(view.id()).isEqualTo(r.getId());
        assertThat(view.vertragId()).isEqualTo(vertrag.getId());
        assertThat(view.orgName()).isEqualTo("FC Test");
        assertThat(view.orgSlug()).isEqualTo("fc-test");
        assertThat(view.rechnungsnummer()).isEqualTo("R-2026-00001");
        assertThat(view.status()).isEqualTo(RechnungsStatus.OFFEN);
        assertThat(view.betragChf()).isEqualByComparingTo("2500");
        assertThat(view.iban()).isEqualTo("CH93 0076 2011 6238 5295 7");
        assertThat(view.qrReferenz()).isEqualTo("210000000003139471430009017");
        assertThat(view.sponsorName()).isEqualTo("CSS Versicherung");
        assertThat(view.sponsorEmail()).isEqualTo("finance@css.ch");
        assertThat(view.sponsorAdresse()).isEqualTo("Tribschenstrasse 21, 6002 Luzern");
        assertThat(view.zahlungszweck()).isEqualTo("Sponsoring Sommerfest 2026");
        assertThat(view.erstelltAm()).isNotNull();
        assertThat(view.erstelltVon()).isEqualTo("owner@test.ch");
        assertThat(view.faelligAm()).isEqualTo(LocalDate.of(2026, 7, 15));
        assertThat(view.bezahltAm()).isNotNull();
        assertThat(view.bezahltVon()).isEqualTo("finance@css.ch");
    }

    @Test
    @DisplayName("VIEW-17: von() mit null-Referenzen wirft keine NPE")
    void vonMitNullReferenzen() {
        Rechnung r = new Rechnung();
        r.setId(UUID.randomUUID());
        r.setRechnungsnummer("R-2026-00002");
        r.setStatus(RechnungsStatus.OFFEN);
        r.setBetragChf(BigDecimal.valueOf(1000));
        r.setIban("CH00 0000 0000 0000 0000 0");
        r.setSponsorName("Anon");
        r.setFaelligAm(LocalDate.of(2026, 8, 1));

        RechnungView view = RechnungView.von(r);

        assertThat(view.vertragId()).isNull();
        assertThat(view.orgName()).isNull();
        assertThat(view.orgSlug()).isNull();
    }
}

