package ch.sponsorplatz.anfrage;

import ch.sponsorplatz.benachrichtigung.BenachrichtigungTyp;
import ch.sponsorplatz.benachrichtigung.NotificationService;
import ch.sponsorplatz.shared.exception.NotFoundException;
import ch.sponsorplatz.benutzer.AppUser;
import ch.sponsorplatz.benutzer.AppUserRepository;
import ch.sponsorplatz.organisation.Mitgliedschaft;
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
    private final NotificationService notificationService;

    public NachrichtService(NachrichtRepository nachrichtRepository,
                            SponsoringAnfrageRepository anfrageRepository,
                            AppUserRepository appUserRepository,
                            MitgliedschaftRepository mitgliedschaftRepository,
                            NotificationService notificationService) {
        this.nachrichtRepository = nachrichtRepository;
        this.anfrageRepository = anfrageRepository;
        this.appUserRepository = appUserRepository;
        this.mitgliedschaftRepository = mitgliedschaftRepository;
        this.notificationService = notificationService;
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

        Nachricht gespeichert = nachrichtRepository.save(nachricht);

        // Benachrichtige alle Beteiligten der "anderen" Org — also der Org,
        // zu der der Absender NICHT gehört. NotificationService.benachrichtige
        // läuft @Async, blockiert die Tx also nicht. Die Empfänger-Org ergibt
        // sich aus der Mitgliedschaft des Absenders.
        boolean absenderInAnfragender = mitgliedschaftRepository
                .existsByUserIdAndOrgId(absenderId, anfrage.getAnfragenderOrg().getId());
        UUID empfaengerOrgId = absenderInAnfragender
                ? anfrage.getEmpfaengerOrg().getId()
                : anfrage.getAnfragenderOrg().getId();
        String link = "/organisationen/" + anfrage.getEmpfaengerOrg().getSlug()
                + "/anfragen/" + anfrage.getId() + "/nachrichten";
        String titel = "Neue Nachricht von " + absender.getAnzeigename();
        String vorschau = kuerzeText(nachricht.getText(), 140);
        for (Mitgliedschaft m : mitgliedschaftRepository.findByOrgId(empfaengerOrgId)) {
            UUID empfaengerId = m.getUser().getId();
            if (empfaengerId.equals(absenderId)) {
                continue; // kein Self-Notify (eingeladene Owner können in beiden Orgs sein)
            }
            notificationService.benachrichtige(empfaengerId,
                    BenachrichtigungTyp.NEUE_NACHRICHT, titel, vorschau, link);
        }

        return gespeichert;
    }

    private static String kuerzeText(String s, int max) {
        if (s == null) return "";
        return s.length() <= max ? s : s.substring(0, max - 1) + "…";
    }

    /**
     * Gibt alle Nachrichten einer Anfrage chronologisch sortiert zurück.
     */
    @Transactional(readOnly = true)
    public List<Nachricht> findeNachAnfrage(UUID anfrageId) {
        return nachrichtRepository.findByAnfrageIdOrderByCreatedAtAsc(anfrageId);
    }
}

