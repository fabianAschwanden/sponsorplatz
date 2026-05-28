package ch.sponsorplatz.anfrage;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

/**
 * Repository für Sponsoring-Anfragen.
 *
 * <p>Listen-Queries laden {@code paket}, {@code anfragenderOrg} und
 * {@code empfaengerOrg} per {@code LEFT JOIN FETCH} mit, weil
 * {@link AnfrageView#von(SponsoringAnfrage)} darauf zugreift und
 * {@code spring.jpa.open-in-view=false} sonst LazyInit-Fehler nach
 * Service-Tx-Ende auslöst.
 */
@Repository
public interface SponsoringAnfrageRepository extends JpaRepository<SponsoringAnfrage, UUID> {

    @Query("""
            select a from SponsoringAnfrage a
              left join fetch a.paket
              left join fetch a.anfragenderOrg
              left join fetch a.empfaengerOrg
             where a.empfaengerOrg.id = :empfaengerOrgId
             order by a.createdAt desc
            """)
    List<SponsoringAnfrage> findByEmpfaengerOrgIdOrderByCreatedAtDesc(@Param("empfaengerOrgId") UUID empfaengerOrgId);

    @Query("""
            select a from SponsoringAnfrage a
              left join fetch a.paket
              left join fetch a.anfragenderOrg
              left join fetch a.empfaengerOrg
             where a.anfragenderOrg.id = :anfragenderOrgId
             order by a.createdAt desc
            """)
    List<SponsoringAnfrage> findByAnfragenderOrgIdOrderByCreatedAtDesc(@Param("anfragenderOrgId") UUID anfragenderOrgId);

    @Query("""
            select a from SponsoringAnfrage a
              left join fetch a.paket
              left join fetch a.anfragenderOrg
              left join fetch a.empfaengerOrg
             where a.anfragenderOrg.id = :anfragenderOrgId
               and a.status = :status
             order by a.createdAt desc
            """)
    List<SponsoringAnfrage> findByAnfragenderOrgIdAndStatusOrderByCreatedAtDesc(
            @Param("anfragenderOrgId") UUID anfragenderOrgId,
            @Param("status") AnfrageStatus status);

    @Query("""
            select a from SponsoringAnfrage a
              left join fetch a.paket
              left join fetch a.anfragenderOrg
              left join fetch a.empfaengerOrg
             where a.empfaengerOrg.id = :empfaengerOrgId
               and a.status = :status
             order by a.createdAt desc
            """)
    List<SponsoringAnfrage> findByEmpfaengerOrgIdAndStatusOrderByCreatedAtDesc(
            @Param("empfaengerOrgId") UUID empfaengerOrgId,
            @Param("status") AnfrageStatus status);

    /**
     * Neueste Anfragen in einem Status quer über alle Sponsoren — für den
     * öffentlichen Engagement-Teaser auf der Startseite. Nur to-one-Fetches,
     * daher paginiert {@link Pageable} sauber in SQL (keine In-Memory-Paginierung).
     */
    @Query("""
            select a from SponsoringAnfrage a
              join fetch a.paket pk
              join fetch pk.projekt
              left join fetch a.anfragenderOrg
              left join fetch a.empfaengerOrg
             where a.status = :status
             order by a.createdAt desc
            """)
    List<SponsoringAnfrage> findNeuesteNachStatus(@Param("status") AnfrageStatus status, Pageable pageable);

    long countByEmpfaengerOrgIdAndStatus(UUID empfaengerOrgId, AnfrageStatus status);

    /** Alle Anfragen in einem bestimmten Status — für den Aufgaben-Backfill beim App-Start. */
    List<SponsoringAnfrage> findByStatus(AnfrageStatus status);

    /** Aggregat-Count für Dashboard: alle eingehenden Anfragen mehrerer Orgs in einer Query. */
    long countByEmpfaengerOrgIdIn(Collection<UUID> empfaengerOrgIds);

    /** Aggregat-Count für Dashboard: Anfragen mehrerer Orgs nach Status filtern. */
    long countByEmpfaengerOrgIdInAndStatus(Collection<UUID> empfaengerOrgIds, AnfrageStatus status);

    /**
     * Sponsor-zentrische Aggregat-Counter: Anfragen-Pipeline der Sponsor-Orgs.
     * Zählt alle Anfragen, die von der Sponsor-Seite ausgehen — d.h. klassische
     * Paket-Anfragen an Vereine, die der Sponsor gestellt hat.
     */
    long countByAnfragenderOrgIdInAndStatus(Collection<UUID> anfragenderOrgIds, AnfrageStatus status);

    /** Aggregat-Count Anfragen einer Sponsor-Org-Menge insgesamt (alle Statūs). */
    long countByAnfragenderOrgIdIn(Collection<UUID> anfragenderOrgIds);

    /** Alle eingehenden Anfragen über mehrere Orgs — für persönliche Anfragen-Übersicht. */
    @Query("""
            select a from SponsoringAnfrage a
              left join fetch a.paket
              left join fetch a.anfragenderOrg
              left join fetch a.empfaengerOrg
             where a.empfaengerOrg.id in :empfaengerOrgIds
             order by a.createdAt desc
            """)
    List<SponsoringAnfrage> findByEmpfaengerOrgIdInOrderByCreatedAtDesc(
            @Param("empfaengerOrgIds") Collection<UUID> empfaengerOrgIds);

    /** Alle ausgehenden Anfragen über mehrere Orgs — für die Verein→Sponsor-Sicht. */
    @Query("""
            select a from SponsoringAnfrage a
              left join fetch a.paket
              left join fetch a.anfragenderOrg
              left join fetch a.empfaengerOrg
             where a.anfragenderOrg.id in :anfragenderOrgIds
             order by a.createdAt desc
            """)
    List<SponsoringAnfrage> findByAnfragenderOrgIdInOrderByCreatedAtDesc(
            @Param("anfragenderOrgIds") Collection<UUID> anfragenderOrgIds);

    /**
     * Ausgehende Anfragen, die der angegebene User <b>selbst</b> erstellt hat
     * (egal aus welcher Org). Bucket „Meine ausgehende Anfragen".
     */
    @Query("""
            select a from SponsoringAnfrage a
              left join fetch a.paket
              left join fetch a.anfragenderOrg
              left join fetch a.empfaengerOrg
             where a.erstelltVon.id = :userId
             order by a.createdAt desc
            """)
    List<SponsoringAnfrage> findByErstelltVonIdOrderByCreatedAtDesc(
            @Param("userId") UUID userId);

    /**
     * Ausgehende Anfragen <em>meiner Organisationen</em>, die der angegebene
     * User <b>nicht selbst</b> erstellt hat — inklusive historischer Anfragen
     * (erstellt_von_id IS NULL, vor V32). Bucket „Ausgehende Anfragen zu
     * meiner Organisation".
     */
    @Query("""
            select a from SponsoringAnfrage a
              left join fetch a.paket
              left join fetch a.anfragenderOrg
              left join fetch a.empfaengerOrg
             where a.anfragenderOrg.id in :anfragenderOrgIds
               and (a.erstelltVon is null or a.erstelltVon.id <> :userId)
             order by a.createdAt desc
            """)
    List<SponsoringAnfrage> findOrgAusgehendNichtVonUser(
            @Param("anfragenderOrgIds") Collection<UUID> anfragenderOrgIds,
            @Param("userId") UUID userId);
}
