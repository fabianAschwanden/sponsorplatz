package ch.sponsorplatz.anfrage;

import java.math.BigDecimal;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import ch.sponsorplatz.organisation.Organisation;

/**
 * Diagnose: führt beim Anwendungsstart einmal eine QR-Bill-Generierung mit
 * Dummy-Daten aus und loggt das Ergebnis. So zeigt der Startup-Log sofort und
 * unabhängig von einem konkreten Request, ob das Java2D/Font-Subsystem in der
 * Laufzeitumgebung (Docker-Container) funktioniert.
 *
 * <p>Hintergrund BETA-V09: Die Rechnungs-Detail- und PDF-Routen crashten auf
 * Staging mit 500, während die QR-freie Vertrags-PDF sauber rendert — ein
 * starkes Indiz für ein umgebungsspezifisches Grafik-/Font-Problem. Dieser
 * Selbsttest macht die exakte Ursache (Throwable-Klasse + Stacktrace) im
 * Boot-Log sichtbar, statt auf einen Tester-Request angewiesen zu sein.
 *
 * <p>Rein lesend, kein DB-Zugriff, keine Persistenz — nutzt {@link QrBillService}
 * direkt mit einer In-Memory-Dummy-Rechnung.
 */
@Component
public class QrBillSelbsttest {

    private static final Logger log = LoggerFactory.getLogger(QrBillSelbsttest.class);

    private final QrBillService qrBillService;

    public QrBillSelbsttest(QrBillService qrBillService) {
        this.qrBillService = qrBillService;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void pruefeQrBillRendering() {
        try {
            byte[] png = qrBillService.erzeuge(dummyRechnung());
            if (png == null) {
                // Gemockter Service (z.B. in @SpringBootTest-Slices) liefert null —
                // kein echter Render-Lauf, also nichts zu bewerten.
                return;
            }
            log.info("QR-Bill-Selbsttest OK — PNG erzeugt ({} Bytes). "
                    + "Java2D/Font-Subsystem funktioniert in dieser Umgebung.", png.length);
        } catch (RuntimeException e) {
            // erzeuge() mappt Umgebungsfehler bereits auf IllegalStateException —
            // wir loggen hier nochmals explizit als Boot-Marker mit voller Cause.
            log.error("QR-Bill-Selbsttest FEHLGESCHLAGEN — QR-Routen (Rechnungs-Detail/-PDF) "
                    + "werden in dieser Umgebung 500/409 liefern. Ursache: {}",
                    e.getMessage(), e);
        }
    }

    private static Rechnung dummyRechnung() {
        Organisation org = new Organisation();
        org.setId(UUID.randomUUID());
        org.setName("Selbsttest Verein");
        org.setStrasse("Bahnhofstrasse 1");
        org.setPostleitzahl("8001");
        org.setOrt("Zürich");

        Rechnung r = new Rechnung();
        r.setId(UUID.randomUUID());
        r.setOrg(org);
        r.setIban("CH4431999123000889012"); // gültige QR-IBAN
        r.setQrReferenz("210000000003139471430009017");
        r.setBetragChf(new BigDecimal("100.00"));
        r.setSponsorName("Selbsttest Sponsor");
        r.setZahlungszweck("Selbsttest");
        r.setRechnungsnummer("R-0000-00000");
        return r;
    }
}
