package ch.sponsorplatz.anfrage;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import ch.sponsorplatz.benutzer.SponsorplatzUserDetailsService;
import ch.sponsorplatz.shared.config.SecurityConfig;
import ch.sponsorplatz.shared.exception.NotFoundException;

/**
 * Controller-Tests für {@link PaymentWebhookController}.
 * Test-IDs: PAY-WH-01..08
 *
 * <p>Die zu bezahlende Rechnung wird über die gespeicherte
 * {@link PaymentTransaction} aufgelöst (nicht aus dem Body) — Confused-Deputy-Schutz.
 *
 * <p>Kein CSRF nötig: Route ist in SecurityConfig CSRF-ausgenommen.
 */
@WebMvcTest(controllers = PaymentWebhookController.class)
@Import(SecurityConfig.class)
@ActiveProfiles("dev")
class PaymentWebhookControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PaymentService paymentService;

    @MockitoBean
    private RechnungService rechnungService;

    @MockitoBean
    private PaymentTransactionService transactionService;

    @MockitoBean
    private SponsorplatzUserDetailsService userDetailsService;

    // Body trägt nur die Provider-Transaktions-Referenz; die Rechnung kommt aus dem Store.
    private static final String VALID_BODY = """
            {"transaktionsId":"TX-123","status":"settled"}""";

    @Test
    @DisplayName("PAY-WH-01: Unbekannter Provider → 404")
    void unbekannterProvider() throws Exception {
        when(paymentService.findeProviderOrNull("unknown")).thenReturn(null);

        mockMvc.perform(post("/payment/webhook/unknown")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"transaktionsId\":\"x\"}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value("error"));
    }

    @Test
    @DisplayName("PAY-WH-02: Ungültige Signatur → 401")
    void ungueltigeSignatur() throws Exception {
        PaymentProvider stubProvider = stubProvider(false);
        when(paymentService.findeProviderOrNull("stub")).thenReturn(stubProvider);

        mockMvc.perform(post("/payment/webhook/stub")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"transaktionsId\":\"x\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value("error"));
    }

    @Test
    @DisplayName("PAY-WH-03: Ungültiger JSON-Body → 400")
    void ungueltigerJson() throws Exception {
        PaymentProvider stubProvider = stubProvider(true);
        when(paymentService.findeProviderOrNull("stub")).thenReturn(stubProvider);

        mockMvc.perform(post("/payment/webhook/stub")
                .contentType(MediaType.APPLICATION_JSON)
                .content("not json"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("PAY-WH-04: Fehlende Transaktions-Referenz → 400")
    void fehlendeReferenz() throws Exception {
        PaymentProvider stubProvider = stubProvider(true);
        when(paymentService.findeProviderOrNull("stub")).thenReturn(stubProvider);

        mockMvc.perform(post("/payment/webhook/stub")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"foo\":\"bar\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Transaktions-Referenz fehlt"));
    }

    @Test
    @DisplayName("PAY-WH-08: Unbekannte Transaktion (kein Store-Eintrag) → 404")
    void unbekannteTransaktion() throws Exception {
        PaymentProvider stubProvider = stubProvider(true);
        when(paymentService.findeProviderOrNull("stub")).thenReturn(stubProvider);
        when(transactionService.findeRechnungIdNachReferenz("stub", "TX-123"))
                .thenReturn(Optional.empty());

        mockMvc.perform(post("/payment/webhook/stub")
                .contentType(MediaType.APPLICATION_JSON)
                .content(VALID_BODY))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Transaktion nicht gefunden"));
    }

    @Test
    @DisplayName("PAY-WH-05: Erfolgreicher Webhook markiert die GEBUNDENE Rechnung als bezahlt")
    void erfolgreicheZahlung() throws Exception {
        PaymentProvider stubProvider = stubProvider(true);
        when(paymentService.findeProviderOrNull("stub")).thenReturn(stubProvider);

        UUID rechnungId = UUID.randomUUID();
        when(transactionService.findeRechnungIdNachReferenz("stub", "TX-123"))
                .thenReturn(Optional.of(rechnungId));
        var ergebnis = new PaymentProvider.ZahlungsErgebnis(
                "TX-123", PaymentProvider.ZahlungsStatus.BEZAHLT, null);
        when(paymentService.bestaetigeViaWebhook("stub", "TX-123")).thenReturn(ergebnis);

        mockMvc.perform(post("/payment/webhook/stub")
                .contentType(MediaType.APPLICATION_JSON)
                .content(VALID_BODY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ok"))
                .andExpect(jsonPath("$.zahlungsStatus").value("BEZAHLT"));

        // Markiert die aus dem Store aufgelöste Rechnung — nicht eine Body-ID.
        verify(rechnungService).markiereAlsBezahltViaWebhook(rechnungId.toString());
    }

    @Test
    @DisplayName("PAY-WH-06: Rechnung nicht gefunden beim Markieren → 404")
    void rechnungNichtGefunden() throws Exception {
        PaymentProvider stubProvider = stubProvider(true);
        when(paymentService.findeProviderOrNull("stub")).thenReturn(stubProvider);

        UUID rechnungId = UUID.randomUUID();
        when(transactionService.findeRechnungIdNachReferenz("stub", "TX-123"))
                .thenReturn(Optional.of(rechnungId));
        var ergebnis = new PaymentProvider.ZahlungsErgebnis(
                "TX-123", PaymentProvider.ZahlungsStatus.BEZAHLT, null);
        when(paymentService.bestaetigeViaWebhook("stub", "TX-123")).thenReturn(ergebnis);
        doThrow(new NotFoundException("Nicht gefunden"))
                .when(rechnungService).markiereAlsBezahltViaWebhook(rechnungId.toString());

        mockMvc.perform(post("/payment/webhook/stub")
                .contentType(MediaType.APPLICATION_JSON)
                .content(VALID_BODY))
                .andExpect(status().isNotFound());
    }

    /**
     * PAY-WH-07: Webhook für eine bereits bezahlte Rechnung muss idempotent
     * mit 200 antworten — sonst stuft der Payment-Provider die Lieferung als
     * "failed" ein und retried im Worst Case in einer Endlos-Schleife.
     */
    @Test
    @DisplayName("PAY-WH-07: Bereits bezahlte Rechnung → idempotent ok")
    void bereitsAbgeschlossen() throws Exception {
        PaymentProvider stubProvider = stubProvider(true);
        when(paymentService.findeProviderOrNull("stub")).thenReturn(stubProvider);

        UUID rechnungId = UUID.randomUUID();
        when(transactionService.findeRechnungIdNachReferenz("stub", "TX-123"))
                .thenReturn(Optional.of(rechnungId));
        var ergebnis = new PaymentProvider.ZahlungsErgebnis(
                "TX-123", PaymentProvider.ZahlungsStatus.BEZAHLT, null);
        when(paymentService.bestaetigeViaWebhook("stub", "TX-123")).thenReturn(ergebnis);
        doThrow(new IllegalStateException("Bereits bezahlt"))
                .when(rechnungService).markiereAlsBezahltViaWebhook(rechnungId.toString());

        mockMvc.perform(post("/payment/webhook/stub")
                .contentType(MediaType.APPLICATION_JSON)
                .content(VALID_BODY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.zahlungsStatus").value("BEZAHLT"));
    }

    /** Stub-Provider für Tests — simuliert Signatur-Prüfung + Referenz-Extraktion. */
    private PaymentProvider stubProvider(boolean signaturGueltig) {
        return new PaymentProvider() {
            @Override
            public String providerName() {
                return "stub";
            }

            @Override
            public ZahlungsErgebnis erstelleZahlung(UUID rechnungId, java.math.BigDecimal betragChf,
                    String beschreibung) {
                return new ZahlungsErgebnis("TX", ZahlungsStatus.BEZAHLT, null);
            }

            @Override
            public ZahlungsErgebnis bestaetigeZahlung(String transaktionsId) {
                return new ZahlungsErgebnis(transaktionsId, ZahlungsStatus.BEZAHLT, null);
            }

            @Override
            public ZahlungsErgebnis widerrufe(String transaktionsId) {
                return new ZahlungsErgebnis(transaktionsId, ZahlungsStatus.STORNIERT, null);
            }

            @Override
            public boolean verifiziereSignatur(Map<String, String> headers, String rawBody) {
                return signaturGueltig;
            }

            @Override
            public String extrahiereTransaktionsReferenz(Map<String, Object> payload) {
                Object txId = payload.get("transaktionsId");
                return txId != null ? txId.toString() : null;
            }
        };
    }
}
