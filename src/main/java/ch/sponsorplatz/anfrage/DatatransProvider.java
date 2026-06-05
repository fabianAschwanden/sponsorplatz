package ch.sponsorplatz.anfrage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Datatrans Payment-Provider für Produktion.
 *
 * <p>Nutzt die Datatrans JSON-API v1:
 * <ul>
 *   <li>POST /v1/transactions — Init (erstellt Hosted-Payment-Page-Session)</li>
 *   <li>GET /v1/transactions/{txId} — Status-Abfrage</li>
 *   <li>POST /v1/transactions/{txId}/cancel — Stornierung</li>
 * </ul>
 *
 * <p>Webhook-Signatur wird via HMAC-SHA256 geprüft (Header {@code Datatrans-Signature}).
 */
@Component
@Profile("prod")
@ConditionalOnProperty(name = "sponsorplatz.payment.datatrans.merchant-id")
public class DatatransProvider implements PaymentProvider {

    private static final Logger log = LoggerFactory.getLogger(DatatransProvider.class);

    private final String merchantId;
    private final String password;
    private final byte[] hmacKey;
    private final String baseUrl;
    private final String successUrl;
    private final String cancelUrl;
    private final String errorUrl;
    private final RestClient restClient;

    public DatatransProvider(
            @Value("${sponsorplatz.payment.datatrans.merchant-id}") String merchantId,
            @Value("${sponsorplatz.payment.datatrans.password}") String password,
            @Value("${sponsorplatz.payment.datatrans.hmac-hex-key}") String hmacHexKey,
            @Value("${sponsorplatz.payment.datatrans.base-url:https://api.datatrans.com}") String baseUrl,
            @Value("${sponsorplatz.payment.datatrans.success-url}") String successUrl,
            @Value("${sponsorplatz.payment.datatrans.cancel-url}") String cancelUrl,
            @Value("${sponsorplatz.payment.datatrans.error-url}") String errorUrl) {
        this.merchantId = merchantId;
        this.password = password;
        this.hmacKey = HexFormat.of().parseHex(hmacHexKey);
        this.baseUrl = baseUrl;
        this.successUrl = successUrl;
        this.cancelUrl = cancelUrl;
        this.errorUrl = errorUrl;
        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .defaultHeaders(h -> h.setBasicAuth(merchantId, password))
                .build();
        log.info("DatatransProvider initialisiert: merchantId={}, baseUrl={}", merchantId, baseUrl);
    }

    /** Konstruktor für Tests (RestClient injizierbar). */
    DatatransProvider(String merchantId, String password, byte[] hmacKey,
                      String baseUrl, String successUrl, String cancelUrl,
                      String errorUrl, RestClient restClient) {
        this.merchantId = merchantId;
        this.password = password;
        this.hmacKey = hmacKey;
        this.baseUrl = baseUrl;
        this.successUrl = successUrl;
        this.cancelUrl = cancelUrl;
        this.errorUrl = errorUrl;
        this.restClient = restClient;
    }

    @Override
    public String providerName() {
        return "datatrans";
    }

    @Override
    @SuppressWarnings("unchecked")
    public ZahlungsErgebnis erstelleZahlung(UUID rechnungId, BigDecimal betragChf, String beschreibung) {
        int betragRappen = betragChf.multiply(BigDecimal.valueOf(100)).intValue();

        Map<String, Object> body = Map.of(
                "currency", "CHF",
                "refno", rechnungId.toString(),
                "amount", betragRappen,
                "paymentMethods", List.of("TWI", "PFC", "VIS", "ECA"),
                "redirect", Map.of(
                        "successUrl", successUrl.replace("{rechnungId}", rechnungId.toString()),
                        "cancelUrl", cancelUrl.replace("{rechnungId}", rechnungId.toString()),
                        "errorUrl", errorUrl.replace("{rechnungId}", rechnungId.toString())
                )
        );

        Map<String, Object> response = restClient.post()
                .uri("/v1/transactions")
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .body(Map.class);

        String transactionId = (String) response.get("transactionId");
        // Datatrans HPP-URL: https://pay.datatrans.com/v1/start/{txId}
        String payUrl = baseUrl.replace("api.", "pay.") + "/v1/start/" + transactionId;

        log.info("Datatrans-Transaktion erstellt: txId={}, betrag={} Rappen, rechnung={}",
                transactionId, betragRappen, rechnungId);

        return new ZahlungsErgebnis(transactionId, ZahlungsStatus.ERSTELLT, payUrl);
    }

    @Override
    @SuppressWarnings("unchecked")
    public ZahlungsErgebnis bestaetigeZahlung(String transaktionsId) {
        Map<String, Object> response = restClient.get()
                .uri("/v1/transactions/{txId}", transaktionsId)
                .retrieve()
                .body(Map.class);

        String status = (String) response.get("status");
        ZahlungsStatus mappedStatus = mapStatus(status);

        log.info("Datatrans-Status abgefragt: txId={}, status={} → {}", transaktionsId, status, mappedStatus);
        return new ZahlungsErgebnis(transaktionsId, mappedStatus, null);
    }

    @Override
    public ZahlungsErgebnis widerrufe(String transaktionsId) {
        restClient.post()
                .uri("/v1/transactions/{txId}/cancel", transaktionsId)
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("refno", transaktionsId))
                .retrieve()
                .toBodilessEntity();

        log.info("Datatrans-Transaktion storniert: txId={}", transaktionsId);
        return new ZahlungsErgebnis(transaktionsId, ZahlungsStatus.STORNIERT, null);
    }

    /**
     * Verifiziert die Datatrans-Webhook-Signatur nach dem offiziellen Schema:
     * Der Header {@code Datatrans-Signature} hat die Form {@code t=<ts>,s0=<hmac>},
     * wobei {@code <hmac> = HMAC-SHA256(key, ts + rawBody)} (hex). Der Timestamp
     * fliesst also in die HMAC ein — eine Prüfung nur über den Body würde echte
     * Datatrans-Callbacks immer ablehnen.
     */
    @Override
    public boolean verifiziereSignatur(Map<String, String> headers, String rawBody) {
        String header = headers.get("datatrans-signature");
        if (header == null || header.isBlank()) {
            log.warn("Datatrans-Webhook ohne Signatur-Header empfangen");
            return false;
        }
        // Header parsen: "t=1561364320,s0=82e8b3653…"
        String timestamp = null;
        String signatur = null;
        for (String teil : header.split(",")) {
            String[] kv = teil.trim().split("=", 2);
            if (kv.length != 2) continue;
            if ("t".equals(kv[0])) timestamp = kv[1];
            else if ("s0".equals(kv[0])) signatur = kv[1];
        }
        if (timestamp == null || signatur == null) {
            log.warn("Datatrans-Signatur-Header unvollständig (erwartet t=…,s0=…)");
            return false;
        }

        String berechnet = hmacSha256Hex(timestamp + rawBody);
        boolean gueltig = MessageDigest.isEqual(
                berechnet.getBytes(StandardCharsets.UTF_8),
                signatur.getBytes(StandardCharsets.UTF_8));
        if (!gueltig) {
            // Keine Signatur-Fragmente loggen — Log-Hygiene.
            log.warn("Datatrans-Webhook HMAC-Signatur ungültig");
        }
        return gueltig;
    }

    @Override
    public String extrahiereTransaktionsReferenz(Map<String, Object> payload) {
        Object txId = payload.get("transactionId");
        return txId != null ? txId.toString() : null;
    }

    private String hmacSha256Hex(String data) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(hmacKey, "HmacSHA256"));
            byte[] hash = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (java.security.NoSuchAlgorithmException | java.security.InvalidKeyException e) {
            throw new IllegalStateException("HMAC-Berechnung fehlgeschlagen", e);
        }
    }

    /**
     * Mappt einen Datatrans-Transaktionsstatus auf den internen Status.
     *
     * <p><strong>Capture-Modell:</strong> Diese Integration kennt keinen
     * separaten Capture-Schritt — die Datatrans-Transaktion MUSS daher mit
     * Auto-Settle konfiguriert sein ({@code autoSettle: true}, Default vieler
     * Setups). Dann liefert der Webhook direkt {@code settled}. Ein
     * {@code authorized}-Status bedeutet, dass nur autorisiert, aber nicht
     * eingezogen wurde (manuelles Capture nötig) — das bleibt bewusst auf
     * {@code ERSTELLT} (= noch nicht bezahlt) und wird geloggt, damit eine
     * Fehlkonfiguration sichtbar wird statt unbemerkt als bezahlt zu gelten.
     */
    private ZahlungsStatus mapStatus(String datatransStatus) {
        if (datatransStatus == null) return ZahlungsStatus.FEHLGESCHLAGEN;
        return switch (datatransStatus.toLowerCase()) {
            case "settled", "transmitted" -> ZahlungsStatus.BEZAHLT;
            case "authorized" -> {
                log.warn("Datatrans-Status 'authorized' (nicht captured) — Transaktion bleibt "
                        + "unbezahlt. Auto-Settle prüfen (autoSettle:true), sonst manuelles Capture nötig.");
                yield ZahlungsStatus.ERSTELLT;
            }
            case "canceled", "failed" -> ZahlungsStatus.FEHLGESCHLAGEN;
            default -> {
                log.warn("Unbekannter Datatrans-Status: {}", datatransStatus);
                yield ZahlungsStatus.FEHLGESCHLAGEN;
            }
        };
    }
}

