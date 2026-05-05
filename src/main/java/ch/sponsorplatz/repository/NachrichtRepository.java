package ch.sponsorplatz.repository;

import ch.sponsorplatz.model.Nachricht;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

/**
 * Repository für Nachrichten (Inbox-Threads).
 */
public interface NachrichtRepository extends JpaRepository<Nachricht, UUID> {

    List<Nachricht> findByAnfrageIdOrderByCreatedAtAsc(UUID anfrageId);

    long countByAnfrageId(UUID anfrageId);
}

