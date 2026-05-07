package ch.sponsorplatz.service;
import ch.sponsorplatz.organisation.Organisation;

import ch.sponsorplatz.model.Rechnung;
import net.codecrete.qrbill.canvas.PNGCanvas;
import net.codecrete.qrbill.generator.Address;
import net.codecrete.qrbill.generator.Bill;
import net.codecrete.qrbill.generator.BillFormat;
import net.codecrete.qrbill.generator.GraphicsFormat;
import net.codecrete.qrbill.generator.OutputSize;
import net.codecrete.qrbill.generator.Payments;
import net.codecrete.qrbill.generator.QRBill;

import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Base64;

/**
 * Generiert den Swiss-QR-Bill als PNG-Bild für die Einbettung ins Rechnungs-PDF.
 *
 * <p>Verwendet {@code net.codecrete.qrbill}. Output ist {@link OutputSize#QR_BILL_ONLY}
 * (105 mm × 210 mm) als PNG mit 200 dpi — passt unter die letzten 105 mm einer
 * A4-Seite, wie von Six Group spezifiziert.
 *
 * <p>QR-IBAN-Detection: bei IBANs in der Reichweite des Schweizer QR-IBAN-
 * Bereichs (Institut-ID 30000–31999) wird automatisch eine QR-Referenz
 * verwendet, sonst Creditor-Reference (ISO 11649). Wenn der Verein keinen
 * QR-IBAN hat, wird {@code referenceNumber} leer gelassen → Sponsor zahlt mit
 * "Mitteilung" statt strukturierter Referenz.
 */
@Service
public class QrBillService {

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
        } catch (IOException e) {
            throw new RuntimeException("QR-Bill-Generierung fehlgeschlagen: " + e.getMessage(), e);
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
