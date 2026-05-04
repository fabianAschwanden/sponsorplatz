package ch.sponsorplatz.repository;

import ch.sponsorplatz.model.SponsoringPaket;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface SponsoringPaketRepository extends JpaRepository<SponsoringPaket, UUID> {

    List<SponsoringPaket> findByProjektIdOrderBySortierungAsc(UUID projektId);

    List<SponsoringPaket> findByProjektIdAndAktivTrueOrderBySortierungAsc(UUID projektId);
}

