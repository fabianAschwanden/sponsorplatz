package ch.sponsorplatz.anfrage;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface VertragRepository extends JpaRepository<Vertrag, UUID> {

    Optional<Vertrag> findByAnfrageId(UUID anfrageId);

    List<Vertrag> findByOrgIdOrderByErstelltAmDesc(UUID orgId);

    List<Vertrag> findByOrgIdAndStatusOrderByErstelltAmDesc(UUID orgId, VertragsStatus status);

    // -------- Sponsor-zentrische Aggregat-Queries (Phase 5.C — Statistiken) --------

    /** Anzahl Verträge in einem Status für eine Menge Sponsor-Orgs. */
    long countBySponsorOrgIdInAndStatus(Collection<UUID> sponsorOrgIds, VertragsStatus status);

    /** Anzahl Verträge in einem Status für eine Menge Verein-Orgs (Vereins-Statistik). */
    long countByOrgIdInAndStatus(Collection<UUID> orgIds, VertragsStatus status);

    /**
     * Summe der Vertragspreise in einem Status — für „Einnahmen-Volumen" auf
     * der Vereins-Statistik. Liefert {@code null}, wenn keine Verträge
     * existieren; Service mappt auf {@link BigDecimal#ZERO}.
     */
    @Query("""
            select coalesce(sum(v.preisChf), 0)
              from Vertrag v
             where v.org.id in :orgIds
               and v.status = :status
            """)
    BigDecimal summePreisChfByOrg(@Param("orgIds") Collection<UUID> orgIds,
                                   @Param("status") VertragsStatus status);

    /** Anzahl Verträge insgesamt für eine Menge Sponsor-Orgs (alle Statūs). */
    long countBySponsorOrgIdIn(Collection<UUID> sponsorOrgIds);

    /**
     * Summe der Vertragspreise in einem Status — für „Gesamt-Sponsoring-
     * Volumen" auf der Sponsor-Statistik. Liefert {@code null}, wenn keine
     * Verträge existieren; Service mappt das auf {@link BigDecimal#ZERO}.
     */
    @Query("""
            select coalesce(sum(v.preisChf), 0)
              from Vertrag v
             where v.sponsorOrg.id in :sponsorOrgIds
               and v.status = :status
            """)
    BigDecimal summePreisChf(@Param("sponsorOrgIds") Collection<UUID> sponsorOrgIds,
                              @Param("status") VertragsStatus status);

    /**
     * Branchen-Verteilung: pro Branche des unterstützten Vereins die Anzahl
     * Verträge eines Sponsors. Liefert Tupel (Branche, count). Iteriert auf
     * der DB-Seite per `group by`, nicht im Service — bei vielen Verträgen
     * schneller als Stream-Aggregation.
     */
    @Query("""
            select v.org.branche, count(v)
              from Vertrag v
             where v.sponsorOrg.id in :sponsorOrgIds
               and v.status = :status
               and v.org.branche is not null
             group by v.org.branche
             order by count(v) desc
            """)
    List<Object[]> zaehleProBranche(@Param("sponsorOrgIds") Collection<UUID> sponsorOrgIds,
                                     @Param("status") VertragsStatus status);
}
