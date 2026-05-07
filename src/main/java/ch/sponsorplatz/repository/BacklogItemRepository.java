package ch.sponsorplatz.repository;

import ch.sponsorplatz.model.BacklogItem;
import ch.sponsorplatz.model.BacklogStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface BacklogItemRepository extends JpaRepository<BacklogItem, UUID> {

    long countByStatus(BacklogStatus status);
}
