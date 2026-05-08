package ch.sponsorplatz.anfrage;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

/**
 * Abstraktes Interface fuer Zahlungs-Provider.
 * Implementierungen: LokalerStubProvider (dev/test), DatatransProvider (prod).
 */
public interface PaymentProvider {

    /**
     * Erstellt eine Zahlung beim Provider.
     * @return Provider-spezifische Transaktions-ID
     */
    ZahlungsErgebnis erstelleZahlung(UUID rechnungId, BigDecimal betragChf, String beschreibung);

    /**
     * Bestaetigt eine Zahlung (idempotent).
     */
    ZahlungsErgebnis bestaetigeZahlung(String transaktionsId);

    /**
     * Widerruft/storniert eine Zahlung.
     */
    ZahlungsErgebnis widerrufe(String transaktionsId);

    /**
     * Name des Providers (fuer Routing im Webhook-Controller).
     */
    String providerName();

    /**
     * Verifiziert die Authentizität eines eingehenden Webhook-Aufrufs anhand
     * von Header-Signatur und Roh-Body. Echte Provider (Datatrans, Stripe)
     * berechnen eine HMAC-SHA256 über den Roh-Body mit einem geteilten Secret —
     * ohne diese Prüfung könnte jeder die Rechnungen frei als bezahlt markieren.
     *
     * <p>Wichtig: Der Controller MUSS den ROH-Body übergeben (vor JSON-Parsing),
     * weil Signaturen byteweise berechnet werden.
     *
     * @return true wenn die Signatur stimmt; false → Controller antwortet 401.
     */
    boolean verifiziereSignatur(Map<String, String> headers, String rawBody);

    record ZahlungsErgebnis(String transaktionsId, ZahlungsStatus status, String meldung) {}

    enum ZahlungsStatus {
        ERSTELLT, BEZAHLT, FEHLGESCHLAGEN, STORNIERT
    }
}

