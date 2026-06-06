package ch.sponsorplatz.anfrage.payment;

import ch.sponsorplatz.anfrage.Rechnung;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/**
 * Persistenz für Payment-Transaktionen — Audit-Trail und Idempotenz-Schutz.
 */
@Service
@Transactional
public class PaymentTransactionService {

    private static final Logger log = LoggerFactory.getLogger(PaymentTransactionService.class);
    private final PaymentTransactionRepository repository;

    public PaymentTransactionService(PaymentTransactionRepository repository) {
        this.repository = repository;
    }

    /**
     * Speichert eine neue Transaktion (nach Provider-Init). Wirft bei
     * doppelter provider_reference eine Exception (DB-UNIQUE-Constraint).
     */
    public PaymentTransaction speichere(Rechnung rechnung, String provider,
                                         String providerReference, BigDecimal betragChf,
                                         String checkoutUrl) {
        PaymentTransaction tx = new PaymentTransaction();
        tx.setId(UUID.randomUUID());
        tx.setRechnung(rechnung);
        tx.setProvider(provider);
        tx.setProviderReference(providerReference);
        tx.setStatus(PaymentTransactionStatus.ERSTELLT);
        tx.setBetragChf(betragChf);
        tx.setCheckoutUrl(checkoutUrl);
        tx.setErstelltAm(Instant.now());

        PaymentTransaction gespeichert = repository.save(tx);
        log.info("PaymentTransaction gespeichert: id={}, provider={}, ref={}, betrag={}",
                gespeichert.getId(), provider, providerReference, betragChf);
        return gespeichert;
    }

    /**
     * Aktualisiert den Status einer existierenden Transaktion (nach Webhook).
     */
    public Optional<PaymentTransaction> aktualisiereStatus(String provider, String providerReference,
                                                            PaymentTransactionStatus neuerStatus,
                                                            String rawPayload) {
        Optional<PaymentTransaction> opt = repository.findByProviderAndProviderReference(provider, providerReference);
        opt.ifPresent(tx -> {
            tx.setStatus(neuerStatus);
            tx.setAktualisiertAm(Instant.now());
            tx.setRawPayload(rawPayload);
            repository.save(tx);
            log.info("PaymentTransaction aktualisiert: id={}, neuerStatus={}", tx.getId(), neuerStatus);
        });
        return opt;
    }

    @Transactional(readOnly = true)
    public Optional<PaymentTransaction> findeNachRechnungUndProvider(UUID rechnungId, String provider) {
        return repository.findByRechnungIdAndProvider(rechnungId, provider);
    }

    /**
     * Liefert die <em>verbindlich</em> mit einer Provider-Transaktion verknüpfte
     * Rechnungs-ID — die Quelle der Wahrheit für den Webhook, statt einer
     * Rechnungs-ID aus dem Request-Body (Confused-Deputy-Schutz).
     */
    @Transactional(readOnly = true)
    public Optional<UUID> findeRechnungIdNachReferenz(String provider, String providerReference) {
        return repository.findByProviderAndProviderReference(provider, providerReference)
                .map(tx -> tx.getRechnung().getId());
    }
}

