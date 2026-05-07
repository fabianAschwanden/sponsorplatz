package ch.sponsorplatz.repository;

import ch.sponsorplatz.model.Rechnung;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RechnungRepository extends JpaRepository<Rechnung, UUID> {

    Optional<Rechnung> findByVertragId(UUID vertragId);

    List<Rechnung> findByOrgIdOrderByErstelltAmDesc(UUID orgId);

    long countByOrgId(UUID orgId);
}
