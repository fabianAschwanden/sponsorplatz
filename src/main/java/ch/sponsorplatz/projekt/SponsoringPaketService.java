package ch.sponsorplatz.projekt;

import ch.sponsorplatz.shared.exception.NotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional
public class SponsoringPaketService {

    private final SponsoringPaketRepository repository;

    public SponsoringPaketService(SponsoringPaketRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public Optional<SponsoringPaket> findeNachId(UUID paketId) {
        return repository.findById(paketId);
    }

    /**
     * Lädt Paket inkl. Projekt und Empfänger-Org — für Anfrage-Erstellungs-Form,
     * damit Template Org-Name/Projekt-Name ohne LazyInit anzeigen kann.
     */
    @Transactional(readOnly = true)
    public Optional<SponsoringPaket> findeNachIdMitProjektUndOrg(UUID paketId) {
        return repository.findByIdMitProjektUndOrg(paketId);
    }

    @Transactional(readOnly = true)
    public List<SponsoringPaket> findeNachProjekt(UUID projektId) {
        return repository.findByProjektIdOrderBySortierungAsc(projektId);
    }

    @Transactional(readOnly = true)
    public List<SponsoringPaket> findeAktiveNachProjekt(UUID projektId) {
        return repository.findByProjektIdAndAktivTrueOrderBySortierungAsc(projektId);
    }

    public SponsoringPaket erstelle(Projekt projekt, String name, String beschreibung, BigDecimal preisChf) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Paketname darf nicht leer sein");
        }

        SponsoringPaket paket = new SponsoringPaket();
        paket.setProjekt(projekt);
        paket.setName(name.trim());
        paket.setBeschreibung(beschreibung);
        paket.setPreisChf(preisChf);
        paket.setAktiv(true);
        return repository.save(paket);
    }

    public SponsoringPaket deaktiviere(UUID paketId) {
        SponsoringPaket paket = repository.findById(paketId)
                .orElseThrow(() -> new NotFoundException("Paket nicht gefunden: " + paketId));
        paket.setAktiv(false);
        return repository.save(paket);
    }
}

