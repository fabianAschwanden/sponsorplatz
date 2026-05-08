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

/**
 * Empfaengt Webhook-Callbacks von Payment-Providern.
 * Route: POST /payment/webhook/{provider}
 *
 * <p>Sicherheits-Pfad: ROH-Body wird gelesen → an
 * {@link PaymentProvider#verifiziereSignatur(Map, String)} delegiert →
 * 401 bei Mismatch → erst dann JSON-Parsing und Side-Effect. Echte Provider
 * (Datatrans/Stripe) prüfen HMAC-SHA256 über die exakten Roh-Bytes.
 */
@RestController
@RequestMapping("/payment/webhook")
public class PaymentWebhookController {

    private static final Logger log = LoggerFactory.getLogger(PaymentWebhookController.class);
    private final PaymentService paymentService;
    private final RechnungService rechnungService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public PaymentWebhookController(PaymentService paymentService, RechnungService rechnungService) {
        this.paymentService = paymentService;
        this.rechnungService = rechnungService;
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
        Map<String, String> payload;
        try {
            payload = objectMapper.readValue(rawBody, new TypeReference<>() {});
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("status", "error", "message", "Ungültiger JSON-Body"));
        }

        String transaktionsId = payload.get("transaktionsId");
        String rechnungIdStr = payload.get("rechnungId");

        if (transaktionsId == null || rechnungIdStr == null) {
            return ResponseEntity.badRequest()
                    .body(Map.of("status", "error", "message", "transaktionsId und rechnungId erforderlich"));
        }

        log.info("Webhook empfangen: provider={}, txId={}, rechnungId={}",
                provider, transaktionsId, rechnungIdStr);

        PaymentProvider.ZahlungsErgebnis ergebnis = paymentService.bestaetigeViaWebhook(provider, transaktionsId);

        if (ergebnis.status() == PaymentProvider.ZahlungsStatus.BEZAHLT) {
            try {
                rechnungService.markiereAlsBezahltViaWebhook(rechnungIdStr);
            } catch (NotFoundException e) {
                return ResponseEntity.status(404)
                        .body(Map.of("status", "error", "message", "Rechnung nicht gefunden"));
            } catch (IllegalStateException e) {
                // Idempotenz: bereits bezahlt
                log.info("Rechnung {} bereits bezahlt (idempotent)", rechnungIdStr);
            }
        }

        return ResponseEntity.ok(Map.of("status", "ok", "zahlungsStatus", ergebnis.status().name()));
    }
}
