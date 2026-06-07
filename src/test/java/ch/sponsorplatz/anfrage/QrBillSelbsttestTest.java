package ch.sponsorplatz.anfrage;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests für {@link QrBillSelbsttest} — der Startup-Selbsttest darf den
 * Anwendungsstart NIE abbrechen, egal ob die QR-Generierung gelingt oder
 * (in einer kaputten Umgebung) scheitert.
 *
 * Test-IDs: QRB-SELF-01..02.
 */
class QrBillSelbsttestTest {

    @Test
    @DisplayName("QRB-SELF-01: erfolgreicher Selbsttest wirft nicht")
    void erfolgWirftNicht() {
        QrBillService service = mock(QrBillService.class);
        when(service.erzeuge(any())).thenReturn(new byte[]{1, 2, 3});
        QrBillSelbsttest selbsttest = new QrBillSelbsttest(service);

        assertThatCode(selbsttest::pruefeQrBillRendering).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("QRB-SELF-02: fehlschlagender Selbsttest propagiert NICHT (nur Log)")
    void fehlerPropagiertNicht() {
        QrBillService service = mock(QrBillService.class);
        when(service.erzeuge(any()))
                .thenThrow(new IllegalStateException("QR-Bill-Generierung fehlgeschlagen"));
        QrBillSelbsttest selbsttest = new QrBillSelbsttest(service);

        // Boot darf nicht abbrechen — der Fehler wird nur geloggt.
        assertThatCode(selbsttest::pruefeQrBillRendering).doesNotThrowAnyException();
    }
}
