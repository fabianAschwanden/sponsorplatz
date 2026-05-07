package ch.sponsorplatz.service;

import ch.sponsorplatz.shared.exception.NotFoundException;
import ch.sponsorplatz.model.AnfrageStatus;
import ch.sponsorplatz.model.SponsoringAnfrage;
import ch.sponsorplatz.model.Vertrag;
import ch.sponsorplatz.model.VertragsStatus;
import ch.sponsorplatz.repository.SponsoringAnfrageRepository;
import ch.sponsorplatz.repository.VertragRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/**
 * Verträge aus angenommenen Sponsoring-Anfragen.
 *
 * <p>Geschäftsregeln:
 * <ul>
 *   <li>Vertrag kann nur aus {@link AnfrageStatus#ANGENOMMEN}-Anfragen entstehen.</li>
 *   <li>Pro Anfrage maximal 1 Vertrag (UNIQUE-Constraint).</li>
 *   <li>Snapshot-Felder (Org-/Sponsor-/Paket-Daten) werden bei Erstellung kopiert
 *       und bleiben unverändert — nachträgliche Änderungen am Quell-Paket
 *       wirken nicht auf den Vertrag zurück.</li>
 *   <li>Status {@link VertragsStatus#UNTERZEICHNET} setzt Zeitstempel + User.</li>
 * </ul>
 */
@Service
@Transactional
public class VertragService {

    private final VertragRepository repository;
    private final SponsoringAnfrageRepository anfrageRepository;

    public VertragService(VertragRepository repository,
                          SponsoringAnfrageRepository anfrageRepository) {
        this.repository = repository;
        this.anfrageRepository = anfrageRepository;
    }

    /**
     * Erstellt einen Entwurf-Vertrag für eine angenommene Anfrage.
     *
     * @throws IllegalStateException wenn Anfrage nicht angenommen ist oder
     *                               schon ein Vertrag existiert
     * @throws NotFoundException     wenn Anfrage nicht existiert
     */
    public Vertrag erstelle(UUID anfrageId, String erstelltVon) {
        SponsoringAnfrage anfrage = anfrageRepository.findById(anfrageId)
                .orElseThrow(() -> new NotFoundException("Anfrage nicht gefunden: " + anfrageId));

        if (anfrage.getStatus() != AnfrageStatus.ANGENOMMEN) {
            throw new IllegalStateException(
                    "Vertrag kann nur aus angenommener Anfrage erstellt werden. Aktueller Status: "
                            + anfrage.getStatus());
        }
        if (repository.findByAnfrageId(anfrageId).isPresent()) {
            throw new IllegalStateException("Für diese Anfrage existiert bereits ein Vertrag.");
        }

        Vertrag v = new Vertrag();
        v.setAnfrage(anfrage);
        v.setStatus(VertragsStatus.ENTWURF);

        // Snapshot der Verein-Seite (Empfänger der Anfrage)
        v.setOrg(anfrage.getEmpfaengerOrg());
        v.setOrgName(anfrage.getEmpfaengerOrg().getName());

        // Snapshot der Sponsor-Seite — Kontakt-Daten aus der Anfrage selbst
        v.setSponsorName(anfrage.getKontaktName());
        v.setSponsorEmail(anfrage.getKontaktEmail());
        v.setSponsorOrg(anfrage.getAnfragenderOrg());

        // Snapshot des Pakets — wird vom Aufrufer eingetragen, wenn das Anfrage-
        // Modell die Paket-Referenz hat. Falls null, ENTWURF kann später ergänzt
        // werden im Edit-Form.
        if (anfrage.getPaket() != null) {
            v.setPaketName(anfrage.getPaket().getName());
            v.setPaketBeschreibung(anfrage.getPaket().getBeschreibung());
            v.setPreisChf(anfrage.getPaket().getPreisChf());
        } else {
            v.setPaketName("(Paket nicht angegeben)");
            v.setPreisChf(java.math.BigDecimal.ZERO);
        }

        v.setErstelltVon(erstelltVon);
        return repository.save(v);
    }

    @Transactional(readOnly = true)
    public Vertrag findeNachId(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new NotFoundException("Vertrag nicht gefunden: " + id));
    }

    @Transactional(readOnly = true)
    public Optional<Vertrag> findeNachAnfrage(UUID anfrageId) {
        return repository.findByAnfrageId(anfrageId);
    }

    /**
     * Aktualisiert die im Entwurf editierbaren Felder. Status muss
     * {@link VertragsStatus#ENTWURF} sein.
     */
    public Vertrag aktualisiere(UUID id, Vertrag aenderungen) {
        Vertrag v = findeNachId(id);
        if (v.getStatus() != VertragsStatus.ENTWURF) {
            throw new IllegalStateException(
                    "Nur Entwurf-Verträge können bearbeitet werden. Aktueller Status: " + v.getStatus());
        }
        if (aenderungen.getPaketBeschreibung() != null) v.setPaketBeschreibung(aenderungen.getPaketBeschreibung());
        if (aenderungen.getPreisChf() != null) v.setPreisChf(aenderungen.getPreisChf());
        if (aenderungen.getLaufzeitVon() != null) v.setLaufzeitVon(aenderungen.getLaufzeitVon());
        if (aenderungen.getLaufzeitBis() != null) v.setLaufzeitBis(aenderungen.getLaufzeitBis());
        if (aenderungen.getLeistungVerein() != null) v.setLeistungVerein(aenderungen.getLeistungVerein());
        if (aenderungen.getLeistungSponsor() != null) v.setLeistungSponsor(aenderungen.getLeistungSponsor());
        return repository.save(v);
    }

    /**
     * Markiert einen Entwurf als unterzeichnet. Setzt Zeitstempel + User.
     */
    public Vertrag markiereUnterzeichnet(UUID id, String unterzeichnetVon) {
        Vertrag v = findeNachId(id);
        if (v.getStatus() != VertragsStatus.ENTWURF) {
            throw new IllegalStateException(
                    "Nur Entwurf-Verträge können unterzeichnet werden. Status: " + v.getStatus());
        }
        v.setStatus(VertragsStatus.UNTERZEICHNET);
        v.setUnterzeichnetAm(Instant.now());
        v.setUnterzeichnetVon(unterzeichnetVon);
        return repository.save(v);
    }
}
