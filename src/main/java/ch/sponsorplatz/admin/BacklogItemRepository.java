package ch.sponsorplatz.admin;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface BacklogItemRepository extends JpaRepository<BacklogItem, UUID> {

    long countByStatus(BacklogStatus status);
}
