package ch.sponsorplatz.crm;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
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

    /**
     * DB-Level-Bulk-Delete für die Bulk-„Entfernen"-Aktion. JPQL-DML löst die
     * FK-{@code ON DELETE CASCADE} aus (kontakt_person, aktivitaet gehen mit);
     * eine JPA-Entity-Cascade existiert bewusst nicht (separate Aggregate).
     * {@code clearAutomatically} verhindert stale L1-Cache nach dem DML-Delete.
     */
    @Modifying(clearAutomatically = true)
    @Query("delete from SponsorAccount a where a.id in :ids")
    void deleteByIdIn(@Param("ids") Collection<UUID> ids);
}
