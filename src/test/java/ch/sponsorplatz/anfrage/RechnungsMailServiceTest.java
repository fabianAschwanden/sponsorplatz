package ch.sponsorplatz.anfrage;

import ch.sponsorplatz.organisation.Organisation;
import ch.sponsorplatz.shared.mail.MailService;
import ch.sponsorplatz.shared.pdf.PdfGeneratorService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests für {@link RechnungsMailService}.
 * Test-IDs: RMAIL-01..04
 */
@ExtendWith(MockitoExtension.class)
class RechnungsMailServiceTest {

    @Mock private MailService mailService;
    @Mock private PdfGeneratorService pdfGenerator;
    @Mock private QrBillService qrBillService;
    @Mock private RechnungRepository rechnungRepository;

    private RechnungsMailService service;

    @BeforeEach
    void setup() {
        service = new RechnungsMailService(mailService, pdfGenerator, qrBillService, rechnungRepository);
    }

    private Rechnung testRechnung(String sponsorEmail) {
        Organisation org = new Organisation();
        org.setId(UUID.randomUUID());
        org.setName("FC Test");
        org.setSlug("fc-test");

        Rechnung r = new Rechnung();
        r.setId(UUID.randomUUID());
        r.setOrg(org);
        r.setRechnungsnummer("R-2026-00001");
        r.setStatus(RechnungsStatus.OFFEN);
        r.setBetragChf(BigDecimal.valueOf(2500));
        r.setIban("CH93 0076 2011 6238 5295 7");
        r.setSponsorName("Acme AG");
        r.setSponsorEmail(sponsorEmail);
        r.setZahlungszweck("Gold-Paket · R-2026-00001");
        r.setFaelligAm(LocalDate.of(2026, 7, 15));
        return r;
    }

    @Test
    @DisplayName("RMAIL-01: sendeRechnungPerEmail lädt per ID und sendet HTML-Mail mit PDF")
    void sendetMailMitPdf() {
        Rechnung r = testRechnung("finance@acme.ch");
        when(rechnungRepository.findById(r.getId())).thenReturn(Optional.of(r));
        when(pdfGenerator.erzeuge(any(), any(), any())).thenReturn("FAKEPDF".getBytes());
        lenient().when(qrBillService.erzeugeAlsDataUrl(r)).thenReturn("data:image/png;base64,ABC");

        service.sendeRechnungPerEmail(r.getId());

        verify(mailService).sendeHtml(
                eq("finance@acme.ch"),
                contains("R-2026-00001"),
                any());
    }

    @Test
    @DisplayName("RMAIL-02: Ohne Sponsor-E-Mail wird keine Mail gesendet")
    void ohneSponsorEmailKeinVersand() {
        Rechnung r = testRechnung(null);
        when(rechnungRepository.findById(r.getId())).thenReturn(Optional.of(r));

        service.sendeRechnungPerEmail(r.getId());

        verify(mailService, never()).sendeHtml(any(), any(), any());
    }

    @Test
    @DisplayName("RMAIL-03: Leere Sponsor-E-Mail wird ebenfalls übersprungen")
    void leereSponsorEmailKeinVersand() {
        Rechnung r = testRechnung("   ");
        when(rechnungRepository.findById(r.getId())).thenReturn(Optional.of(r));

        service.sendeRechnungPerEmail(r.getId());

        verify(mailService, never()).sendeHtml(any(), any(), any());
    }

    @Test
    @DisplayName("RMAIL-04: Unbekannte Rechnungs-ID → kein Versand, kein Crash")
    void unbekannteIdKeinVersand() {
        UUID id = UUID.randomUUID();
        when(rechnungRepository.findById(id)).thenReturn(Optional.empty());

        service.sendeRechnungPerEmail(id);

        verify(mailService, never()).sendeHtml(any(), any(), any());
    }
}
