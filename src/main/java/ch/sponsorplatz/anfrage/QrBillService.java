package ch.sponsorplatz.anfrage;

import java.io.IOException;
import java.util.Base64;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import net.codecrete.qrbill.canvas.PNGCanvas;
import net.codecrete.qrbill.generator.Address;
import net.codecrete.qrbill.generator.Bill;
import net.codecrete.qrbill.generator.BillFormat;
import net.codecrete.qrbill.generator.GraphicsFormat;
import net.codecrete.qrbill.generator.OutputSize;
import net.codecrete.qrbill.generator.Payments;
import net.codecrete.qrbill.generator.QRBill;
import net.codecrete.qrbill.generator.QRBillValidationError;

/**
 * Generiert den Swiss-QR-Bill als PNG-Bild für die Einbettung ins
 * Rechnungs-PDF.
 *
 * <p>
 * Verwendet {@code net.codecrete.qrbill}. Output ist
 * {@link OutputSize#QR_BILL_ONLY}
 * (105 mm × 210 mm) als PNG mit 200 dpi — passt unter die letzten 105 mm einer
 * A4-Seite, wie von Six Group spezifiziert.
 *
 * <p>
 * QR-IBAN-Detection: bei IBANs in der Reichweite des Schweizer QR-IBAN-
 * Bereichs (Institut-ID 30000–31999) wird automatisch eine QR-Referenz
 * verwendet, sonst Creditor-Reference (ISO 11649). Wenn der Verein keinen
 * QR-IBAN hat, wird {@code referenceNumber} leer gelassen → Sponsor zahlt mit
 * "Mitteilung" statt strukturierter Referenz.
 */
@Service
public class QrBillService {

    private static final Logger log = LoggerFactory.getLogger(QrBillService.class);

    private final RechnungRepository rechnungRepository;

    public QrBillService(RechnungRepository rechnungRepository) {
        this.rechnungRepository = rechnungRepository;
    }

    /**
     * Controller-freundlicher Eingang: lädt die Rechnung selbst und baut den
     * Data-URL, damit der Aufrufer keine Entity halten muss (ARCH-02).
     *
     * <p><strong>{@code @Transactional(readOnly = true)} ist zwingend:</strong>
     * {@link #erzeuge(Rechnung)} greift auf die LAZY-Relation {@code Rechnung.org}
     * zu (Name + Adresse für die QR-Bill). Mit {@code spring.jpa.open-in-view=false}
     * (prod-Härtung) ist die Session nach dem Repository-Call sonst geschlossen →
     * {@code LazyInitializationException} → 500 auf Rechnungs-Detail UND -PDF
     * (Beta-Bug BETA-V09). Die offene Transaktion hält die Session bis zur
     * Org-Auflösung offen.
     */
    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public String erzeugeAlsDataUrlFuerId(java.util.UUID rechnungId) {
        Rechnung r = rechnungRepository.findById(rechnungId)
                .orElseThrow(() -> new ch.sponsorplatz.shared.exception.NotFoundException(
                        "Rechnung nicht gefunden: " + rechnungId));
        return erzeugeAlsDataUrl(r);
    }

    /**
     * Generiert das QR-Bill-PNG (Base64-Data-URL für direkte Einbettung in
     * HTML/PDF-Templates via {@code <img src="data:image/png;base64,…">}).
     */
    public String erzeugeAlsDataUrl(Rechnung rechnung) {
        byte[] png = erzeuge(rechnung);
        return "data:image/png;base64," + Base64.getEncoder().encodeToString(png);
    }

    /**
     * Generiert das QR-Bill-PNG als Bytes.
     */
    public byte[] erzeuge(Rechnung rechnung) {
        Bill bill = new Bill();
        bill.setAccount(normalisiereIban(rechnung.getIban()));
        bill.setAmountFromDouble(rechnung.getBetragChf().doubleValue());
        bill.setCurrency("CHF");

        // Empfänger (Verein) — Six-Group-Spec verlangt vollständige Adresse.
        // Strasse + PLZ + Ort kommen aus Organisation; ohne sie wirft die
        // Library QRBillValidationError beim Rendern.
        Address creditor = new Address();
        var org = rechnung.getOrg();
        creditor.setName(org != null ? org.getName() : "Verein");
        creditor.setCountryCode("CH");
        if (org != null) {
            if (org.getStrasse() != null && !org.getStrasse().isBlank()) {
                creditor.setStreet(org.getStrasse());
            }
            if (org.getPostleitzahl() != null && !org.getPostleitzahl().isBlank()) {
                creditor.setPostalCode(org.getPostleitzahl());
            }
            if (org.getOrt() != null && !org.getOrt().isBlank()) {
                creditor.setTown(org.getOrt());
            }
        }
        bill.setCreditor(creditor);

        // QR-Referenz: nur valide für QR-IBANs (Institut-ID 30000-31999),
        // sonst leer + Mitteilung.
        if (rechnung.getQrReferenz() != null && !rechnung.getQrReferenz().isBlank()
                && Payments.isValidQRReference(rechnung.getQrReferenz())) {
            bill.setReference(rechnung.getQrReferenz());
        }

        if (rechnung.getZahlungszweck() != null && !rechnung.getZahlungszweck().isBlank()) {
            bill.setUnstructuredMessage(rechnung.getZahlungszweck());
        }

        // Schuldner (Sponsor) bewusst optional — wenn Sponsor-Adresse zentral
        // erfasst wird (Org-Profil), kann das später als structured Address
        // (street/postalCode/town) ergänzt werden. Aktuell setzen wir nur den
        // Namen nicht — die QR-Bill rendert dann ein leeres "Zahlbar durch"-
        // Feld, das der Sponsor von Hand ausfüllen kann (Standard-Workflow
        // bei vielen Schweizer Vereins-Rechnungen).

        BillFormat format = bill.getFormat();
        format.setOutputSize(OutputSize.QR_BILL_ONLY);
        format.setLanguage(net.codecrete.qrbill.generator.Language.DE);
        format.setGraphicsFormat(GraphicsFormat.PNG);

        try (PNGCanvas canvas = new PNGCanvas(210, 105, 200, "Helvetica")) {
            QRBill.draw(bill, canvas);
            return canvas.toByteArray();
        } catch (QRBillValidationError e) {
            // Ungültige Rechnungsdaten (z.B. fehlerhafte IBAN, unvollständige
            // Empfänger-Adresse) → 400 statt unbehandeltem 500. Sonst crasht die
            // Rechnungs-Detail- bzw. PDF-Route an fehlerhaften Stammdaten.
            throw new IllegalArgumentException(
                    "QR-Bill kann nicht erzeugt werden — Rechnungsdaten ungültig: "
                            + e.getMessage(), e);
        } catch (IOException | RuntimeException e) {
            // Umgebungs-/Rendering-Fehler (z.B. fehlende Fonts/fontconfig im
            // Runtime-Container → Java2D-Text-Rendering wirft InternalError o.ä.,
            // verpackt als RuntimeException). Wir lassen das NICHT als rohen 500
            // durch, sondern als IllegalStateException (→ 409) mit klarer Meldung,
            // damit die Rechnungs-Detail-/PDF-Route nicht generisch crasht.
            log.error("QR-Bill-Generierung fehlgeschlagen (Rendering/Umgebung) — "
                    + "Throwable={}, msg={}, iban={}, qrRef={}",
                    e.getClass().getName(), e.getMessage(),
                    rechnung.getIban(), rechnung.getQrReferenz(), e);
            throw new IllegalStateException(
                    "QR-Bill-Generierung fehlgeschlagen (Rendering-/Umgebungsfehler): "
                            + e.getMessage(), e);
        } catch (Error e) {
            // VirtualMachineError (OOM/StackOverflow) NICHT verschlucken — das
            // muss die JVM/den Request hart beenden. Alles andere (InternalError,
            // NoClassDefFoundError, UnsatisfiedLinkError aus dem AWT/Font-Subsystem
            // bei fehlendem fontconfig) fangen wir ab, loggen den vollen Stacktrace
            // und mappen auf 409 statt eines nackten 500.
            if (e instanceof VirtualMachineError) {
                throw e;
            }
            log.error("QR-Bill-Generierung fehlgeschlagen (Grafik-/Font-Subsystem) — "
                    + "Error={}, msg={}", e.getClass().getName(), e.getMessage(), e);
            throw new IllegalStateException(
                    "QR-Bill-Generierung fehlgeschlagen (Grafik-/Font-Subsystem nicht verfügbar): "
                            + e, e);
        }
    }

    /**
     * IBAN ohne Leerzeichen (Library erwartet kompakte Form).
     */
    private static String normalisiereIban(String iban) {
        if (iban == null) {
            throw new IllegalArgumentException("Kein IBAN auf der Rechnung gesetzt");
        }
        return iban.replace(" ", "").toUpperCase();
    }
}
