package ch.sponsorplatz.repository;

import ch.sponsorplatz.model.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, UUID> {

    List<AuditLog> findTop100ByOrderByZeitpunktDesc();

    List<AuditLog> findByBereichOrderByZeitpunktDesc(String bereich);

    List<AuditLog> findByBenutzerIdOrderByZeitpunktDesc(UUID benutzerId);

    List<AuditLog> findByZeitpunktBetweenOrderByZeitpunktDesc(Instant von, Instant bis);

    long countByBereich(String bereich);
}

