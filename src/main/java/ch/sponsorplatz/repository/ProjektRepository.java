package ch.sponsorplatz.repository;

import ch.sponsorplatz.model.Projekt;
import ch.sponsorplatz.model.Sichtbarkeit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProjektRepository extends JpaRepository<Projekt, UUID> {

    Optional<Projekt> findBySlug(String slug);

    List<Projekt> findByOrgIdOrderByCreatedAtDesc(UUID orgId);

    List<Projekt> findBySichtbarkeitOrderByVeroeffentlichtAmDesc(Sichtbarkeit sichtbarkeit);

    boolean existsBySlug(String slug);
}

