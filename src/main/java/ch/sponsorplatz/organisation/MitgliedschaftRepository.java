package ch.sponsorplatz.organisation;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface MitgliedschaftRepository extends JpaRepository<Mitgliedschaft, UUID> {

    boolean existsByUserIdAndOrgId(UUID userId, UUID orgId);

    boolean existsByUserIdAndOrgIdAndRolleIn(UUID userId, UUID orgId, Collection<Rolle> rollen);

    boolean existsByUserIdAndOrgIdAndRolle(UUID userId, UUID orgId, Rolle rolle);

    /**
     * JOIN FETCH user — verhindert {@code LazyInitializationException} im
     * Template, da {@code spring.jpa.open-in-view=false} und {@code MitgliedView.von}
     * auf {@code user.getAnzeigename()} / {@code user.getProfilbildId()} zugreift,
     * nachdem die Service-Transaktion bereits geschlossen ist.
     */
    @Query("SELECT m FROM Mitgliedschaft m JOIN FETCH m.user WHERE m.org.id = :orgId")
    List<Mitgliedschaft> findByOrgId(@Param("orgId") UUID orgId);

    List<Mitgliedschaft> findByUserId(UUID userId);

    /**
     * Slugs der Sponsor-Orgs eines bestimmten Typs, auf denen der User
     * (per E-Mail) eine der angegebenen Rollen hat — Projektion nur auf den
     * Slug, kein Entity-Load. Für die Sidebar: zeigt den CRM-Einstieg nur
     * Mitgliedern einer Firma mit Bearbeitungsrechten. Nach Org-Name sortiert,
     * damit der erste Treffer deterministisch ist.
     */
    @Query("""
            select m.org.slug from Mitgliedschaft m
             where lower(m.user.email) = :email
               and m.rolle in :rollen
               and m.org.typ = :typ
             order by m.org.name asc
            """)
    List<String> findSponsorOrgSlugs(@Param("email") String email,
                                     @Param("rollen") Collection<Rolle> rollen,
                                     @Param("typ") OrgTyp typ);

    /**
     * Mitgliedschaften eines Users gefiltert nach Rolle, mit eager geladener
     * Org — für UI-Listen, die Org-Namen ausserhalb der Service-Tx brauchen
     * (z.B. Anfrage-Erstellungs-Form, die den anfragenderOrg-Select aufbaut).
     */
    @Query("""
            select m from Mitgliedschaft m
              join fetch m.org
             where m.user.id = :userId
               and m.rolle in :rollen
            """)
    List<Mitgliedschaft> findByUserIdAndRolleInMitOrg(@Param("userId") UUID userId,
                                                     @Param("rollen") Collection<Rolle> rollen);

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
     * <p>
     * Funktioniert auf Postgres und auf H2 (mit MODE=PostgreSQL).
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
