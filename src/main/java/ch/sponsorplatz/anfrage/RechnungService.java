package ch.sponsorplatz.anfrage;

import ch.sponsorplatz.audit.AuditAktion;
import ch.sponsorplatz.audit.AuditService;
import ch.sponsorplatz.shared.exception.NotFoundException;
import ch.sponsorplatz.organisation.Organisation;
import net.codecrete.qrbill.generator.Payments;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Optional;
import java.util.UUID;

/**
 * Sponsoring-Rechnungen aus Verträgen.
 *
 * <p>Geschäftsregeln:
 * <ul>
 *   <li>Rechnung kann aus jedem Vertrag erstellt werden, der nicht bereits eine hat.</li>
 *   <li>Org-IBAN muss vor Erstellung gepflegt sein — sonst kein QR-Bill möglich.</li>
 *   <li>Rechnungsnummer ist fortlaufend pro Org: {@code SP-<jahr>-<nummer>}.</li>
 *   <li>QR-Referenz wird automatisch berechnet (Mod-10-Recursive aus
 *       {@code <orgkurz><jahr><nummer><rechnungs-uuid>}) wenn IBAN ein QR-IBAN ist.</li>
 *   <li>{@link RechnungsStatus#OFFEN} → {@link RechnungsStatus#BEZAHLT} via
 *       {@code markiereBezahlt}; gegenrichtung über {@code stornieren}.</li>
 * </ul>
 */
@Service
@Transactional
public class RechnungService {

    private static final int FAELLIG_TAGE = 30;

    private final RechnungRepository repository;
    private final VertragService vertragService;
    private final RechnungsnummerGenerator rechnungsnummerGenerator;
    private final AuditService auditService;

    public RechnungService(RechnungRepository repository,
                           VertragService vertragService,
                           RechnungsnummerGenerator rechnungsnummerGenerator,
                           AuditService auditService) {
        this.repository = repository;
        this.vertragService = vertragService;
        this.rechnungsnummerGenerator = rechnungsnummerGenerator;
        this.auditService = auditService;
    }

    /**
     * Erstellt eine offene Rechnung aus dem Vertrag. Schreibt einen Snapshot
     * der relevanten Daten (IBAN, Sponsor, Betrag) in die Rechnung; der
     * Vertrag bleibt unverändert.
     *
     * @throws IllegalStateException wenn Org-IBAN fehlt, Vertrag schon eine
     *                               Rechnung hat oder Vertrag im falschen Status
     */
    public Rechnung erstelle(UUID vertragId, String erstelltVon) {
        Vertrag vertrag = vertragService.findeNachId(vertragId);
        if (repository.findByVertragId(vertragId).isPresent()) {
            throw new IllegalStateException("Für diesen Vertrag existiert bereits eine Rechnung.");
        }
        Organisation org = vertrag.getOrg();
        if (org == null) {
            throw new IllegalStateException("Vertrag hat keine Verein-Organisation hinterlegt.");
        }
        String iban = org.getIban();
        if (iban == null || iban.isBlank()) {
            throw new IllegalStateException(
                    "Verein hat keinen IBAN im Profil hinterlegt. Bitte unter "
                            + "Organisation/" + org.getSlug() + "/bearbeiten ergänzen.");
        }
        if (org.getStrasse() == null || org.getStrasse().isBlank()
                || org.getPostleitzahl() == null || org.getPostleitzahl().isBlank()
                || org.getOrt() == null || org.getOrt().isBlank()) {
            throw new IllegalStateException(
                    "Vereins-Adresse (Strasse, PLZ, Ort) ist Pflicht für die Swiss QR-Bill. "
                            + "Bitte unter Organisation/" + org.getSlug() + "/bearbeiten ergänzen.");
        }

        // Rechnungsnummer R-YYYY-NNNNN (lückenlos pro Org-Jahr, Buchhaltungs-Pflicht
        // OR Art. 957 ff. — siehe SPONSORING_ZAHLUNGSFLUSS.md §5).
        String rechnungsnummer = rechnungsnummerGenerator.naechste(org.getId());

        Rechnung r = new Rechnung();
        r.setId(UUID.randomUUID());
        r.setVertrag(vertrag);
        r.setOrg(org);
        r.setRechnungsnummer(rechnungsnummer);
        r.setStatus(RechnungsStatus.OFFEN);
        r.setBetragChf(vertrag.getPreisChf());
        r.setIban(iban);
        r.setSponsorName(vertrag.getSponsorName() != null ? vertrag.getSponsorName() : "—");
        r.setSponsorEmail(vertrag.getSponsorEmail());
        r.setZahlungszweck(vertrag.getPaketName() + " · " + rechnungsnummer);
        r.setErstelltVon(erstelltVon);
        r.setFaelligAm(LocalDate.now().plusDays(FAELLIG_TAGE));

        // QR-Referenz nur generieren, wenn IBAN ein QR-IBAN ist (Institut-ID 30000-31999)
        String iso13 = iban.replace(" ", "").toUpperCase();
        if (Payments.isQRIBAN(iso13)) {
            String basis = ziffernAus(rechnungsnummer + r.getId().toString());
            String berechnet = Payments.createQRReference(basis);
            r.setQrReferenz(berechnet);
        }

        Rechnung gespeichert = repository.save(r);

        // Audit-Pflicht-Event laut Spec §10
        auditService.protokolliere(AuditAktion.RECHNUNG_ERSTELLT, "RECHNUNG",
                gespeichert.getId(), "Rechnung",
                "rechnungsnummer=" + rechnungsnummer
                        + ", betrag=" + gespeichert.getBetragChf()
                        + ", erstellt_von=" + erstelltVon);

        return gespeichert;
    }

    @Transactional(readOnly = true)
    public Rechnung findeNachId(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new NotFoundException("Rechnung nicht gefunden: " + id));
    }

    @Transactional(readOnly = true)
    public Optional<Rechnung> findeNachVertrag(UUID vertragId) {
        return repository.findByVertragId(vertragId);
    }

    /**
     * Markiert eine offene Rechnung als bezahlt — manuell durch ORG_OWNER/EDITOR
     * nach Eingangs-Check auf dem Banking-Konto.
     */
    public Rechnung markiereBezahlt(UUID id, String bezahltVon) {
        Rechnung r = findeNachId(id);
        if (r.getStatus() != RechnungsStatus.OFFEN) {
            throw new IllegalStateException(
                    "Nur offene Rechnungen können als bezahlt markiert werden. Status: " + r.getStatus());
        }
        r.setStatus(RechnungsStatus.BEZAHLT);
        r.setBezahltAm(Instant.now());
        r.setBezahltVon(bezahltVon);
        Rechnung gespeichert = repository.save(r);

        auditService.protokolliere(AuditAktion.RECHNUNG_BEZAHLT, "RECHNUNG",
                gespeichert.getId(), "Rechnung",
                "rechnungsnummer=" + gespeichert.getRechnungsnummer()
                        + ", bezahlt_von=" + bezahltVon
                        + ", quelle=MANUELL");

        return gespeichert;
    }

    /**
     * Webhook-Variante: Markiert Rechnung als bezahlt via String-ID.
     * Wirft NotFoundException bei unbekannter ID, IllegalStateException wenn schon bezahlt (Idempotenz).
     */
    public void markiereAlsBezahltViaWebhook(String rechnungIdStr) {
        UUID id;
        try {
            id = UUID.fromString(rechnungIdStr);
        } catch (IllegalArgumentException e) {
            throw new NotFoundException("Ungueltige Rechnungs-ID: " + rechnungIdStr);
        }
        Rechnung r = findeNachId(id);
        if (r.getStatus() == RechnungsStatus.BEZAHLT) {
            throw new IllegalStateException("Rechnung bereits bezahlt (idempotent)");
        }
        r.setStatus(RechnungsStatus.BEZAHLT);
        r.setBezahltAm(Instant.now());
        r.setBezahltVon("webhook");
        Rechnung gespeichert = repository.save(r);

        auditService.protokolliere(AuditAktion.RECHNUNG_BEZAHLT, "RECHNUNG",
                gespeichert.getId(), "Rechnung",
                "rechnungsnummer=" + gespeichert.getRechnungsnummer()
                        + ", quelle=WEBHOOK_DATATRANS");
    }

    /**
     * Storniert eine offene Rechnung mit optionalem Grund. Bezahlte Rechnungen
     * können nicht storniert werden (Backlog: BEZAHLT → Rückabwicklung via
     * Provider-Refund). Lückenlosigkeit der Nummerierung bleibt erhalten —
     * stornierte Rechnungen behalten ihre Nummer.
     */
    public Rechnung stornieren(UUID id, String grund) {
        Rechnung r = findeNachId(id);
        if (r.getStatus() == RechnungsStatus.BEZAHLT) {
            throw new IllegalStateException("Bezahlte Rechnungen können nicht storniert werden.");
        }
        RechnungsStatus vorher = r.getStatus();
        r.setStatus(RechnungsStatus.STORNIERT);
        r.setStornoGrund(grund);
        Rechnung gespeichert = repository.save(r);

        auditService.protokolliere(AuditAktion.RECHNUNG_STORNIERT, "RECHNUNG",
                gespeichert.getId(), "Rechnung",
                "rechnungsnummer=" + gespeichert.getRechnungsnummer()
                        + ", vorheriger_status=" + vorher
                        + ", grund=" + (grund == null ? "" : grund));

        return gespeichert;
    }

    /** Reduziert einen String auf reine Ziffern, max. 26 Stellen (QR-Ref erlaubt 27 mit Prüfziffer). */
    private static String ziffernAus(String input) {
        StringBuilder sb = new StringBuilder();
        for (char c : input.toCharArray()) {
            if (Character.isDigit(c)) {
                sb.append(c);
                if (sb.length() == 26) break;
            } else if (Character.isLetter(c)) {
                // Buchstabe → Ziffer 1–26 mappen, dann erste Stelle
                int v = (Character.toUpperCase(c) - 'A') + 1;
                sb.append(v % 10);
                if (sb.length() == 26) break;
            }
        }
        // Mindestens 1 Ziffer; padding mit '0' wenn input nur wenige Zeichen lieferte
        while (sb.length() < 1) sb.append('0');
        return sb.toString();
    }

    /** Hilfs-Konversion Instant → LocalDate (für Tests). */
    @SuppressWarnings("unused")
    private static LocalDate toLocalDate(Instant i) {
        return i == null ? null : i.atZone(ZoneId.systemDefault()).toLocalDate();
    }
}
