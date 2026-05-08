package ch.sponsorplatz.projekt;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface WatchlistRepository extends JpaRepository<WatchlistEintrag, UUID> {

    List<WatchlistEintrag> findByUserIdOrderByCreatedAtDesc(UUID userId);

    Optional<WatchlistEintrag> findByUserIdAndProjektId(UUID userId, UUID projektId);

    boolean existsByUserIdAndProjektId(UUID userId, UUID projektId);

    void deleteByUserIdAndProjektId(UUID userId, UUID projektId);
}

