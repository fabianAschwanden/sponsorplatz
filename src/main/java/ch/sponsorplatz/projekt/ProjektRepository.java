package ch.sponsorplatz.projekt;

import ch.sponsorplatz.organisation.Branche;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProjektRepository extends JpaRepository<Projekt, UUID> {

    Optional<Projekt> findBySlug(String slug);

    List<Projekt> findByOrgIdOrderByCreatedAtDesc(UUID orgId);

    List<Projekt> findBySichtbarkeitOrderByVeroeffentlichtAmDesc(Sichtbarkeit sichtbarkeit);

    boolean existsBySlug(String slug);

    long countByOrgIdInAndSichtbarkeit(Collection<UUID> orgIds, Sichtbarkeit sichtbarkeit);

    /**
     * Zählt direkt auf DB-Ebene — Marken-Landing braucht nur die Anzahl, nicht
     * die Liste. Spart das Laden + Mappen aller Projekte für eine Statistik.
     */
    long countBySichtbarkeit(Sichtbarkeit sichtbarkeit);

    /**
     * Volltextsuche: durchsucht Name, Beschreibung, Kategorie, Ort und Org-Name.
     * Case-insensitive LIKE-Suche, funktioniert auf H2 und PostgreSQL.
     */
    @Query("""
            SELECT p FROM Projekt p
            WHERE p.sichtbarkeit = :sichtbarkeit
            AND (LOWER(p.name) LIKE LOWER(CONCAT('%', :suchbegriff, '%'))
                 OR LOWER(p.beschreibung) LIKE LOWER(CONCAT('%', :suchbegriff, '%'))
                 OR LOWER(p.kategorie) LIKE LOWER(CONCAT('%', :suchbegriff, '%'))
                 OR LOWER(p.ort) LIKE LOWER(CONCAT('%', :suchbegriff, '%'))
                 OR LOWER(p.org.name) LIKE LOWER(CONCAT('%', :suchbegriff, '%')))
            ORDER BY p.veroeffentlichtAm DESC
            """)
    List<Projekt> sucheOeffentliche(@Param("suchbegriff") String suchbegriff,
                                     @Param("sichtbarkeit") Sichtbarkeit sichtbarkeit);

    /**
     * Matching-Query: Findet öffentliche Projekte, deren Org eine der gegebenen Branchen hat,
     * aber nicht zu den eigenen Orgs gehört. Limitiert auf die neuesten Einträge.
     */
    @Query("""
            SELECT p FROM Projekt p
            WHERE p.sichtbarkeit = :sichtbarkeit
            AND p.org.branche IN :branchen
            AND p.org.id NOT IN :eigeneOrgIds
            ORDER BY p.veroeffentlichtAm DESC
            """)
    List<Projekt> findePassende(@Param("branchen") Collection<Branche> branchen,
                                @Param("eigeneOrgIds") Collection<UUID> eigeneOrgIds,
                                @Param("sichtbarkeit") Sichtbarkeit sichtbarkeit);
}
