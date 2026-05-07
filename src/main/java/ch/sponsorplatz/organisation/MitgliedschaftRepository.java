package ch.sponsorplatz.organisation;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
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

    List<Mitgliedschaft> findByUserId(UUID userId);

    boolean existsByOrgId(UUID orgId);

    /**
     * Direkte Projection auf nur die org_id-Spalte — vermeidet das Laden
     * von Mitgliedschaft + Lazy-Load der Org. Wird vom DashboardService
     * verwendet, um aus N+1 = 1 Query zu machen.
     */
    @Query("select m.org.id from Mitgliedschaft m where m.user.id = :userId")
    List<UUID> findOrgIdsByUserId(@Param("userId") UUID userId);
}
