package ch.sponsorplatz.anfrage;

import ch.sponsorplatz.organisation.Organisation;
import ch.sponsorplatz.projekt.SponsoringPaket;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class AnfrageViewTest {

    /** VIEW-06: AnfrageView mappt Anfrage inkl. paketName (flach). */
    @Test
    void mappingEinerAnfrage() {
        UUID anfrageId = UUID.randomUUID();

        SponsoringPaket paket = new SponsoringPaket();
        paket.setId(UUID.randomUUID());
        paket.setName("Gold-Paket");

        Organisation anfragender = new Organisation();
        anfragender.setId(UUID.randomUUID());
        Organisation empfaenger = new Organisation();
        empfaenger.setId(UUID.randomUUID());

        SponsoringAnfrage a = new SponsoringAnfrage();
        a.setId(anfrageId);
        a.setStatus(AnfrageStatus.NEU);
        a.setNachricht("Wir würden gerne sponsern.");
        a.setKontaktName("Max Müller");
        a.setKontaktEmail("max@firma.ch");
        a.setPaket(paket);
        a.setAnfragenderOrg(anfragender);
        a.setEmpfaengerOrg(empfaenger);

        AnfrageView view = AnfrageView.von(a);

        assertThat(view.id()).isEqualTo(anfrageId);
        assertThat(view.status()).isEqualTo(AnfrageStatus.NEU);
        assertThat(view.nachricht()).isEqualTo("Wir würden gerne sponsern.");
        assertThat(view.kontaktName()).isEqualTo("Max Müller");
        assertThat(view.kontaktEmail()).isEqualTo("max@firma.ch");
        assertThat(view.paketName()).isEqualTo("Gold-Paket");
    }
}
