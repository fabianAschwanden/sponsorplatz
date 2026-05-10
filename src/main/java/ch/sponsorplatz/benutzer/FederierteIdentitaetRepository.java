package ch.sponsorplatz.benutzer;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface FederierteIdentitaetRepository extends JpaRepository<FederierteIdentitaet, UUID> {

    /**
     * Stabiler Lookup-Pfad: subject ist beim IdP unveränderlich, Email kann
     * sich ändern. Wird bei jedem OIDC-Login zuerst probiert (Spec §6.1, Stufe 1).
     */
    Optional<FederierteIdentitaet> findByProviderAndSubject(IdentityProvider provider, String subject);

    List<FederierteIdentitaet> findByUserId(UUID userId);
}
