package ch.sponsorplatz.anfrage;

import ch.sponsorplatz.organisation.Organisation;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Smoke-Tests für {@link QrBillService} — verifiziert dass die Library-
 * Integration funktioniert (PNG-Bytes nicht leer, Data-URL-Präfix korrekt).
 *
 * Test-IDs: QRB-01..03.
 */
class QrBillServiceTest {

    private final QrBillService service = new QrBillService(org.mockito.Mockito.mock(RechnungRepository.class));

    @Test
    @DisplayName("QRB-01: erzeuge liefert nicht-leeres PNG mit Magic Bytes")
    void erzeugePngBytes() {
        Rechnung r = neueRechnung("CH4431999123000889012", "210000000003139471430009017");

        byte[] png = service.erzeuge(r);

        assertThat(png).hasSizeGreaterThan(1000);
        // PNG magic: 89 50 4E 47
        assertThat(png[0] & 0xFF).isEqualTo(0x89);
        assertThat(png[1]).isEqualTo((byte) 'P');
        assertThat(png[2]).isEqualTo((byte) 'N');
        assertThat(png[3]).isEqualTo((byte) 'G');
    }

    @Test
    @DisplayName("QRB-02: erzeugeAlsDataUrl gibt base64-encoded data:-URL zurück")
    void erzeugeDataUrl() {
        Rechnung r = neueRechnung("CH4431999123000889012", "210000000003139471430009017");

        String dataUrl = service.erzeugeAlsDataUrl(r);

        assertThat(dataUrl).startsWith("data:image/png;base64,");
        assertThat(dataUrl.length()).isGreaterThan(2000);
    }

    @Test
    @DisplayName("QRB-03: ohne IBAN wirft IllegalArgumentException")
    void ibanPflicht() {
        Rechnung r = neueRechnung(null, null);

        assertThatThrownBy(() -> service.erzeuge(r))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("IBAN");
    }

    private static Rechnung neueRechnung(String iban, String qrRef) {
        Organisation org = new Organisation();
        org.setId(UUID.randomUUID());
        org.setName("FC Beispiel");
        org.setStrasse("Bahnhofstrasse 1");
        org.setPostleitzahl("8001");
        org.setOrt("Zürich");

        Rechnung r = new Rechnung();
        r.setId(UUID.randomUUID());
        r.setOrg(org);
        r.setIban(iban);
        r.setQrReferenz(qrRef);
        r.setBetragChf(new BigDecimal("123.45"));
        r.setSponsorName("Acme AG");
        r.setZahlungszweck("Test");
        r.setRechnungsnummer("SP-2026-0001");
        r.setFaelligAm(LocalDate.now().plusDays(30));
        return r;
    }
}
