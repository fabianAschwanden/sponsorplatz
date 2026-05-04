package ch.sponsorplatz.dto;

import ch.sponsorplatz.model.SponsoringPaket;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class SponsoringPaketViewTest {

    /** VIEW-05: Mapping eines SponsoringPaket-Records. */
    @Test
    void mappingEinesPakets() {
        UUID paketId = UUID.randomUUID();

        SponsoringPaket paket = new SponsoringPaket();
        paket.setId(paketId);
        paket.setName("Gold");
        paket.setBeschreibung("Logo auf Trikot");
        paket.setPreisChf(new BigDecimal("5000"));
        paket.setAktiv(true);

        SponsoringPaketView view = SponsoringPaketView.von(paket);

        assertThat(view.id()).isEqualTo(paketId);
        assertThat(view.name()).isEqualTo("Gold");
        assertThat(view.beschreibung()).isEqualTo("Logo auf Trikot");
        assertThat(view.preisChf()).isEqualByComparingTo(new BigDecimal("5000"));
        assertThat(view.aktiv()).isTrue();
    }
}
