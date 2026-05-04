package ch.sponsorplatz.repository;

import ch.sponsorplatz.model.AnfrageStatus;
import ch.sponsorplatz.model.SponsoringAnfrage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface SponsoringAnfrageRepository extends JpaRepository<SponsoringAnfrage, UUID> {

    List<SponsoringAnfrage> findByEmpfaengerOrgIdOrderByCreatedAtDesc(UUID empfaengerOrgId);

    List<SponsoringAnfrage> findByAnfragenderOrgIdOrderByCreatedAtDesc(UUID anfragenderOrgId);

    List<SponsoringAnfrage> findByEmpfaengerOrgIdAndStatusOrderByCreatedAtDesc(UUID empfaengerOrgId, AnfrageStatus status);

    long countByEmpfaengerOrgIdAndStatus(UUID empfaengerOrgId, AnfrageStatus status);
}

