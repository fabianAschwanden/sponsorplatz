package ch.sponsorplatz.anfrage;

import ch.sponsorplatz.shared.exception.NotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Delegiert Zahlungs-Operationen an den aktiven {@link PaymentProvider}.
 * Bei mehreren Providern wird anhand des Provider-Namens geroutet.
 *
 * <p>Erweiterung Phase 15.1: {@link #erstelleCheckoutSession(Rechnung)} erstellt
 * eine Hosted-Payment-Page-Session beim Provider und persistiert die Transaktion.
 */
@Service
public class PaymentService {

    private static final Logger log = LoggerFactory.getLogger(PaymentService.class);
    private final Map<String, PaymentProvider> providerMap;
    private final PaymentTransactionService transactionService;
    private final RechnungRepository rechnungRepository;

    public PaymentService(List<PaymentProvider> providers,
                          PaymentTransactionService transactionService,
                          RechnungRepository rechnungRepository) {
        this.providerMap = providers.stream()
                .collect(Collectors.toMap(PaymentProvider::providerName, Function.identity()));
        this.transactionService = transactionService;
        this.rechnungRepository = rechnungRepository;
        log.info("PaymentService initialisiert mit Providern: {}", providerMap.keySet());
    }

    public PaymentProvider.ZahlungsErgebnis erstelleZahlung(UUID rechnungId, BigDecimal betragChf, String beschreibung) {
        PaymentProvider provider = standardProvider();
        return provider.erstelleZahlung(rechnungId, betragChf, beschreibung);
    }

    /**
     * Erstellt eine Checkout-Session beim Standard-Provider und persistiert die
     * Transaktion. Gibt die Checkout-URL zurück (Redirect-Ziel für den Sponsor).
     *
     * @throws IllegalStateException wenn Rechnung nicht OFFEN oder bereits eine
     *                               aktive Transaktion existiert
     */
    public String erstelleCheckoutSession(UUID rechnungId) {
        Rechnung rechnung = rechnungRepository.findById(rechnungId)
                .orElseThrow(() -> new NotFoundException("Rechnung nicht gefunden: " + rechnungId));
        return erstelleCheckoutSessionIntern(rechnung);
    }

    private String erstelleCheckoutSessionIntern(Rechnung rechnung) {
        if (rechnung.getStatus() != RechnungsStatus.OFFEN) {
            throw new IllegalStateException("Nur offene Rechnungen können online bezahlt werden.");
        }

        PaymentProvider provider = standardProvider();

        // Prüfe ob bereits eine aktive Transaktion für diese Rechnung existiert
        Optional<PaymentTransaction> bestehend = transactionService
                .findeNachRechnungUndProvider(rechnung.getId(), provider.providerName());
        if (bestehend.isPresent() && bestehend.get().getCheckoutUrl() != null) {
            // Existierende Session wiederverwenden (Idempotenz)
            log.info("Bestehende Checkout-Session gefunden für Rechnung {}", rechnung.getId());
            return bestehend.get().getCheckoutUrl();
        }

        String beschreibung = rechnung.getZahlungszweck() != null
                ? rechnung.getZahlungszweck()
                : "Rechnung " + rechnung.getRechnungsnummer();

        PaymentProvider.ZahlungsErgebnis ergebnis = provider.erstelleZahlung(
                rechnung.getId(), rechnung.getBetragChf(), beschreibung);

        // Transaktion persistieren
        transactionService.speichere(
                rechnung,
                provider.providerName(),
                ergebnis.transaktionsId(),
                rechnung.getBetragChf(),
                ergebnis.checkoutUrl()
        );

        return ergebnis.checkoutUrl();
    }

    public PaymentProvider.ZahlungsErgebnis bestaetigeViaWebhook(String providerName, String transaktionsId) {
        PaymentProvider provider = findeProvider(providerName);
        PaymentProvider.ZahlungsErgebnis ergebnis = provider.bestaetigeZahlung(transaktionsId);

        // Transaktion im lokalen Store aktualisieren
        PaymentTransactionStatus mappedStatus = mapZahlungsStatus(ergebnis.status());
        transactionService.aktualisiereStatus(providerName, transaktionsId, mappedStatus, null);

        return ergebnis;
    }

    private PaymentProvider standardProvider() {
        if (providerMap.isEmpty()) {
            // Kein Provider geladen (z.B. prod im QR-Rechnung-Modus ohne Datatrans-
            // Credentials). Online-Checkout ist dann nicht verfügbar — der Button
            // wird ohnehin nur bei aktivem Online-Modus angezeigt.
            throw new IllegalStateException("Kein Online-Payment-Provider konfiguriert.");
        }
        if (providerMap.size() == 1) {
            return providerMap.values().iterator().next();
        }
        // Bevorzuge nicht-stub in prod
        return providerMap.values().stream()
                .filter(p -> !"stub".equals(p.providerName()))
                .findFirst()
                .orElse(providerMap.values().iterator().next());
    }

    private PaymentProvider findeProvider(String name) {
        PaymentProvider provider = providerMap.get(name);
        if (provider == null) {
            throw new NotFoundException("Payment-Provider nicht gefunden: " + name);
        }
        return provider;
    }

    /**
     * Lookup ohne Throw — der Webhook-Controller will einen 404-Body
     * zurückgeben, keinen Stacktrace bei unbekanntem Provider-Namen.
     */
    public PaymentProvider findeProviderOrNull(String name) {
        return providerMap.get(name);
    }

    private PaymentTransactionStatus mapZahlungsStatus(PaymentProvider.ZahlungsStatus status) {
        return switch (status) {
            case ERSTELLT -> PaymentTransactionStatus.ERSTELLT;
            case BEZAHLT -> PaymentTransactionStatus.BEZAHLT;
            case FEHLGESCHLAGEN -> PaymentTransactionStatus.FEHLGESCHLAGEN;
            case STORNIERT -> PaymentTransactionStatus.STORNIERT;
        };
    }
}

