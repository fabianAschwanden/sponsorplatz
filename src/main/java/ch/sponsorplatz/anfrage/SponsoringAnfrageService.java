package ch.sponsorplatz.anfrage;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ch.sponsorplatz.aufgabe.AufgabenEngine;
import ch.sponsorplatz.benachrichtigung.BenachrichtigungTyp;
import ch.sponsorplatz.benachrichtigung.NotificationService;
import ch.sponsorplatz.benutzer.AppUser;
import ch.sponsorplatz.benutzer.AppUserRepository;
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
    private final AppUserRepository appUserRepository;
    private final AufgabenEngine aufgabenEngine;

    public SponsoringAnfrageService(SponsoringAnfrageRepository repository,
            BenachrichtigungsService benachrichtigungsService,
            NotificationService notificationService,
            MitgliedschaftRepository mitgliedschaftRepository,
            AppUserRepository appUserRepository,
            AufgabenEngine aufgabenEngine) {
        this.repository = repository;
        this.benachrichtigungsService = benachrichtigungsService;
        this.notificationService = notificationService;
        this.mitgliedschaftRepository = mitgliedschaftRepository;
        this.appUserRepository = appUserRepository;
        this.aufgabenEngine = aufgabenEngine;
    }

    /**
     * Lookup einer Anfrage über die ID — Komfort-Methode für Controller (ARCH-01),
     * die {@link SponsoringAnfrageRepository} nicht direkt injizieren sollen.
     *
     * @throws ch.sponsorplatz.shared.exception.NotFoundException wenn die ID
     *         keiner existierenden Anfrage zugeordnet ist
     */
    @Transactional(readOnly = true)
    public SponsoringAnfrage findeNachId(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new ch.sponsorplatz.shared.exception.NotFoundException(
                        "Anfrage nicht gefunden: " + id));
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

    /** Alle eingehenden Anfragen über alle Orgs eines Users — für die persönliche Übersicht. */
    @Transactional(readOnly = true)
    public List<SponsoringAnfrage> findeAlleEingehenden(Collection<UUID> empfaengerOrgIds) {
        if (empfaengerOrgIds == null || empfaengerOrgIds.isEmpty()) {
            return List.of();
        }
        return repository.findByEmpfaengerOrgIdInOrderByCreatedAtDesc(empfaengerOrgIds);
    }

    /** Alle ausgehenden Anfragen über alle Orgs eines Users — für Verein→Sponsor-Übersicht. */
    @Transactional(readOnly = true)
    public List<SponsoringAnfrage> findeAlleAusgehenden(Collection<UUID> anfragenderOrgIds) {
        if (anfragenderOrgIds == null || anfragenderOrgIds.isEmpty()) {
            return List.of();
        }
        return repository.findByAnfragenderOrgIdInOrderByCreatedAtDesc(anfragenderOrgIds);
    }

    /**
     * Bucket „Meine ausgehende Anfragen" — vom angegebenen User selbst gestellt.
     */
    @Transactional(readOnly = true)
    public List<SponsoringAnfrage> findeAusgehendeVonUser(UUID userId) {
        if (userId == null) {
            return List.of();
        }
        return repository.findByErstelltVonIdOrderByCreatedAtDesc(userId);
    }

    /**
     * Bucket „Ausgehende Anfragen meiner Organisation" — von der/den eigenen
     * Vereins-Orgs gestellt, aber <em>nicht</em> vom angegebenen User selbst.
     * Historische Anfragen (erstelltVon IS NULL) landen ebenfalls hier.
     */
    @Transactional(readOnly = true)
    public List<SponsoringAnfrage> findeAusgehendeMeinerOrgsOhneUser(
            Collection<UUID> anfragenderOrgIds, UUID userId) {
        if (anfragenderOrgIds == null || anfragenderOrgIds.isEmpty() || userId == null) {
            return List.of();
        }
        return repository.findOrgAusgehendNichtVonUser(anfragenderOrgIds, userId);
    }

    public SponsoringAnfrage erstelle(SponsoringPaket paket,
            Organisation anfragenderOrg,
            Organisation empfaengerOrg,
            String nachricht,
            String kontaktName,
            String kontaktEmail,
            UUID erstelltVonUserId) {
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
        anfrage.setErstelltVon(ladeUser(erstelltVonUserId));
        SponsoringAnfrage gespeichert = repository.save(anfrage);

        // Benachrichtigung an Empfänger-Org (async)
        benachrichtigungsService.benachrichtigeUeberNeueAnfrage(gespeichert, kontaktEmail);

        // In-App-Notification an alle Mitglieder der Empfänger-Org
        benachrichtigeMitglieder(empfaengerOrg.getId(), BenachrichtigungTyp.NEUE_ANFRAGE,
                "Neue Sponsoring-Anfrage",
                "Von " + (kontaktName != null ? kontaktName : "Unbekannt"),
                "/organisationen/" + empfaengerOrg.getSlug() + "/anfragen");

        aufgabenEngine.onAnfrageStatusWechsel(gespeichert);
        return gespeichert;
    }

    /**
     * Erstellt eine Kontakt-Anfrage (Verein → Sponsor) ohne Paket-Bindung.
     * Betreff + Nachricht sind Pflicht; das Paket bleibt {@code null}.
     */
    public SponsoringAnfrage erstelleKontaktAnfrage(Organisation anfragenderOrg,
            Organisation empfaengerOrg,
            String betreff,
            String nachricht,
            String kontaktName,
            String kontaktEmail,
            java.math.BigDecimal wunschBetragChf,
            UUID erstelltVonUserId) {
        if (betreff == null || betreff.isBlank()) {
            throw new IllegalArgumentException("Betreff darf nicht leer sein");
        }
        if (nachricht == null || nachricht.isBlank()) {
            throw new IllegalArgumentException("Nachricht darf nicht leer sein");
        }
        if (anfragenderOrg.getId().equals(empfaengerOrg.getId())) {
            throw new IllegalArgumentException("Eigene Org kann nicht angefragt werden");
        }
        if (wunschBetragChf != null && wunschBetragChf.signum() < 0) {
            // Defense-in-depth — DB-CHECK gibt's auch, aber hier mit klarer Fehlermeldung
            throw new IllegalArgumentException("Wunsch-Betrag darf nicht negativ sein");
        }

        SponsoringAnfrage anfrage = new SponsoringAnfrage();
        anfrage.setAnfragenderOrg(anfragenderOrg);
        anfrage.setEmpfaengerOrg(empfaengerOrg);
        anfrage.setBetreff(betreff.trim());
        anfrage.setNachricht(nachricht.trim());
        anfrage.setKontaktName(kontaktName);
        anfrage.setKontaktEmail(kontaktEmail);
        anfrage.setWunschBetragChf(wunschBetragChf);
        anfrage.setStatus(AnfrageStatus.NEU);
        anfrage.setErstelltVon(ladeUser(erstelltVonUserId));
        SponsoringAnfrage gespeichert = repository.save(anfrage);

        // Benachrichtigung an Empfänger-Org
        benachrichtigungsService.benachrichtigeUeberNeueAnfrage(gespeichert, kontaktEmail);

        benachrichtigeMitglieder(empfaengerOrg.getId(), BenachrichtigungTyp.NEUE_ANFRAGE,
                "Neue Kontakt-Anfrage",
                "Von " + anfragenderOrg.getName() + ": " + betreff.trim(),
                "/anfragen");

        aufgabenEngine.onAnfrageStatusWechsel(gespeichert);
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

        aufgabenEngine.onAnfrageStatusWechsel(gespeichert);
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

        aufgabenEngine.onAnfrageStatusWechsel(gespeichert);
        return gespeichert;
    }

    private void benachrichtigeMitglieder(UUID orgId, BenachrichtigungTyp typ,
            String titel, String text, String link) {
        mitgliedschaftRepository.findByOrgId(orgId)
                .forEach(m -> notificationService.benachrichtige(m.getUser().getId(), typ, titel, text, link));
    }

    /**
     * Liefert die Empfänger-Org-ID einer Anfrage — wird vom MeineAnfragenController
     * für den Authorization-Check benutzt, ohne dass der Controller die ganze
     * Anfrage laden muss.
     */
    @Transactional(readOnly = true)
    public UUID findeEmpfaengerOrgId(UUID anfrageId) {
        return repository.findById(anfrageId)
                .map(a -> a.getEmpfaengerOrg().getId())
                .orElseThrow(() -> new IllegalArgumentException("Anfrage nicht gefunden: " + anfrageId));
    }

    private SponsoringAnfrage laden(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Anfrage nicht gefunden: " + id));
    }

    /**
     * Lädt den User, der die Anfrage stellt. Bewusst defensiv: {@code null}
     * (etwa weil der Controller kein Auth-Kontext hatte) → wir akzeptieren
     * NULL erstelltVon. Eine unbekannte UUID ist dagegen ein Programmfehler,
     * darum {@link IllegalArgumentException}.
     */
    private AppUser ladeUser(UUID userId) {
        if (userId == null) {
            return null;
        }
        return appUserRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Erstellender User nicht gefunden: " + userId));
    }

    private void pruefeNichtBeantwortet(SponsoringAnfrage anfrage) {
        if (BEANTWORTET.contains(anfrage.getStatus())) {
            throw new IllegalStateException("Anfrage wurde bereits beantwortet (Status: " + anfrage.getStatus() + ")");
        }
    }
}
