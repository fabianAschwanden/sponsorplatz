package ch.sponsorplatz.anfrage;

import ch.sponsorplatz.shared.mail.MailAnhang;
import ch.sponsorplatz.shared.mail.MailVersand;
import ch.sponsorplatz.shared.pdf.PdfGeneratorService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Versendet Rechnungen mit Swiss-QR-Bill-PDF per E-Mail an den Sponsor.
 *
 * <p>Wird automatisch nach Rechnungs-Erstellung aufgerufen, sofern der Payment-
 * Modus QR_RECHNUNG aktiv ist — dann ist die E-Mail mit QR-Bill der primäre
 * Zahlungskanal: der Sponsor scannt den QR-Code in seiner Banking-App und
 * überweist direkt auf den Vereins-IBAN.
 *
 * <p>Mail-Versand ist {@code @Async} — ein SMTP-Fehler blockiert den
 * Request-Thread nicht und bricht die Rechnungs-Erstellung nicht ab. Weil der
 * Async-Thread eine eigene Persistence-Session bekommt, wird die Rechnung hier
 * <strong>per ID neu in einer eigenen Transaktion geladen</strong> — sonst
 * würde der Zugriff auf die LAZY-Relation {@code org} auf der detached Entity
 * eine {@code LazyInitializationException} werfen.
 */
@Service
public class RechnungsMailService {

    private static final Logger log = LoggerFactory.getLogger(RechnungsMailService.class);

    private final MailVersand mailService;
    private final PdfGeneratorService pdfGenerator;
    private final QrBillService qrBillService;
    private final RechnungRepository rechnungRepository;

    public RechnungsMailService(MailVersand mailService,
                                PdfGeneratorService pdfGenerator,
                                QrBillService qrBillService,
                                RechnungRepository rechnungRepository) {
        this.mailService = mailService;
        this.pdfGenerator = pdfGenerator;
        this.qrBillService = qrBillService;
        this.rechnungRepository = rechnungRepository;
    }

    /**
     * Lädt die Rechnung in einer eigenen Transaktion und sendet sie als PDF mit
     * Swiss-QR-Bill an die Sponsor-E-Mail. Kein Fehler-Throw — Probleme werden
     * geloggt. Async + eigene Transaktion: LAZY-Relationen (org) sind innerhalb
     * dieser Methode initialisierbar.
     */
    @Async
    @Transactional(readOnly = true)
    public void sendeRechnungPerEmail(UUID rechnungId) {
        Rechnung rechnung = rechnungRepository.findById(rechnungId).orElse(null);
        if (rechnung == null) {
            log.warn("Rechnung {} nicht gefunden — überspringe Mail-Versand", rechnungId);
            return;
        }

        String empfaenger = rechnung.getSponsorEmail();
        if (empfaenger == null || empfaenger.isBlank()) {
            log.info("Keine Sponsor-E-Mail auf Rechnung {} — überspringe Mail-Versand",
                    rechnung.getRechnungsnummer());
            return;
        }

        try {
            byte[] pdf = erzeugePdf(rechnung);
            String dateiname = "sponsorplatz-rechnung-" + rechnung.getRechnungsnummer() + ".pdf";
            String betreff = "Rechnung " + rechnung.getRechnungsnummer()
                    + " — " + rechnung.getOrg().getName();

            mailService.sendeHtmlMitAnhang(empfaenger, betreff, mailBody(rechnung),
                    new MailAnhang(dateiname, pdf, "application/pdf"));

            log.info("Rechnung {} per E-Mail an {} gesendet",
                    rechnung.getRechnungsnummer(), empfaenger);
        } catch (RuntimeException e) {
            log.error("Rechnungs-Mail an {} fehlgeschlagen (Rechnung {}): {}",
                    empfaenger, rechnung.getRechnungsnummer(), e.getMessage());
        }
    }

    private byte[] erzeugePdf(Rechnung rechnung) {
        Map<String, Object> vars = new HashMap<>();
        vars.put("rechnung", RechnungView.von(rechnung));
        vars.put("qrBildDataUrl", qrBillService.erzeugeAlsDataUrl(rechnung));
        vars.put("erstelltAmDatum", LocalDate.now());
        return pdfGenerator.erzeuge("anfrage/rechnung-pdf", vars, "/");
    }

    private String mailBody(Rechnung rechnung) {
        return """
                <h2>Rechnung %s</h2>
                <p>Guten Tag</p>
                <p>Anbei erhalten Sie die Rechnung <strong>%s</strong> über
                <strong>CHF %s</strong> von %s.</p>
                <p>Die Rechnung ist fällig am <strong>%s</strong>.</p>
                <h3>So bezahlen Sie</h3>
                <p>Scannen Sie den <strong>Swiss QR-Code</strong> im angehängten PDF
                mit Ihrer Banking-App (TWINT, Postfinance, UBS, ZKB etc.) —
                die Zahlungsdaten werden automatisch übernommen.</p>
                <p>Alternativ überweisen Sie manuell auf:<br/>
                IBAN: <code>%s</code><br/>
                Verwendungszweck: <code>%s</code></p>
                <p>Freundliche Grüsse<br/>%s via Sponsorplatz</p>
                """.formatted(
                rechnung.getRechnungsnummer(),
                rechnung.getRechnungsnummer(),
                rechnung.getBetragChf().toPlainString(),
                rechnung.getOrg().getName(),
                rechnung.getFaelligAm().toString(),
                rechnung.getIban(),
                rechnung.getZahlungszweck() != null ? rechnung.getZahlungszweck() : rechnung.getRechnungsnummer(),
                rechnung.getOrg().getName()
        );
    }
}
