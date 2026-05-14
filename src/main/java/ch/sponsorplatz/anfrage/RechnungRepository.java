package ch.sponsorplatz.anfrage;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RechnungRepository extends JpaRepository<Rechnung, UUID> {

    Optional<Rechnung> findByVertragId(UUID vertragId);

    List<Rechnung> findByOrgIdOrderByErstelltAmDesc(UUID orgId);

    long countByOrgId(UUID orgId);

    /**
     * Höchste laufende Nummer (NNNNN aus „R-YYYY-NNNNN") für eine Org im Jahr.
     * Lückenlose Nummerierung: zählt auch stornierte Rechnungen mit, damit die
     * nächste Nummer nicht versehentlich Lücken überspringt (OR Art. 957 ff.).
     *
     * <p>Substring-Position 9 = nach „R-2026-" (8 Zeichen-Präfix incl. Bindestrich).
     * Liefert {@code null}, wenn die Org im Jahr noch keine Rechnung hat.
     */
    @Query("""
            select max(cast(substring(r.rechnungsnummer, 9) as int))
              from Rechnung r
             where r.org.id = :orgId
               and r.rechnungsnummer like :praefix
            """)
    Integer findeMaxLfdNr(@Param("orgId") UUID orgId, @Param("praefix") String praefix);

    /**
     * Sponsor-zentrischer Rechnungs-Counter pro Status. Rechnungen sind an
     * {@code vertrag.sponsorOrg} gehängt — der Join läuft also über den
     * Vertrag. Für das Sponsor-Statistik-Dashboard (Liquiditäts-Sicht).
     */
    @Query("""
            select count(r)
              from Rechnung r
             where r.vertrag.sponsorOrg.id in :sponsorOrgIds
               and r.status = :status
            """)
    long zaehleProSponsorOrgUndStatus(@Param("sponsorOrgIds") Collection<UUID> sponsorOrgIds,
                                       @Param("status") RechnungsStatus status);

    /**
     * Verein-zentrischer Rechnungs-Counter pro Status (für Vereins-Statistik).
     * Rechnungen sind direkt an {@code rechnung.org} (= Verein) gehängt —
     * kein Join nötig.
     */
    long countByOrgIdInAndStatus(Collection<UUID> orgIds, RechnungsStatus status);
}
