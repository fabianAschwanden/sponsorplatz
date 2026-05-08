package ch.sponsorplatz.anfrage;

import ch.sponsorplatz.shared.exception.NotFoundException;
import ch.sponsorplatz.benutzer.AppUser;
import ch.sponsorplatz.benutzer.AppUserRepository;
import ch.sponsorplatz.organisation.MitgliedschaftRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Service für Nachrichten (Inbox-Threads) an angenommene Sponsoring-Anfragen.
 */
@Service
@Transactional
public class NachrichtService {

    private final NachrichtRepository nachrichtRepository;
    private final SponsoringAnfrageRepository anfrageRepository;
    private final AppUserRepository appUserRepository;
    private final MitgliedschaftRepository mitgliedschaftRepository;

    public NachrichtService(NachrichtRepository nachrichtRepository,
                            SponsoringAnfrageRepository anfrageRepository,
                            AppUserRepository appUserRepository,
                            MitgliedschaftRepository mitgliedschaftRepository) {
        this.nachrichtRepository = nachrichtRepository;
        this.anfrageRepository = anfrageRepository;
        this.appUserRepository = appUserRepository;
        this.mitgliedschaftRepository = mitgliedschaftRepository;
    }

    /**
     * Sendet eine Nachricht zu einer angenommenen Anfrage.
     *
     * @throws NotFoundException wenn Anfrage nicht existiert
     * @throws IllegalStateException wenn Anfrage nicht ANGENOMMEN
     * @throws AccessDeniedException wenn User nicht zur anfragenden oder empfangenden Org gehört
     * @throws IllegalArgumentException wenn Text leer
     */
    public Nachricht sendeNachricht(UUID anfrageId, UUID absenderId, String text) {
        if (text == null || text.isBlank()) {
            throw new IllegalArgumentException("Nachricht darf nicht leer sein");
        }

        SponsoringAnfrage anfrage = anfrageRepository.findById(anfrageId)
                .orElseThrow(() -> new NotFoundException("Anfrage nicht gefunden: " + anfrageId));

        if (anfrage.getStatus() != AnfrageStatus.ANGENOMMEN) {
            throw new IllegalStateException(
                    "Nachrichten können nur zu angenommenen Anfragen gesendet werden (Status: " + anfrage.getStatus() + ")");
        }

        AppUser absender = appUserRepository.findById(absenderId)
                .orElseThrow(() -> new NotFoundException("Benutzer nicht gefunden: " + absenderId));

        // Prüfe ob User Mitglied der anfragenden ODER empfangenden Org ist
        boolean istBeteiligt = mitgliedschaftRepository.existsByUserIdAndOrgId(absenderId, anfrage.getAnfragenderOrg().getId())
                || mitgliedschaftRepository.existsByUserIdAndOrgId(absenderId, anfrage.getEmpfaengerOrg().getId());

        if (!istBeteiligt) {
            throw new AccessDeniedException("Benutzer ist nicht an dieser Anfrage beteiligt");
        }

        Nachricht nachricht = new Nachricht();
        nachricht.setAnfrage(anfrage);
        nachricht.setAbsender(absender);
        nachricht.setText(text.trim());

        return nachrichtRepository.save(nachricht);
    }

    /**
     * Gibt alle Nachrichten einer Anfrage chronologisch sortiert zurück.
     */
    @Transactional(readOnly = true)
    public List<Nachricht> findeNachAnfrage(UUID anfrageId) {
        return nachrichtRepository.findByAnfrageIdOrderByCreatedAtAsc(anfrageId);
    }
}

