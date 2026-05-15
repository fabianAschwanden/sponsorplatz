package ch.sponsorplatz.anfrage;

import ch.sponsorplatz.audit.AuditAktion;
import ch.sponsorplatz.audit.AuditService;
import ch.sponsorplatz.aufgabe.AufgabenEngine;
import ch.sponsorplatz.organisation.OrgTyp;
import ch.sponsorplatz.organisation.Organisation;
import ch.sponsorplatz.shared.exception.NotFoundException;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
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
    /**
     * @Lazy bricht den potenziellen Cycle VertragService ↔ RechnungService:
     * RechnungService injiziert VertragService (für findeNachId), VertragService
     * braucht RechnungService nur in kuendige() um die offene Rechnung mit zu
     * stornieren. Lazy-Proxy → keine Eager-Initialisierung.
     */
    private final RechnungService rechnungService;
    private final AufgabenEngine aufgabenEngine;

    public VertragService(VertragRepository repository,
                          SponsoringAnfrageRepository anfrageRepository,
                          AuditService auditService,
                          @Lazy RechnungService rechnungService,
                          AufgabenEngine aufgabenEngine) {
        this.repository = repository;
        this.anfrageRepository = anfrageRepository;
        this.auditService = auditService;
        this.rechnungService = rechnungService;
        this.aufgabenEngine = aufgabenEngine;
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

        aufgabenEngine.onStatusWechsel(ch.sponsorplatz.aufgabe.TriggerEntityTyp.VERTRAG, gespeichert.getId(), gespeichert.getStatus().name(), ch.sponsorplatz.aufgabe.AssigneeKontext.ausVertragOrgs(gespeichert.getOrg(), gespeichert.getSponsorOrg()));
        return gespeichert;
    }

    @Transactional(readOnly = true)
    public Vertrag findeNachId(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new NotFoundException("Vertrag nicht gefunden: " + id));
    }

    /** View statt Entity — Controller braucht keine Entity-Methoden (ARCH-02). */
    @Transactional(readOnly = true)
    public VertragView findeViewNachId(UUID id) {
        return VertragView.von(findeNachId(id));
    }

    /** Form-DTO für den Edit-Pfad — kein Entity-Touch im Controller. */
    @Transactional(readOnly = true)
    public VertragFormDto findeFormularNachId(UUID id) {
        Vertrag v = findeNachId(id);
        VertragFormDto f = new VertragFormDto();
        f.setPaketBeschreibung(v.getPaketBeschreibung());
        f.setPreisChf(v.getPreisChf());
        f.setLaufzeitVon(v.getLaufzeitVon());
        f.setLaufzeitBis(v.getLaufzeitBis());
        f.setLeistungVerein(v.getLeistungVerein());
        f.setLeistungSponsor(v.getLeistungSponsor());
        return f;
    }

    /** View-Variante von {@link #erstelle(UUID, String)}. */
    public VertragView erstelleAlsView(UUID anfrageId, String erstelltVon) {
        return VertragView.von(erstelle(anfrageId, erstelltVon));
    }

    /**
     * Aktualisierung über das Form-DTO, ohne dass der Controller eine
     * Vertrag-Entity selbst anlegen muss (ARCH-02).
     */
    public void aktualisiereAusForm(UUID id, VertragFormDto form) {
        Vertrag aenderungen = new Vertrag();
        aenderungen.setPaketBeschreibung(form.getPaketBeschreibung());
        aenderungen.setPreisChf(form.getPreisChf());
        aenderungen.setLaufzeitVon(form.getLaufzeitVon());
        aenderungen.setLaufzeitBis(form.getLaufzeitBis());
        aenderungen.setLeistungVerein(form.getLeistungVerein());
        aenderungen.setLeistungSponsor(form.getLeistungSponsor());
        aktualisiere(id, aenderungen);
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
        // Pflicht-Check vor Unterzeichnung: entweder Geld-Sponsoring (preisChf > 0)
        // ODER explizit Naturalien-Sponsoring (Leistung Verein/Sponsor gepflegt).
        // Verhindert versehentliche Unterzeichnung leerer Entwürfe — der Verein
        // muss bewusst eine der beiden Sponsoring-Formen wählen.
        boolean hatGeldsponsoring = v.getPreisChf() != null
                && v.getPreisChf().compareTo(BigDecimal.ZERO) > 0;
        boolean hatNaturalien = istNichtLeer(v.getLeistungVerein())
                || istNichtLeer(v.getLeistungSponsor());
        if (!hatGeldsponsoring && !hatNaturalien) {
            throw new IllegalStateException(
                    "Vertrag braucht einen Preis > 0 oder eine Leistungsbeschreibung "
                            + "(Naturalien-Sponsoring), bevor er unterzeichnet werden kann.");
        }

        v.setStatus(VertragsStatus.UNTERZEICHNET);
        v.setUnterzeichnetAm(Instant.now());
        v.setUnterzeichnetVon(unterzeichnetVon);
        Vertrag gespeichert = repository.save(v);

        auditService.protokolliere(AuditAktion.VERTRAG_UNTERZEICHNET, "VERTRAG",
                gespeichert.getId(), "Vertrag",
                "unterzeichnet_von=" + unterzeichnetVon);

        aufgabenEngine.onStatusWechsel(ch.sponsorplatz.aufgabe.TriggerEntityTyp.VERTRAG, gespeichert.getId(), gespeichert.getStatus().name(), ch.sponsorplatz.aufgabe.AssigneeKontext.ausVertragOrgs(gespeichert.getOrg(), gespeichert.getSponsorOrg()));
        return gespeichert;
    }

    /**
     * Kündigt einen unterzeichneten Vertrag. Konsistenz mit der zugehörigen
     * Rechnung (Spec §3.3):
     * <ul>
     *   <li>Rechnung BEZAHLT → wirft {@link IllegalStateException} (Buchhaltungs-
     *       Integrität, manuelle Rückabwicklung nötig)</li>
     *   <li>Rechnung OFFEN → wird mit-storniert (Grund: „Vertrag gekündigt: …")</li>
     *   <li>Keine Rechnung / Rechnung schon STORNIERT → einfacher Pfad</li>
     * </ul>
     * Status-Übergang nur aus UNTERZEICHNET; ENTWURF + GEKUENDIGT werfen.
     */
    public Vertrag kuendige(UUID id, String grund) {
        Vertrag v = findeNachId(id);
        if (v.getStatus() != VertragsStatus.UNTERZEICHNET) {
            throw new IllegalStateException(
                    "Nur unterzeichnete Verträge können gekündigt werden. Status: " + v.getStatus());
        }

        // Rechnungs-Konsistenz prüfen
        rechnungService.findeNachVertrag(id).ifPresent(rechnung -> {
            if (rechnung.getStatus() == RechnungsStatus.BEZAHLT) {
                throw new IllegalStateException(
                        "Vertrag mit bezahlter Rechnung kann nicht gekündigt werden. "
                                + "Rechnung muss erst storniert/zurückgebucht werden.");
            }
            if (rechnung.getStatus() == RechnungsStatus.OFFEN) {
                rechnungService.stornieren(rechnung.getId(),
                        "Vertrag gekündigt" + (grund != null ? ": " + grund : ""));
            }
        });

        v.setStatus(VertragsStatus.GEKUENDIGT);
        v.setGekuendigtAm(Instant.now());
        v.setKuendigungsGrund(grund);
        Vertrag gespeichert = repository.save(v);

        auditService.protokolliere(AuditAktion.VERTRAG_GEKUENDIGT, "VERTRAG",
                gespeichert.getId(), "Vertrag",
                "grund=" + (grund == null ? "" : grund));

        aufgabenEngine.onStatusWechsel(ch.sponsorplatz.aufgabe.TriggerEntityTyp.VERTRAG, gespeichert.getId(), gespeichert.getStatus().name(), ch.sponsorplatz.aufgabe.AssigneeKontext.ausVertragOrgs(gespeichert.getOrg(), gespeichert.getSponsorOrg()));
        return gespeichert;
    }

    private static boolean istNichtLeer(String s) {
        return s != null && !s.isBlank();
    }
}
