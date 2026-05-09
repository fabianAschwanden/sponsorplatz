package ch.sponsorplatz.anfrage;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ch.sponsorplatz.benachrichtigung.BenachrichtigungTyp;
import ch.sponsorplatz.benachrichtigung.NotificationService;
import ch.sponsorplatz.organisation.MitgliedschaftRepository;
import ch.sponsorplatz.organisation.Organisation;
import ch.sponsorplatz.projekt.SponsoringPaket;

@Service
@Transactional
public class SponsoringAnfrageService {

    private static final Set<AnfrageStatus> BEANTWORTET = Set.of(
            AnfrageStatus.ANGENOMMEN, AnfrageStatus.ABGELEHNT);

    private final SponsoringAnfrageRepository repository;
    private final BenachrichtigungsService benachrichtigungsService;
    private final NotificationService notificationService;
    private final MitgliedschaftRepository mitgliedschaftRepository;

    public SponsoringAnfrageService(SponsoringAnfrageRepository repository,
            BenachrichtigungsService benachrichtigungsService,
            NotificationService notificationService,
            MitgliedschaftRepository mitgliedschaftRepository) {
        this.repository = repository;
        this.benachrichtigungsService = benachrichtigungsService;
        this.notificationService = notificationService;
        this.mitgliedschaftRepository = mitgliedschaftRepository;
    }

    @Transactional(readOnly = true)
    public List<SponsoringAnfrage> findeEingehende(UUID empfaengerOrgId) {
        return repository.findByEmpfaengerOrgIdOrderByCreatedAtDesc(empfaengerOrgId);
    }

    @Transactional(readOnly = true)
    public List<SponsoringAnfrage> findeAusgehende(UUID anfragenderOrgId) {
        return repository.findByAnfragenderOrgIdOrderByCreatedAtDesc(anfragenderOrgId);
    }

    @Transactional(readOnly = true)
    public long zaehleNeue(UUID empfaengerOrgId) {
        return repository.countByEmpfaengerOrgIdAndStatus(empfaengerOrgId, AnfrageStatus.NEU);
    }

    /** Aggregat: Anzahl aller eingehenden Anfragen für mehrere Orgs (Dashboard). */
    @Transactional(readOnly = true)
    public long zaehleEingehende(Collection<UUID> empfaengerOrgIds) {
        if (empfaengerOrgIds == null || empfaengerOrgIds.isEmpty()) {
            return 0L;
        }
        return repository.countByEmpfaengerOrgIdIn(empfaengerOrgIds);
    }

    /** Aggregat: Anzahl der NEU-Anfragen für mehrere Orgs (Dashboard). */
    @Transactional(readOnly = true)
    public long zaehleNeue(Collection<UUID> empfaengerOrgIds) {
        if (empfaengerOrgIds == null || empfaengerOrgIds.isEmpty()) {
            return 0L;
        }
        return repository.countByEmpfaengerOrgIdInAndStatus(empfaengerOrgIds, AnfrageStatus.NEU);
    }

    public SponsoringAnfrage erstelle(SponsoringPaket paket,
            Organisation anfragenderOrg,
            Organisation empfaengerOrg,
            String nachricht,
            String kontaktName,
            String kontaktEmail) {
        if (nachricht == null || nachricht.isBlank()) {
            throw new IllegalArgumentException("Nachricht darf nicht leer sein");
        }

        SponsoringAnfrage anfrage = new SponsoringAnfrage();
        anfrage.setPaket(paket);
        anfrage.setAnfragenderOrg(anfragenderOrg);
        anfrage.setEmpfaengerOrg(empfaengerOrg);
        anfrage.setNachricht(nachricht.trim());
        anfrage.setKontaktName(kontaktName);
        anfrage.setKontaktEmail(kontaktEmail);
        anfrage.setStatus(AnfrageStatus.NEU);
        SponsoringAnfrage gespeichert = repository.save(anfrage);

        // Benachrichtigung an Empfänger-Org (async)
        benachrichtigungsService.benachrichtigeUeberNeueAnfrage(gespeichert, kontaktEmail);

        // In-App-Notification an alle Mitglieder der Empfänger-Org
        benachrichtigeMitglieder(empfaengerOrg.getId(), BenachrichtigungTyp.NEUE_ANFRAGE,
                "Neue Sponsoring-Anfrage",
                "Von " + (kontaktName != null ? kontaktName : "Unbekannt"),
                "/organisationen/" + empfaengerOrg.getSlug() + "/anfragen");

        return gespeichert;
    }

    public SponsoringAnfrage annehme(UUID anfrageId, String antwort) {
        SponsoringAnfrage anfrage = laden(anfrageId);
        pruefeNichtBeantwortet(anfrage);

        anfrage.setStatus(AnfrageStatus.ANGENOMMEN);
        anfrage.setAntwort(antwort);
        anfrage.setBeantwortetAm(Instant.now());
        SponsoringAnfrage gespeichert = repository.save(anfrage);

        benachrichtigungsService.benachrichtigeUeberAntwort(gespeichert, anfrage.getKontaktEmail());

        // In-App-Notification an anfragende Org
        benachrichtigeMitglieder(anfrage.getAnfragenderOrg().getId(), BenachrichtigungTyp.ANFRAGE_ANGENOMMEN,
                "Anfrage angenommen",
                "Ihre Anfrage wurde angenommen",
                "/organisationen/" + anfrage.getAnfragenderOrg().getSlug() + "/anfragen");

        return gespeichert;
    }

    public SponsoringAnfrage lehneAb(UUID anfrageId, String antwort) {
        SponsoringAnfrage anfrage = laden(anfrageId);
        pruefeNichtBeantwortet(anfrage);

        anfrage.setStatus(AnfrageStatus.ABGELEHNT);
        anfrage.setAntwort(antwort);
        anfrage.setBeantwortetAm(Instant.now());
        SponsoringAnfrage gespeichert = repository.save(anfrage);

        benachrichtigungsService.benachrichtigeUeberAntwort(gespeichert, anfrage.getKontaktEmail());

        // In-App-Notification an anfragende Org
        benachrichtigeMitglieder(anfrage.getAnfragenderOrg().getId(), BenachrichtigungTyp.ANFRAGE_ABGELEHNT,
                "Anfrage abgelehnt",
                "Ihre Anfrage wurde leider abgelehnt",
                "/organisationen/" + anfrage.getAnfragenderOrg().getSlug() + "/anfragen");

        return gespeichert;
    }

    private void benachrichtigeMitglieder(UUID orgId, BenachrichtigungTyp typ,
            String titel, String text, String link) {
        mitgliedschaftRepository.findByOrgId(orgId)
                .forEach(m -> notificationService.benachrichtige(m.getUser().getId(), typ, titel, text, link));
    }

    private SponsoringAnfrage laden(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Anfrage nicht gefunden: " + id));
    }

    private void pruefeNichtBeantwortet(SponsoringAnfrage anfrage) {
        if (BEANTWORTET.contains(anfrage.getStatus())) {
            throw new IllegalStateException("Anfrage wurde bereits beantwortet (Status: " + anfrage.getStatus() + ")");
        }
    }
}
