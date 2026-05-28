package ch.sponsorplatz.crm;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * VIEW-KONTAKT-01 — Mapping-Vertrag für KontaktPersonView.
 */
class KontaktPersonViewTest {

    @Test
    @DisplayName("VIEW-KONTAKT-01: Mapping + name() + kein Mandanten-Key im Output")
    void mapping() {
        SponsorAccount account = new SponsorAccount();
        account.setId(UUID.randomUUID());

        KontaktPerson k = new KontaktPerson();
        k.setId(UUID.randomUUID());
        k.setBesitzerSponsorOrgId(UUID.randomUUID());
        k.setAccount(account);
        k.setVorname("Anna");
        k.setNachname("Muster");
        k.setFunktion("Präsidentin");
        k.setKontaktRolle(KontaktRolle.HAUPTANSPRECHPARTNER);
        k.setEmail("anna@verein.ch");
        k.setTelefon("044 111 22 33");
        k.setMobile("079 444 55 66");

        KontaktPersonView view = KontaktPersonView.von(k);

        assertThat(view.id()).isEqualTo(k.getId());
        assertThat(view.accountId()).isEqualTo(account.getId());
        assertThat(view.name()).isEqualTo("Anna Muster");
        assertThat(view.funktion()).isEqualTo("Präsidentin");
        assertThat(view.kontaktRolle()).isEqualTo(KontaktRolle.HAUPTANSPRECHPARTNER);
        assertThat(view.email()).isEqualTo("anna@verein.ch");
        assertThat(view.telefon()).isEqualTo("044 111 22 33");
        assertThat(view.mobile()).isEqualTo("079 444 55 66");
        assertThat(view.toString()).doesNotContain(k.getBesitzerSponsorOrgId().toString());
    }
}
