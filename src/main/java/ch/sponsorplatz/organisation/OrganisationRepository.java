package ch.sponsorplatz.organisation;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OrganisationRepository extends JpaRepository<Organisation, UUID> {

    /**
     * LEFT JOIN FETCH uebergeordneteOrg — verhindert
     * {@code LazyInitializationException} im Template, weil
     * {@code OrganisationView.von} auf {@code eltern.getName()} und
     * {@code eltern.getSlug()} zugreift, nachdem die Service-Transaktion
     * bereits geschlossen ist (open-in-view=false). LEFT damit Root-Orgs
     * (kein Parent) nicht rausgefiltert werden.
     */
    @Query("SELECT o FROM Organisation o LEFT JOIN FETCH o.uebergeordneteOrg WHERE o.slug = :slug")
    Optional<Organisation> findBySlug(@Param("slug") String slug);

    boolean existsBySlug(String slug);

    /**
     * Liste aller Orgs (für /organisationen + Hierarchie-Edit-Dropdown).
     * JOIN FETCH wegen {@code OrganisationView.von} → siehe findBySlug.
     */
    @Query("SELECT o FROM Organisation o LEFT JOIN FETCH o.uebergeordneteOrg ORDER BY o.name ASC")
    List<Organisation> findAllByOrderByNameAsc();

    List<Organisation> findByStatusOrderByCreatedAtAsc(OrgStatus status);

    /**
     * JOIN FETCH uebergeordneteOrg — verhindert {@code LazyInitializationException}
     * im Template, weil {@code OrganisationView.von} auf {@code eltern.getName()}
     * zugreift, nachdem die Service-Transaktion schon zu ist (open-in-view=false).
     * Diese Query lädt typischerweise die direkten Sub-Orgs einer Eltern-Org —
     * deren Eltern-Referenz IST die ganze Zeit dieselbe (= die anfragende Org),
     * aber Hibernate hängt trotzdem einen LAZY-Proxy pro Row dran.
     */
    @Query("SELECT o FROM Organisation o LEFT JOIN FETCH o.uebergeordneteOrg "
            + "WHERE o.uebergeordneteOrg.id = :uebergeordneteOrgId ORDER BY o.name ASC")
    List<Organisation> findByUebergeordneteOrgIdOrderByNameAsc(
            @Param("uebergeordneteOrgId") UUID uebergeordneteOrgId);

    boolean existsByUebergeordneteOrgId(UUID uebergeordneteOrgId);

    /**
     * Aggregat-Query für die Marken-Landing-Page-Statistik: zählt Vereine
     * (TYP = VEREIN) gruppiert nach Branche, gefiltert auf die übergebenen
     * Stati. Macht aus dem alten "alle laden + in Java gruppieren" einen
     * einzigen DB-Roundtrip und skaliert auch für tausende Vereine.
     *
     * <p>Rückgabe als {@code Object[]}: [0] = {@link Branche}, [1] = {@link Long}.
     */
    @Query("SELECT o.branche, COUNT(o) FROM Organisation o " +
           "WHERE o.typ = ch.sponsorplatz.organisation.OrgTyp.VEREIN " +
           "  AND o.status IN :stati " +
           "  AND o.branche IS NOT NULL " +
           "GROUP BY o.branche")
    List<Object[]> zaehleVereineNachBranche(@Param("stati") Collection<OrgStatus> stati);
}
