package ch.sponsorplatz.service;

import ch.sponsorplatz.model.AppUser;
import ch.sponsorplatz.model.Benachrichtigung;
import ch.sponsorplatz.model.BenachrichtigungTyp;
import ch.sponsorplatz.repository.AppUserRepository;
import ch.sponsorplatz.repository.BenachrichtigungRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Service für In-App-Benachrichtigungen (Glocke).
 */
@Service
@Transactional
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    private final BenachrichtigungRepository repository;
    private final AppUserRepository appUserRepository;

    public NotificationService(BenachrichtigungRepository repository,
                               AppUserRepository appUserRepository) {
        this.repository = repository;
        this.appUserRepository = appUserRepository;
    }

    /**
     * Erstellt eine Benachrichtigung für einen User (async).
     */
    @Async
    public void benachrichtige(UUID empfaengerId, BenachrichtigungTyp typ,
                                String titel, String text, String link) {
        AppUser empfaenger = appUserRepository.findById(empfaengerId).orElse(null);
        if (empfaenger == null) {
            log.warn("Notification: Empfänger {} nicht gefunden", empfaengerId);
            return;
        }

        Benachrichtigung b = new Benachrichtigung();
        b.setEmpfaenger(empfaenger);
        b.setTyp(typ);
        b.setTitel(titel);
        b.setText(text);
        b.setLink(link);
        repository.save(b);

        log.debug("Notification erstellt: {} → {} ({})", typ, empfaenger.getEmail(), titel);
    }

    /**
     * Gibt die letzten 20 Benachrichtigungen eines Users zurück.
     */
    @Transactional(readOnly = true)
    public List<Benachrichtigung> letzteNachrichtenFuer(UUID empfaengerId) {
        return repository.findTop20ByEmpfaengerIdOrderByCreatedAtDesc(empfaengerId);
    }

    /**
     * Zählt ungelesene Benachrichtigungen (für Badge).
     */
    @Transactional(readOnly = true)
    public long zaehleUngelesen(UUID empfaengerId) {
        return repository.countByEmpfaengerIdAndGelesenFalse(empfaengerId);
    }

    /**
     * Markiert alle Benachrichtigungen eines Users als gelesen.
     */
    public void markiereAlleAlsGelesen(UUID empfaengerId) {
        repository.markiereAlleAlsGelesen(empfaengerId);
    }

    /**
     * Markiert eine einzelne Benachrichtigung als gelesen — nur wenn der
     * angegebene User auch der Empfänger ist (verhindert IDOR: fremde
     * Notifications via beliebiger UUID auf "gelesen" setzen).
     *
     * @throws AccessDeniedException wenn der User nicht Empfänger ist
     */
    public void markiereAlsGelesen(UUID benachrichtigungId, UUID anfragenderUserId) {
        repository.findById(benachrichtigungId).ifPresent(b -> {
            if (!b.getEmpfaenger().getId().equals(anfragenderUserId)) {
                throw new AccessDeniedException(
                        "Notification gehört nicht zum eingeloggten User");
            }
            b.setGelesen(true);
            repository.save(b);
        });
    }
}
