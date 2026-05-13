package ch.sponsorplatz.anfrage;

import ch.sponsorplatz.audit.AuditAktion;
import ch.sponsorplatz.audit.AuditService;
import ch.sponsorplatz.organisation.OrgTyp;
import ch.sponsorplatz.organisation.Organisation;
import ch.sponsorplatz.shared.exception.NotFoundException;
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
    private final AuditService auditService;

    public VertragService(VertragRepository repository,
                          SponsoringAnfrageRepository anfrageRepository,
                          AuditService auditService) {
        this.repository = repository;
        this.anfrageRepository = anfrageRepository;
        this.auditService = auditService;
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

        // Welche Org ist Verein, welche Sponsor? Bei Paket-Anfragen ist der
        // Verein der Empfänger, bei Kontakt-Anfragen der Anfragende. Wir
        // mappen über den OrgTyp statt über Anfrage-Richtung — funktioniert
        // robust für beide Flow-Richtungen.
        Organisation vereinOrg = (anfrage.getEmpfaengerOrg().getTyp() == OrgTyp.VEREIN)
                ? anfrage.getEmpfaengerOrg()
                : anfrage.getAnfragenderOrg();
        Organisation sponsorOrg = (anfrage.getEmpfaengerOrg().getTyp() == OrgTyp.VEREIN)
                ? anfrage.getAnfragenderOrg()
                : anfrage.getEmpfaengerOrg();

        v.setOrg(vereinOrg);
        v.setOrgName(vereinOrg.getName());

        v.setSponsorName(anfrage.getKontaktName());
        v.setSponsorEmail(anfrage.getKontaktEmail());
        v.setSponsorOrg(sponsorOrg);

        // Snapshot des Pakets bei klassischer Paket-Anfrage. Kontakt-Anfragen
        // haben kein Paket — Preis kommt aus anfrage.wunschBetragChf (V33),
        // fällt auf 0 zurück wenn der Verein keinen Richtbetrag nannte.
        // Leistungsumfang ergänzt der Verein-Owner im Vertrag-Edit-Form.
        if (anfrage.getPaket() != null) {
            v.setPaketName(anfrage.getPaket().getName());
            v.setPaketBeschreibung(anfrage.getPaket().getBeschreibung());
            v.setPreisChf(anfrage.getPaket().getPreisChf());
        } else {
            v.setPaketName(anfrage.getBetreff() != null
                    ? anfrage.getBetreff()
                    : "(Kontakt-Anfrage ohne Betreff)");
            v.setPaketBeschreibung(anfrage.getNachricht());
            v.setPreisChf(anfrage.getWunschBetragChf() != null
                    ? anfrage.getWunschBetragChf()
                    : java.math.BigDecimal.ZERO);
        }

        v.setErstelltVon(erstelltVon);
        Vertrag gespeichert = repository.save(v);

        auditService.protokolliere(AuditAktion.VERTRAG_ERSTELLT, "VERTRAG",
                gespeichert.getId(), "Vertrag",
                "anfrage_id=" + anfrage.getId()
                        + ", verein=" + gespeichert.getOrgName()
                        + ", erstellt_von=" + erstelltVon);

        return gespeichert;
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
        Vertrag gespeichert = repository.save(v);

        auditService.protokolliere(AuditAktion.VERTRAG_UNTERZEICHNET, "VERTRAG",
                gespeichert.getId(), "Vertrag",
                "unterzeichnet_von=" + unterzeichnetVon);

        return gespeichert;
    }
}
