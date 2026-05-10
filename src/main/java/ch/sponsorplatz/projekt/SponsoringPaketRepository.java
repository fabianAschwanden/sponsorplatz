package ch.sponsorplatz.projekt;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SponsoringPaketRepository extends JpaRepository<SponsoringPaket, UUID> {

    List<SponsoringPaket> findByProjektIdOrderBySortierungAsc(UUID projektId);

    List<SponsoringPaket> findByProjektIdAndAktivTrueOrderBySortierungAsc(UUID projektId);

    /**
     * Lädt Paket inkl. Projekt und Empfänger-Org in einer Query — wird vom
     * Anfrage-Erstellungs-Flow verwendet, damit die Form die Empfänger-Org
     * benennen kann, ohne LazyInit nach Service-Tx-Ende auszulösen.
     */
    @Query("""
            select p from SponsoringPaket p
              join fetch p.projekt pr
              join fetch pr.org
             where p.id = :id
            """)
    Optional<SponsoringPaket> findByIdMitProjektUndOrg(@Param("id") UUID id);
}

