package ch.sponsorplatz.service;

import ch.sponsorplatz.shared.exception.NotFoundException;
import ch.sponsorplatz.model.Organisation;
import ch.sponsorplatz.model.Rechnung;
import ch.sponsorplatz.model.RechnungsStatus;
import ch.sponsorplatz.model.Vertrag;
import ch.sponsorplatz.repository.RechnungRepository;
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

    public RechnungService(RechnungRepository repository, VertragService vertragService) {
        this.repository = repository;
        this.vertragService = vertragService;
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

        long count = repository.countByOrgId(org.getId());
        int jahr = LocalDate.now().getYear();
        String rechnungsnummer = String.format("SP-%d-%04d", jahr, count + 1);

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

        return repository.save(r);
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
        return repository.save(r);
    }

    public Rechnung stornieren(UUID id) {
        Rechnung r = findeNachId(id);
        if (r.getStatus() == RechnungsStatus.BEZAHLT) {
            throw new IllegalStateException("Bezahlte Rechnungen können nicht storniert werden.");
        }
        r.setStatus(RechnungsStatus.STORNIERT);
        return repository.save(r);
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
