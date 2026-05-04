package ch.sponsorplatz.repository;

import ch.sponsorplatz.model.Mitgliedschaft;
import ch.sponsorplatz.model.Rolle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

@Repository
public interface MitgliedschaftRepository extends JpaRepository<Mitgliedschaft, UUID> {

    boolean existsByUserIdAndOrgId(UUID userId, UUID orgId);

    boolean existsByUserIdAndOrgIdAndRolleIn(UUID userId, UUID orgId, Collection<Rolle> rollen);

    boolean existsByUserIdAndOrgIdAndRolle(UUID userId, UUID orgId, Rolle rolle);

    List<Mitgliedschaft> findByOrgId(UUID orgId);

    boolean existsByOrgId(UUID orgId);
}

