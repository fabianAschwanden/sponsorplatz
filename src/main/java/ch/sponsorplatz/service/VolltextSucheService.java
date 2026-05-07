package ch.sponsorplatz.service;

import ch.sponsorplatz.model.Projekt;
import ch.sponsorplatz.model.Sichtbarkeit;
import ch.sponsorplatz.repository.ProjektRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Volltextsuche über Projekte. Wählt zur Laufzeit den passenden Pfad:
 *
 * <ul>
 *   <li><b>PostgreSQL:</b> native Query mit {@code tsvector @@ plainto_tsquery('german', ?)}
 *       + GIN-Index (siehe Migration V18 unter {@code db/migration/postgres}).
 *       Ranking via {@code ts_rank_cd} — relevantere Treffer zuerst.</li>
 *   <li><b>H2 (dev/test):</b> Fallback auf JPQL-LIKE-Suche im
 *       {@link ProjektRepository#sucheOeffentliche}. tsvector ist in H2 nicht
 *       verfügbar; die Migration V18 liegt unter {@code db/migration/postgres}
 *       und wird in dev nicht geladen.</li>
 * </ul>
 *
 * <p>Org-Name wird in beiden Pfaden zusätzlich per LIKE durchsucht — er ist
 * nicht im tsvector-Index (cross-table-reference geht nicht in GENERATED-Columns).
 */
@Service
public class VolltextSucheService {

    private static final Logger log = LoggerFactory.getLogger(VolltextSucheService.class);

    private final JdbcTemplate jdbc;
    private final ProjektRepository projektRepository;

    @Value("${spring.datasource.url:}")
    private String datasourceUrl;

    public VolltextSucheService(JdbcTemplate jdbc, ProjektRepository projektRepository) {
        this.jdbc = jdbc;
        this.projektRepository = projektRepository;
    }

    @Transactional(readOnly = true)
    public List<Projekt> suchen(String suchbegriff) {
        if (suchbegriff == null || suchbegriff.isBlank()) {
            return projektRepository.findBySichtbarkeitOrderByVeroeffentlichtAmDesc(Sichtbarkeit.OEFFENTLICH);
        }
        String trimmed = suchbegriff.trim();
        if (istPostgres()) {
            return suchePostgresVolltext(trimmed);
        }
        return projektRepository.sucheOeffentliche(trimmed, Sichtbarkeit.OEFFENTLICH);
    }

    private List<Projekt> suchePostgresVolltext(String suchbegriff) {
        // tsvector-Match auf projekt (Name/Beschreibung/Kategorie/Ort) ODER
        // LIKE auf Org-Name. Ranking nach ts_rank_cd, dann veroeffentlicht_am.
        String sql = """
                SELECT p.id
                FROM projekt p
                LEFT JOIN organisation o ON o.id = p.org_id
                WHERE p.sichtbarkeit = ?
                  AND (p.tsvektor @@ plainto_tsquery('german', ?)
                       OR LOWER(o.name) LIKE LOWER('%' || ? || '%'))
                ORDER BY ts_rank_cd(p.tsvektor, plainto_tsquery('german', ?)) DESC,
                         p.veroeffentlicht_am DESC NULLS LAST
                LIMIT 200
                """;
        try {
            List<UUID> ids = jdbc.queryForList(sql, UUID.class,
                    Sichtbarkeit.OEFFENTLICH.name(),
                    suchbegriff,
                    suchbegriff,
                    suchbegriff);
            if (ids.isEmpty()) return List.of();

            // findAllById liefert nicht in der gleichen Reihenfolge — nachsortieren.
            var idIndex = new java.util.HashMap<UUID, Integer>();
            for (int i = 0; i < ids.size(); i++) idIndex.put(ids.get(i), i);
            return projektRepository.findAllById(ids).stream()
                    .sorted(java.util.Comparator.comparingInt(p -> idIndex.getOrDefault(p.getId(), Integer.MAX_VALUE)))
                    .collect(Collectors.toList());
        } catch (RuntimeException e) {
            log.warn("Postgres-Volltextsuche fehlgeschlagen — Fallback auf LIKE: {}", e.getMessage());
            return projektRepository.sucheOeffentliche(suchbegriff, Sichtbarkeit.OEFFENTLICH);
        }
    }

    private boolean istPostgres() {
        return datasourceUrl != null && datasourceUrl.toLowerCase().contains("postgresql");
    }
}
