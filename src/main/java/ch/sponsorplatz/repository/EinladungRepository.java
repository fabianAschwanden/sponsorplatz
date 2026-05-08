package ch.sponsorplatz.repository;
import ch.sponsorplatz.service.EinladungsCleanupJob;

import ch.sponsorplatz.model.Einladung;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface EinladungRepository extends JpaRepository<Einladung, UUID> {

    Optional<Einladung> findByToken(String token);

    List<Einladung> findByOrgId(UUID orgId);

    boolean existsByOrgIdAndEmail(UUID orgId, String email);

    Optional<Einladung> findByOrgIdAndEmail(UUID orgId, String email);

    void deleteByToken(String token);

    /**
     * Löscht alle Einladungen, deren Gültigkeit vor dem Cutoff liegt.
     * Wird vom {@code EinladungsCleanupJob} verwendet (M2-Fix: DB-Hygiene für abgelaufene Tokens).
     *
     * @return Anzahl gelöschter Einladungen
     */
    @Modifying
    @Query("delete from Einladung e where e.gueltigBis < :cutoff")
    int deleteByGueltigBisBefore(@Param("cutoff") Instant cutoff);
}

