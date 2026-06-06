package ch.sponsorplatz.anfrage.payment.datatrans;

import ch.sponsorplatz.anfrage.payment.PaymentProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit-Tests für {@link DatatransProvider}.
 * Test-IDs: PAY-DT-01..05
 */
class DatatransProviderTest {

    private static final String HMAC_HEX_KEY = "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef";
    private static final byte[] HMAC_KEY = HexFormat.of().parseHex(HMAC_HEX_KEY);

    private DatatransProvider provider;
    private RestClient restClient;

    @BeforeEach
    void setup() {
        restClient = mock(RestClient.class);
        provider = new DatatransProvider(
                "merchant123",
                "password123",
                HMAC_KEY,
                "https://api.sandbox.datatrans.com",
                "https://sponsorplatz.ch/payment/erfolg?ref={rechnungId}",
                "https://sponsorplatz.ch/payment/abgebrochen?ref={rechnungId}",
                "https://sponsorplatz.ch/payment/fehler?ref={rechnungId}",
                restClient
        );
    }

    @Test
    @DisplayName("PAY-DT-01: providerName ist 'datatrans'")
    void providerName() {
        assertThat(provider.providerName()).isEqualTo("datatrans");
    }

    @Test
    @DisplayName("PAY-DT-02: verifiziereSignatur erkennt gültige HMAC im t=…,s0=…-Format")
    void gueltigeSignatur() {
        String body = "{\"transactionId\":\"123\",\"status\":\"settled\"}";
        String timestamp = "1561364320";
        // Datatrans signiert HMAC über (timestamp + body).
        String signatur = berechneHmac(timestamp + body);
        String header = "t=" + timestamp + ",s0=" + signatur;

        boolean ergebnis = provider.verifiziereSignatur(
                Map.of("datatrans-signature", header), body);

        assertThat(ergebnis).isTrue();
    }

    @Test
    @DisplayName("PAY-DT-03: verifiziereSignatur lehnt falsche HMAC ab")
    void falscheSignatur() {
        String body = "{\"transactionId\":\"123\"}";

        boolean ergebnis = provider.verifiziereSignatur(
                Map.of("datatrans-signature", "t=123,s0=deadbeef1234"), body);

        assertThat(ergebnis).isFalse();
    }

    @Test
    @DisplayName("PAY-DT-03b: verifiziereSignatur lehnt fehlenden Header ab")
    void fehlenderHeader() {
        boolean ergebnis = provider.verifiziereSignatur(Map.of(), "{\"foo\":\"bar\"}");
        assertThat(ergebnis).isFalse();
    }

    @Test
    @DisplayName("PAY-DT-03c: verifiziereSignatur lehnt Header ohne t/s0 ab")
    void unvollstaendigerHeader() {
        String body = "{\"transactionId\":\"123\"}";
        boolean ergebnis = provider.verifiziereSignatur(
                Map.of("datatrans-signature", berechneHmac(body)), body); // altes Format ohne t=,s0=
        assertThat(ergebnis).isFalse();
    }

    @Test
    @DisplayName("PAY-DT-06: extrahiereTransaktionsReferenz liest 'transactionId'")
    void extrahiereReferenz() {
        String txId = provider.extrahiereTransaktionsReferenz(
                Map.of("transactionId", "DT-TX-99", "status", "settled"));
        assertThat(txId).isEqualTo("DT-TX-99");
    }

    @Test
    @DisplayName("PAY-DT-04: erstelleZahlung sendet korrekten Request")
    @SuppressWarnings("unchecked")
    void erstelleZahlung() {
        // Mock RestClient chain
        RestClient.RequestBodyUriSpec bodyUriSpec = mock(RestClient.RequestBodyUriSpec.class);
        RestClient.RequestBodySpec bodySpec = mock(RestClient.RequestBodySpec.class);
        RestClient.ResponseSpec responseSpec = mock(RestClient.ResponseSpec.class);

        when(restClient.post()).thenReturn(bodyUriSpec);
        when(bodyUriSpec.uri("/v1/transactions")).thenReturn(bodySpec);
        when(bodySpec.contentType(any())).thenReturn(bodySpec);
        when(bodySpec.body(any(Object.class))).thenReturn(bodySpec);
        when(bodySpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.body((Class<Object>) any(Class.class))).thenReturn(Map.of("transactionId", "DT-TX-42"));

        UUID rechnungId = UUID.randomUUID();
        PaymentProvider.ZahlungsErgebnis ergebnis = provider.erstelleZahlung(
                rechnungId, BigDecimal.valueOf(500.00), "Sponsoring Gold");

        assertThat(ergebnis.transaktionsId()).isEqualTo("DT-TX-42");
        assertThat(ergebnis.status()).isEqualTo(PaymentProvider.ZahlungsStatus.ERSTELLT);
        assertThat(ergebnis.checkoutUrl()).contains("DT-TX-42");
    }

    @Test
    @DisplayName("PAY-DT-05: bestaetigeZahlung mappt 'settled' → BEZAHLT")
    @SuppressWarnings("unchecked")
    void bestaetigeZahlung() {
        RestClient.RequestHeadersUriSpec headersUriSpec = mock(RestClient.RequestHeadersUriSpec.class);
        RestClient.RequestHeadersSpec headersSpec = mock(RestClient.RequestHeadersSpec.class);
        RestClient.ResponseSpec responseSpec = mock(RestClient.ResponseSpec.class);

        when(restClient.get()).thenReturn(headersUriSpec);
        when(headersUriSpec.uri("/v1/transactions/{txId}", "DT-TX-42")).thenReturn(headersSpec);
        when(headersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.body((Class<Object>) any(Class.class))).thenReturn(Map.of("status", "settled"));

        PaymentProvider.ZahlungsErgebnis ergebnis = provider.bestaetigeZahlung("DT-TX-42");

        assertThat(ergebnis.status()).isEqualTo(PaymentProvider.ZahlungsStatus.BEZAHLT);
        assertThat(ergebnis.transaktionsId()).isEqualTo("DT-TX-42");
    }

    /** Berechnet HMAC-SHA256 Hex mit dem Test-Key. */
    private String berechneHmac(String data) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(HMAC_KEY, "HmacSHA256"));
            byte[] hash = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}



