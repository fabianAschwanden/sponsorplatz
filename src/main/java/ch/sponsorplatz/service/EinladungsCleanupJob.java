package ch.sponsorplatz.service;

import ch.sponsorplatz.repository.EinladungRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

/**
 * Scheduled-Job: löscht abgelaufene Einladungen aus der DB (M2-Fix).
 * <p>
 * Läuft täglich um 03:00 Uhr. Die {@code (org_id, email)}-Unique-Constraint blockt
 * ohne Cleanup auch dann eine Re-Einladung, wenn die alte Einladung längst verfallen
 * ist. Plus DB-Hygiene: Tokens sammeln sich sonst ungenutzt an.
 */
@Component
public class EinladungsCleanupJob {

    private static final Logger log = LoggerFactory.getLogger(EinladungsCleanupJob.class);

    private final EinladungRepository einladungRepository;

    public EinladungsCleanupJob(EinladungRepository einladungRepository) {
        this.einladungRepository = einladungRepository;
    }

    @Scheduled(cron = "0 0 3 * * *")
    @Transactional
    public void loescheAbgelaufene() {
        int geloescht = einladungRepository.deleteByGueltigBisBefore(Instant.now());
        if (geloescht > 0) {
            log.info("Cleanup: {} abgelaufene Einladungen gelöscht", geloescht);
        }
    }
}
