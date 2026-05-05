package ch.sponsorplatz.service;

import ch.sponsorplatz.model.AnfrageStatus;
import ch.sponsorplatz.model.Organisation;
import ch.sponsorplatz.model.SponsoringAnfrage;
import ch.sponsorplatz.model.SponsoringPaket;
import ch.sponsorplatz.repository.SponsoringAnfrageRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@Transactional
public class SponsoringAnfrageService {

    private static final Set<AnfrageStatus> BEANTWORTET = Set.of(
            AnfrageStatus.ANGENOMMEN, AnfrageStatus.ABGELEHNT);

    private final SponsoringAnfrageRepository repository;
    private final BenachrichtigungsService benachrichtigungsService;

    public SponsoringAnfrageService(SponsoringAnfrageRepository repository,
                                     BenachrichtigungsService benachrichtigungsService) {
        this.repository = repository;
        this.benachrichtigungsService = benachrichtigungsService;
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
        return gespeichert;
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

