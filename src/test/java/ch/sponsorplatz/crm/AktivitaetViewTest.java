package ch.sponsorplatz.crm;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * VIEW-AKT-01..02 — Mapping-Vertrag für AktivitaetView.
 */
class AktivitaetViewTest {

    @Test
    @DisplayName("VIEW-AKT-01: Mapping mit verknüpftem Kontakt flacht Namen ein")
    void mappingMitKontakt() {
        SponsorAccount account = new SponsorAccount();
        account.setId(UUID.randomUUID());
        KontaktPerson kontakt = new KontaktPerson();
        kontakt.setId(UUID.randomUUID());
        kontakt.setVorname("Anna");
        kontakt.setNachname("Muster");

        Aktivitaet a = new Aktivitaet();
        a.setId(UUID.randomUUID());
        a.setAccount(account);
        a.setKontaktPerson(kontakt);
        a.setTyp(AktivitaetTyp.ANRUF);
        a.setDatum(LocalDate.of(2026, 5, 28));
        a.setBetreff("Erstkontakt");
        a.setNotiz("Interessiert");

        AktivitaetView view = AktivitaetView.von(a);

        assertThat(view.accountId()).isEqualTo(account.getId());
        assertThat(view.typ()).isEqualTo(AktivitaetTyp.ANRUF);
        assertThat(view.datum()).isEqualTo(LocalDate.of(2026, 5, 28));
        assertThat(view.betreff()).isEqualTo("Erstkontakt");
        assertThat(view.kontaktPersonId()).isEqualTo(kontakt.getId());
        assertThat(view.kontaktName()).isEqualTo("Anna Muster");
    }

    @Test
    @DisplayName("VIEW-AKT-02: Mapping ohne Kontakt → kontaktName null")
    void mappingOhneKontakt() {
        SponsorAccount account = new SponsorAccount();
        account.setId(UUID.randomUUID());
        Aktivitaet a = new Aktivitaet();
        a.setId(UUID.randomUUID());
        a.setAccount(account);
        a.setTyp(AktivitaetTyp.NOTIZ);
        a.setDatum(LocalDate.now());
        a.setBetreff("Notiz");

        AktivitaetView view = AktivitaetView.von(a);

        assertThat(view.kontaktPersonId()).isNull();
        assertThat(view.kontaktName()).isNull();
    }
}
