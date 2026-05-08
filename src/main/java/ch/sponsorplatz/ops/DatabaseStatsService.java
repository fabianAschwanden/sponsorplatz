package ch.sponsorplatz.ops;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.OptionalLong;

/**
 * Aggregierte DB-Statistiken für das Ops-Dashboard.
 *
 * <p>Postgres: {@code SELECT pg_database_size(current_database())} —
 * physische Grösse inkl. Indices/WAL.<br>
 * H2 (file): Datei-Grösse der {@code .mv.db}-Datei aus der JDBC-URL.<br>
 * H2 (in-memory, Tests): {@link OptionalLong#empty()}.
 */
@Service
public class DatabaseStatsService {

    private static final Logger log = LoggerFactory.getLogger(DatabaseStatsService.class);

    private final DataSource dataSource;

    public DatabaseStatsService(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public OptionalLong groesseInBytes() {
        try (var conn = dataSource.getConnection()) {
            String produkt = conn.getMetaData().getDatabaseProductName();
            if ("PostgreSQL".equalsIgnoreCase(produkt)) {
                try (var stmt = conn.createStatement();
                     var rs = stmt.executeQuery(
                             "SELECT pg_database_size(current_database())")) {
                    if (rs.next()) return OptionalLong.of(rs.getLong(1));
                }
            } else if ("H2".equalsIgnoreCase(produkt)) {
                return h2DateiGroesse(conn.getMetaData().getURL());
            }
        } catch (SQLException e) {
            log.warn("DB-Grösse konnte nicht ermittelt werden: {}", e.getMessage());
        }
        return OptionalLong.empty();
    }

    private OptionalLong h2DateiGroesse(String jdbcUrl) {
        // jdbc:h2:file:./data/sponsorplatz → ./data/sponsorplatz.mv.db
        // in-memory: jdbc:h2:mem:... → kein File
        if (jdbcUrl == null || !jdbcUrl.startsWith("jdbc:h2:file:")) {
            return OptionalLong.empty();
        }
        String dateiBasis = jdbcUrl.substring("jdbc:h2:file:".length());
        // Optionen abschneiden: jdbc:h2:file:./data/sponsorplatz;MODE=PostgreSQL → ./data/sponsorplatz
        int semi = dateiBasis.indexOf(';');
        if (semi > 0) dateiBasis = dateiBasis.substring(0, semi);
        Path mvDb = Path.of(dateiBasis + ".mv.db");
        try {
            if (Files.exists(mvDb)) {
                return OptionalLong.of(Files.size(mvDb));
            }
        } catch (IOException e) {
            log.warn("H2-Datei {} konnte nicht gelesen werden: {}", mvDb, e.getMessage());
        }
        return OptionalLong.empty();
    }
}
