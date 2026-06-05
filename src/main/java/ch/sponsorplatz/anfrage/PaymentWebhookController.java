package ch.sponsorplatz.anfrage;

import ch.sponsorplatz.shared.exception.NotFoundException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Empfaengt Webhook-Callbacks von Payment-Providern.
 * Route: POST /payment/webhook/{provider}
 *
 * <p>Sicherheits-Pfad:
 * <ol>
 *   <li>ROH-Body lesen → {@link PaymentProvider#verifiziereSignatur} → 401 bei Mismatch.</li>
 *   <li>Body parsen → Provider extrahiert seine Transaktions-Referenz.</li>
 *   <li>Die zugehörige Rechnung wird <strong>lokal</strong> über die gespeicherte
 *       {@link PaymentTransaction} aufgelöst — NICHT aus dem Request-Body
 *       (Confused-Deputy-Schutz: ein Angreifer könnte sonst eine fremde
 *       Rechnungs-ID unterschieben).</li>
 *   <li>Status beim Provider bestätigen → bei BEZAHLT die gebundene Rechnung markieren.</li>
 * </ol>
 */
@RestController
@RequestMapping("/payment/webhook")
public class PaymentWebhookController {

    private static final Logger log = LoggerFactory.getLogger(PaymentWebhookController.class);
    private final PaymentService paymentService;
    private final RechnungService rechnungService;
    private final PaymentTransactionService transactionService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public PaymentWebhookController(PaymentService paymentService,
                                    RechnungService rechnungService,
                                    PaymentTransactionService transactionService) {
        this.paymentService = paymentService;
        this.rechnungService = rechnungService;
        this.transactionService = transactionService;
    }

    @PostMapping("/{provider}")
    public ResponseEntity<Map<String, String>> webhook(
            @PathVariable String provider,
            @RequestBody String rawBody,
            HttpServletRequest request) {

        // Header-Map case-insensitive aufbauen (lowercase keys), damit Provider-
        // Implementierungen einheitlich auf "datatrans-signature" o.ä. zugreifen können.
        Map<String, String> headers = new HashMap<>();
        Collections.list(request.getHeaderNames())
                .forEach(name -> headers.put(name.toLowerCase(), request.getHeader(name)));

        // 1. Provider-Lookup ohne Throw — unbekannter Provider → 404, kein Stacktrace.
        PaymentProvider providerImpl = paymentService.findeProviderOrNull(provider);
        if (providerImpl == null) {
            log.warn("Webhook für unbekannten Provider abgelehnt: {}", provider);
            return ResponseEntity.status(404)
                    .body(Map.of("status", "error", "message", "Provider nicht gefunden"));
        }

        // 2. Signatur ZUERST prüfen — vor JSON-Parsing, vor Side-Effects.
        if (!providerImpl.verifiziereSignatur(headers, rawBody)) {
            log.warn("Webhook-Signatur ungültig: provider={}, ip={}", provider, request.getRemoteAddr());
            return ResponseEntity.status(401)
                    .body(Map.of("status", "error", "message", "Signatur ungültig"));
        }

        // 3. Erst nach erfolgreicher Verifikation: Body parsen.
        Map<String, Object> payload;
        try {
            payload = objectMapper.readValue(rawBody, new TypeReference<>() {});
        } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("status", "error", "message", "Ungültiger JSON-Body"));
        }

        // 4. Provider-spezifische Transaktions-Referenz extrahieren.
        String transaktionsId = providerImpl.extrahiereTransaktionsReferenz(payload);
        if (transaktionsId == null || transaktionsId.isBlank()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("status", "error", "message", "Transaktions-Referenz fehlt"));
        }

        // 5. Verbindliche Rechnung aus dem lokalen Transaktions-Store ableiten
        //    (NICHT aus dem Body — Confused-Deputy-Schutz).
        Optional<UUID> rechnungId = transactionService.findeRechnungIdNachReferenz(provider, transaktionsId);
        if (rechnungId.isEmpty()) {
            log.warn("Webhook für unbekannte Transaktion: provider={}, txId={}", provider, transaktionsId);
            return ResponseEntity.status(404)
                    .body(Map.of("status", "error", "message", "Transaktion nicht gefunden"));
        }

        log.info("Webhook empfangen: provider={}, txId={}, rechnungId={}",
                provider, transaktionsId, rechnungId.get());

        PaymentProvider.ZahlungsErgebnis ergebnis = paymentService.bestaetigeViaWebhook(provider, transaktionsId);

        if (ergebnis.status() == PaymentProvider.ZahlungsStatus.BEZAHLT) {
            try {
                rechnungService.markiereAlsBezahltViaWebhook(rechnungId.get().toString());
            } catch (NotFoundException e) {
                return ResponseEntity.status(404)
                        .body(Map.of("status", "error", "message", "Rechnung nicht gefunden"));
            } catch (IllegalStateException e) {
                // Idempotenz: bereits bezahlt
                log.info("Rechnung {} bereits bezahlt (idempotent)", rechnungId.get());
            }
        }

        return ResponseEntity.ok(Map.of("status", "ok", "zahlungsStatus", ergebnis.status().name()));
    }
}
