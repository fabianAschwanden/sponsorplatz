package ch.sponsorplatz.anfrage;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

/**
 * Repository für Payment-Transaktionen.
 */
public interface PaymentTransactionRepository extends JpaRepository<PaymentTransaction, UUID> {

    Optional<PaymentTransaction> findByProviderAndProviderReference(String provider, String providerReference);

    Optional<PaymentTransaction> findByRechnungIdAndProvider(UUID rechnungId, String provider);
}

