package ch.sponsorplatz.anfrage;

import ch.sponsorplatz.shared.exception.NotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Delegiert Zahlungs-Operationen an den aktiven {@link PaymentProvider}.
 * Bei mehreren Providern wird anhand des Provider-Namens geroutet.
 */
@Service
public class PaymentService {

    private static final Logger log = LoggerFactory.getLogger(PaymentService.class);
    private final Map<String, PaymentProvider> providerMap;

    public PaymentService(List<PaymentProvider> providers) {
        this.providerMap = providers.stream()
                .collect(Collectors.toMap(PaymentProvider::providerName, Function.identity()));
        log.info("PaymentService initialisiert mit Providern: {}", providerMap.keySet());
    }

    public PaymentProvider.ZahlungsErgebnis erstelleZahlung(UUID rechnungId, BigDecimal betragChf, String beschreibung) {
        PaymentProvider provider = standardProvider();
        return provider.erstelleZahlung(rechnungId, betragChf, beschreibung);
    }

    public PaymentProvider.ZahlungsErgebnis bestaetigeViaWebhook(String providerName, String transaktionsId) {
        PaymentProvider provider = findeProvider(providerName);
        return provider.bestaetigeZahlung(transaktionsId);
    }

    private PaymentProvider standardProvider() {
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
}

