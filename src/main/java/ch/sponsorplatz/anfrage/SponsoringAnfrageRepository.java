package ch.sponsorplatz.anfrage;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

@Repository
public interface SponsoringAnfrageRepository extends JpaRepository<SponsoringAnfrage, UUID> {

    List<SponsoringAnfrage> findByEmpfaengerOrgIdOrderByCreatedAtDesc(UUID empfaengerOrgId);

    List<SponsoringAnfrage> findByAnfragenderOrgIdOrderByCreatedAtDesc(UUID anfragenderOrgId);

    List<SponsoringAnfrage> findByEmpfaengerOrgIdAndStatusOrderByCreatedAtDesc(UUID empfaengerOrgId, AnfrageStatus status);

    long countByEmpfaengerOrgIdAndStatus(UUID empfaengerOrgId, AnfrageStatus status);

    /** Aggregat-Count für Dashboard: alle eingehenden Anfragen mehrerer Orgs in einer Query. */
    long countByEmpfaengerOrgIdIn(Collection<UUID> empfaengerOrgIds);

    /** Aggregat-Count für Dashboard: Anfragen mehrerer Orgs nach Status filtern. */
    long countByEmpfaengerOrgIdInAndStatus(Collection<UUID> empfaengerOrgIds, AnfrageStatus status);
}

