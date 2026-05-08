package ch.sponsorplatz.anfrage;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class LokalerStubProviderTest {

    private final LokalerStubProvider provider = new LokalerStubProvider();

    @Test
    @DisplayName("PAY-01: erstelleZahlung gibt sofort Status BEZAHLT zurueck")
    void erstelleZahlungSofortBezahlt() {
        PaymentProvider.ZahlungsErgebnis ergebnis = provider.erstelleZahlung(
                UUID.randomUUID(), new BigDecimal("500.00"), "Test");
        assertThat(ergebnis.status()).isEqualTo(PaymentProvider.ZahlungsStatus.BEZAHLT);
        assertThat(ergebnis.transaktionsId()).startsWith("STUB-");
    }

    @Test
    @DisplayName("PAY-02: bestaetigeZahlung ist idempotent")
    void bestaetigeIdempotent() {
        PaymentProvider.ZahlungsErgebnis erstellt = provider.erstelleZahlung(
                UUID.randomUUID(), new BigDecimal("100.00"), "Test");

        PaymentProvider.ZahlungsErgebnis bestaetigt = provider.bestaetigeZahlung(erstellt.transaktionsId());
        assertThat(bestaetigt.status()).isEqualTo(PaymentProvider.ZahlungsStatus.BEZAHLT);

        // Zweiter Aufruf bleibt idempotent
        PaymentProvider.ZahlungsErgebnis nochmal = provider.bestaetigeZahlung(erstellt.transaktionsId());
        assertThat(nochmal.status()).isEqualTo(PaymentProvider.ZahlungsStatus.BEZAHLT);
    }
}

