package ch.sponsorplatz.anfrage;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface VertragRepository extends JpaRepository<Vertrag, UUID> {

    Optional<Vertrag> findByAnfrageId(UUID anfrageId);

    List<Vertrag> findByOrgIdOrderByErstelltAmDesc(UUID orgId);

    List<Vertrag> findByOrgIdAndStatusOrderByErstelltAmDesc(UUID orgId, VertragsStatus status);
}
