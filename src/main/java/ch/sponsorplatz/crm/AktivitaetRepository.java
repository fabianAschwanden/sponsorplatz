package ch.sponsorplatz.crm;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Repository für {@link Aktivitaet}. Timeline-Finder ist auf den Account gescoped
 * (Datum absteigend = neueste zuerst); der Zugriffs-Check liegt im
 * {@code AktivitaetService}, ARCH-01 verbietet Controller-Direktzugriff.
 */
@Repository
public interface AktivitaetRepository extends JpaRepository<Aktivitaet, UUID> {

    List<Aktivitaet> findByAccountIdOrderByDatumDescErstelltAmDesc(UUID accountId);
}
