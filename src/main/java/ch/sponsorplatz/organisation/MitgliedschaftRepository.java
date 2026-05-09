package ch.sponsorplatz.organisation;
import ch.sponsorplatz.projekt.DashboardService;

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

    /**
     * AccessControl-Vererbungs-Lookup in EINEM Query: prüft ob der User
     * eine passende Mitgliedschaft auf der Ziel-Org ODER irgendeiner Eltern-Org
     * in der Hierarchie-Kette hat. Ersetzt die N×2-Iteration (Mitglied-Check +
     * findById pro Stufe) durch ein rekursives CTE.
     *
     * <p>Funktioniert auf Postgres und auf H2 (mit MODE=PostgreSQL).
     * Rollen werden als Strings übergeben, weil das {@code rolle}-Spalte
     * der DB ein VARCHAR mit Enum-Namen ist — Spring Data JPA serialisiert
     * Enum-Collections in native Queries nicht zuverlässig.
     */
    @Query(value = """
            WITH RECURSIVE elternkette(id, uebergeordnete_org_id) AS (
                SELECT id, uebergeordnete_org_id
                  FROM organisation
                 WHERE id = :startId
                UNION ALL
                SELECT o.id, o.uebergeordnete_org_id
                  FROM organisation o
                  JOIN elternkette e ON e.uebergeordnete_org_id = o.id
            )
            SELECT COUNT(*) FROM mitgliedschaft m
             WHERE m.user_id = :userId
               AND m.org_id IN (SELECT id FROM elternkette)
               AND m.rolle IN (:rollen)
            """, nativeQuery = true)
    long zaehleMitgliedschaftenInHierarchie(
            @Param("userId") UUID userId,
            @Param("startId") UUID startId,
            @Param("rollen") Collection<String> rollen);
}
