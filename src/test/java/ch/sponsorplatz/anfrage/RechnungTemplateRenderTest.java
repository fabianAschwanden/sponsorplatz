package ch.sponsorplatz.anfrage;

import ch.sponsorplatz.anfrage.payment.PaymentService;
import ch.sponsorplatz.organisation.AccessControl;
import ch.sponsorplatz.shared.einstellungen.PlattformEinstellungenService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Voll-Kontext-Render-Tests der Rechnungs-Routen (echtes Thymeleaf/PDF-Rendering,
 * nicht nur View-Name wie im WebMvc-Slice).
 *
 * <p>Regression für Beta-Bug BETA-V09: {@code th:if="${rechnung.sponsorEmail}"}
 * nutzte einen String als Boolean-Bedingung → SpringEL „Invalid boolean value:
 * finance@…" → 500 (Detail) bzw. 400/500 (PDF). Diese Tests rendern die echten
 * Templates mit gesetzter Sponsor-E-Mail + QR-Referenz.
 *
 * Test-IDs: RTPL-01..02.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
class RechnungTemplateRenderTest {

    private static final String SLUG = "fc-test";
    private static final UUID RECHNUNG_ID = UUID.randomUUID();

    @Autowired private MockMvc mockMvc;

    @MockitoBean private RechnungService rechnungService;
    @MockitoBean private QrBillService qrBillService;
    @MockitoBean private AccessControl accessControl;
    @MockitoBean private PaymentService paymentService;
    @MockitoBean private PlattformEinstellungenService einstellungenService;

    private RechnungView viewMitEmail() {
        return new RechnungView(
                RECHNUNG_ID, UUID.randomUUID(), "FC Test", SLUG,
                "R-2026-00001", RechnungsStatus.OFFEN, BigDecimal.valueOf(2500),
                "CH93 0076 2011 6238 5295 7", "210000000003139471430009017",
                "CSS Versicherung", "finance@css.ch", "Luzern",
                "Sponsoring", Instant.now(), "admin@t.ch",
                LocalDate.of(2026, 7, 15), null, null);
    }

    private void stubs() {
        when(accessControl.kannOrgEditierenNachSlug(eq(SLUG), any())).thenReturn(true);
        when(rechnungService.findeViewNachId(RECHNUNG_ID)).thenReturn(viewMitEmail());
        when(qrBillService.erzeugeAlsDataUrlFuerId(RECHNUNG_ID)).thenReturn("data:image/png;base64,AAAA");
        when(einstellungenService.istOnlineZahlungAktiv()).thenReturn(false);
    }

    /** RTPL-01: Detail-Route rendert mit Sponsor-E-Mail + QR-Referenz → 200 (kein „Invalid boolean value"). */
    @Test
    @WithUserDetails("dev@sponsorplatz.ch")
    void detailRendertMitStringFeldern() throws Exception {
        stubs();
        mockMvc.perform(get("/organisationen/{slug}/rechnungen/{id}", SLUG, RECHNUNG_ID))
                .andExpect(status().isOk());
    }

    /** RTPL-02: PDF-Route rendert mit Sponsor-E-Mail → 200 application/pdf. */
    @Test
    @WithUserDetails("dev@sponsorplatz.ch")
    void pdfRendertMitSponsorEmail() throws Exception {
        stubs();
        mockMvc.perform(get("/organisationen/{slug}/rechnungen/{id}/pdf", SLUG, RECHNUNG_ID))
                .andExpect(status().isOk());
    }
}
