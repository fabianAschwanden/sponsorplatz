package ch.sponsorplatz.crm;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Repository für {@link KontaktPerson}. Finder sind auf den Account gescoped —
 * der Zugriffs-Check ({@code kannSponsorDatenSehen}) liegt im
 * {@code KontaktPersonService}, ARCH-01 verbietet Controller-Direktzugriff.
 */
@Repository
public interface KontaktPersonRepository extends JpaRepository<KontaktPerson, UUID> {

    List<KontaktPerson> findByAccountIdOrderByNachnameAscVornameAsc(UUID accountId);
}
