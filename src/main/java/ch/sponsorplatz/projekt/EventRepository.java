package ch.sponsorplatz.projekt;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface EventRepository extends JpaRepository<Event, UUID> {

    Optional<Event> findBySlug(String slug);

    List<Event> findByOrgIdOrderByDatumAsc(UUID orgId);

    List<Event> findByOrgIdInAndDatumGreaterThanEqualOrderByDatumAsc(
            Collection<UUID> orgIds, LocalDate ab);

    long countByOrgId(UUID orgId);
}

