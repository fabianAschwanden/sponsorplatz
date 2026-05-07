package ch.sponsorplatz.organisation;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OrganisationRepository extends JpaRepository<Organisation, UUID> {

    Optional<Organisation> findBySlug(String slug);

    boolean existsBySlug(String slug);

    List<Organisation> findAllByOrderByNameAsc();

    List<Organisation> findByStatusOrderByCreatedAtAsc(OrgStatus status);
}
