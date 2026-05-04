package ch.sponsorplatz.service;

import ch.sponsorplatz.model.AnfrageStatus;
import ch.sponsorplatz.model.Organisation;
import ch.sponsorplatz.model.SponsoringAnfrage;
import ch.sponsorplatz.model.SponsoringPaket;
import ch.sponsorplatz.repository.SponsoringAnfrageRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@Transactional
public class SponsoringAnfrageService {

    private static final Set<AnfrageStatus> BEANTWORTET = Set.of(
            AnfrageStatus.ANGENOMMEN, AnfrageStatus.ABGELEHNT);

    private final SponsoringAnfrageRepository repository;

    public SponsoringAnfrageService(SponsoringAnfrageRepository repository) {
        this.repository = repository;
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
        return repository.save(anfrage);
    }

    public SponsoringAnfrage annehme(UUID anfrageId, String antwort) {
        SponsoringAnfrage anfrage = laden(anfrageId);
        pruefeNichtBeantwortet(anfrage);

        anfrage.setStatus(AnfrageStatus.ANGENOMMEN);
        anfrage.setAntwort(antwort);
        anfrage.setBeantwortetAm(Instant.now());
        return repository.save(anfrage);
    }

    public SponsoringAnfrage lehneAb(UUID anfrageId, String antwort) {
        SponsoringAnfrage anfrage = laden(anfrageId);
        pruefeNichtBeantwortet(anfrage);

        anfrage.setStatus(AnfrageStatus.ABGELEHNT);
        anfrage.setAntwort(antwort);
        anfrage.setBeantwortetAm(Instant.now());
        return repository.save(anfrage);
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

