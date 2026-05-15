package ch.sponsorplatz.projekt;

import ch.sponsorplatz.organisation.OrganisationView;
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
    private final ProjektRepository projektRepository;

    public SponsoringPaketService(SponsoringPaketRepository repository,
                                   ProjektRepository projektRepository) {
        this.repository = repository;
        this.projektRepository = projektRepository;
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

    /**
     * Snapshot für die Anfrage-Erstellungs-Form: Paket-Stammdaten + Projekt-Kopf +
     * Empfänger-Org als View. Controller braucht keine Entity (ARCH-02).
     */
    @Transactional(readOnly = true)
    public PaketAnfrageInfo findePaketAnfrageInfo(UUID paketId) {
        SponsoringPaket paket = repository.findByIdMitProjektUndOrg(paketId)
                .orElseThrow(() -> new NotFoundException("Paket nicht gefunden: " + paketId));
        Projekt projekt = paket.getProjekt();
        return new PaketAnfrageInfo(
                paket.getId(),
                paket.getName(),
                paket.getPreisChf(),
                projekt.getName(),
                projekt.getSlug(),
                OrganisationView.von(projekt.getOrg()));
    }

    /**
     * Datenpaket für die Anfrage-Erstellungs-Form — Paket + Projekt-Kopf +
     * Empfänger-Org-View ohne dass der Controller die Entity-Kette berühren muss.
     */
    public record PaketAnfrageInfo(
            UUID paketId,
            String paketName,
            BigDecimal paketPreisChf,
            String projektName,
            String projektSlug,
            OrganisationView empfaengerOrg) {}

    @Transactional(readOnly = true)
    public List<SponsoringPaket> findeNachProjekt(UUID projektId) {
        return repository.findByProjektIdOrderBySortierungAsc(projektId);
    }

    /** View-Variante — Controller braucht keine Entity-Liste (ARCH-02). */
    @Transactional(readOnly = true)
    public List<SponsoringPaketView> findeViewsNachProjekt(UUID projektId) {
        return SponsoringPaketView.von(findeNachProjekt(projektId));
    }

    /** Erstellt ein Paket via Projekt-Slug statt Entity (ARCH-02). */
    public void erstelleNachProjektSlug(String projektSlug, String name, String beschreibung, BigDecimal preisChf) {
        Projekt projekt = projektRepository.findBySlug(projektSlug)
                .orElseThrow(() -> new NotFoundException("Projekt nicht gefunden: " + projektSlug));
        erstelle(projekt, name, beschreibung, preisChf);
    }

    @Transactional(readOnly = true)
    public List<SponsoringPaket> findeAktiveNachProjekt(UUID projektId) {
        return repository.findByProjektIdAndAktivTrueOrderBySortierungAsc(projektId);
    }

    /** View-Variante — Controller braucht keine Entity-Liste (ARCH-02). */
    @Transactional(readOnly = true)
    public List<SponsoringPaketView> findeAktiveViewsNachProjekt(UUID projektId) {
        return SponsoringPaketView.von(findeAktiveNachProjekt(projektId));
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

