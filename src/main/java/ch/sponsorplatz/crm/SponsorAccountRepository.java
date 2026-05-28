package ch.sponsorplatz.crm;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository für {@link SponsorAccount}. ALLE Finder sind auf den Mandanten-
 * Schlüssel {@code besitzerSponsorOrgId} gescoped — es gibt bewusst kein
 * ungefiltertes {@code findAll()} im Anwendungspfad. Der Zugriffs-Check
 * ({@code kannSponsorDatenSehen}) liegt im {@code SponsorAccountService};
 * ARCH-01 verbietet Controller-Direktzugriff aufs Repository.
 */
@Repository
public interface SponsorAccountRepository extends JpaRepository<SponsorAccount, UUID> {

    List<SponsorAccount> findByBesitzerSponsorOrgIdOrderByErstelltAmDesc(UUID besitzerSponsorOrgId);

    Optional<SponsorAccount> findByBesitzerSponsorOrgIdAndVereinId(UUID besitzerSponsorOrgId, UUID vereinId);

    boolean existsByBesitzerSponsorOrgIdAndVereinId(UUID besitzerSponsorOrgId, UUID vereinId);
}
