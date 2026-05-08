package ch.sponsorplatz.anfrage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Lokaler Stub-Provider fuer dev/test: Zahlungen werden sofort als BEZAHLT markiert.
 * Kein externer Aufruf, keine Konfiguration noetig.
 */
@Component
@Profile({"dev", "test", "demo"})
public class LokalerStubProvider implements PaymentProvider {

    private static final Logger log = LoggerFactory.getLogger(LokalerStubProvider.class);
    private final Map<String, ZahlungsStatus> transaktionen = new ConcurrentHashMap<>();

    @Override
    public ZahlungsErgebnis erstelleZahlung(UUID rechnungId, BigDecimal betragChf, String beschreibung) {
        String txId = "STUB-" + UUID.randomUUID();
        transaktionen.put(txId, ZahlungsStatus.BEZAHLT);
        log.info("STUB: Zahlung erstellt und sofort bezahlt — txId={}, betrag={} CHF", txId, betragChf);
        return new ZahlungsErgebnis(txId, ZahlungsStatus.BEZAHLT, "Stub: sofort bezahlt");
    }

    @Override
    public ZahlungsErgebnis bestaetigeZahlung(String transaktionsId) {
        ZahlungsStatus status = transaktionen.getOrDefault(transaktionsId, ZahlungsStatus.BEZAHLT);
        return new ZahlungsErgebnis(transaktionsId, status, "Stub: bestaetigt");
    }

    @Override
    public ZahlungsErgebnis widerrufe(String transaktionsId) {
        transaktionen.put(transaktionsId, ZahlungsStatus.STORNIERT);
        return new ZahlungsErgebnis(transaktionsId, ZahlungsStatus.STORNIERT, "Stub: storniert");
    }

    @Override
    public String providerName() {
        return "stub";
    }

    /**
     * Stub akzeptiert alle Signaturen — gilt nur in dev/test/demo (siehe
     * {@code @Profile}). In prod wird ein echter Provider geladen, dessen
     * verifiziereSignatur HMAC-SHA256 prüft.
     */
    @Override
    public boolean verifiziereSignatur(Map<String, String> headers, String rawBody) {
        log.debug("STUB: Signatur ohne Prüfung akzeptiert (dev/test/demo only)");
        return true;
    }
}

