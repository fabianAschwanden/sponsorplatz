package ch.sponsorplatz.anfrage;

import ch.sponsorplatz.benutzer.SponsorplatzUserDetailsService;
import ch.sponsorplatz.organisation.AccessControl;
import ch.sponsorplatz.shared.config.SecurityConfig;
import ch.sponsorplatz.shared.exception.GlobalExceptionHandler;
import ch.sponsorplatz.shared.pdf.PdfGeneratorService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Controller-Tests für {@link RechnungController}.
 * Test-IDs: RCTRL-01..09
 */
@WebMvcTest(controllers = RechnungController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class})
@ActiveProfiles("dev")
class RechnungControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private RechnungService rechnungService;

    @MockitoBean
    private QrBillService qrBillService;

    @MockitoBean
    private PdfGeneratorService pdfGenerator;

    @MockitoBean
    private AccessControl accessControl;

    @MockitoBean
    private SponsorplatzUserDetailsService userDetailsService;

    private static final String SLUG = "fc-test";
    private static final UUID RECHNUNG_ID = UUID.randomUUID();
    private static final UUID VERTRAG_ID = UUID.randomUUID();

    private RechnungView testView() {
        return new RechnungView(
                RECHNUNG_ID, VERTRAG_ID, "FC Test", SLUG,
                "R-2026-00001", RechnungsStatus.OFFEN, BigDecimal.valueOf(2500),
                "CH93 0076 2011 6238 5295 7", "210000000003139471430009017",
                "CSS Versicherung", "finance@css.ch", "Luzern",
                "Sponsoring", Instant.now(), "admin@t.ch",
                LocalDate.of(2026, 7, 15), null, null
        );
    }

    @Test
    @WithMockUser
    @DisplayName("RCTRL-01: POST erstellen → Redirect auf Rechnung-Detail")
    void erstellenRedirects() throws Exception {
        when(accessControl.kannOrgEditierenNachSlug(eq(SLUG), any())).thenReturn(true);
        RechnungView view = testView();
        when(rechnungService.erstelleAlsView(eq(VERTRAG_ID), any())).thenReturn(view);

        mockMvc.perform(post("/organisationen/{slug}/vertraege/{vertragId}/rechnung/erstellen",
                        SLUG, VERTRAG_ID).with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/organisationen/" + SLUG + "/rechnungen/" + RECHNUNG_ID));
    }

    @Test
    @WithMockUser
    @DisplayName("RCTRL-02: POST erstellen bei IllegalStateException → Redirect mit Fehlermeldung")
    void erstellenBeiFehler() throws Exception {
        when(accessControl.kannOrgEditierenNachSlug(eq(SLUG), any())).thenReturn(true);
        when(rechnungService.erstelleAlsView(eq(VERTRAG_ID), any()))
                .thenThrow(new IllegalStateException("Vertrag nicht unterzeichnet"));

        mockMvc.perform(post("/organisationen/{slug}/vertraege/{vertragId}/rechnung/erstellen",
                        SLUG, VERTRAG_ID).with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attributeExists("fehlermeldung"));
    }

    @Test
    @WithMockUser
    @DisplayName("RCTRL-03: GET detail zeigt Rechnung im Model")
    void detailZeigtRechnung() throws Exception {
        when(accessControl.kannOrgEditierenNachSlug(eq(SLUG), any())).thenReturn(true);
        when(rechnungService.findeViewNachId(RECHNUNG_ID)).thenReturn(testView());
        when(qrBillService.erzeugeAlsDataUrlFuerId(RECHNUNG_ID)).thenReturn("data:image/png;base64,...");

        mockMvc.perform(get("/organisationen/{slug}/rechnungen/{id}", SLUG, RECHNUNG_ID))
                .andExpect(status().isOk())
                .andExpect(model().attributeExists("rechnung", "qrBildDataUrl"))
                .andExpect(view().name("anfrage/rechnung-detail"));
    }

    @Test
    @WithMockUser
    @DisplayName("RCTRL-04: POST bezahlt → Redirect mit Erfolgsmeldung")
    void bezahltMarkieren() throws Exception {
        when(rechnungService.findeViewNachId(RECHNUNG_ID)).thenReturn(testView());
        when(accessControl.kannOrgEditierenNachSlug(eq(SLUG), any())).thenReturn(true);

        mockMvc.perform(post("/organisationen/{slug}/rechnungen/{id}/bezahlt",
                        SLUG, RECHNUNG_ID).with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attributeExists("erfolgsMeldung"));

        verify(rechnungService).markiereBezahlt(eq(RECHNUNG_ID), any());
    }

    @Test
    @WithMockUser
    @DisplayName("RCTRL-05: POST stornieren → Redirect mit Erfolgsmeldung")
    void stornieren() throws Exception {
        when(rechnungService.findeViewNachId(RECHNUNG_ID)).thenReturn(testView());
        when(accessControl.kannOrgEditierenNachSlug(eq(SLUG), any())).thenReturn(true);

        mockMvc.perform(post("/organisationen/{slug}/rechnungen/{id}/stornieren",
                        SLUG, RECHNUNG_ID)
                        .param("grund", "Irrtum")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attributeExists("erfolgsMeldung"));

        verify(rechnungService).stornieren(RECHNUNG_ID, "Irrtum");
    }

    @Test
    @WithMockUser
    @DisplayName("RCTRL-06: GET pdf liefert application/pdf")
    void pdfDownload() throws Exception {
        when(accessControl.kannOrgEditierenNachSlug(eq(SLUG), any())).thenReturn(true);
        when(rechnungService.findeViewNachId(RECHNUNG_ID)).thenReturn(testView());
        when(qrBillService.erzeugeAlsDataUrlFuerId(RECHNUNG_ID)).thenReturn("data:...");
        when(pdfGenerator.erzeuge(any(), any(), any())).thenReturn("PDF".getBytes());

        mockMvc.perform(get("/organisationen/{slug}/rechnungen/{id}/pdf", SLUG, RECHNUNG_ID))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/pdf"))
                .andExpect(header().exists("Content-Disposition"));
    }

    @Test
    @WithMockUser
    @DisplayName("RCTRL-07: POST erstellen ohne Edit-Recht → 403")
    void erstellenOhneRecht() throws Exception {
        when(accessControl.kannOrgEditierenNachSlug(eq(SLUG), any())).thenReturn(false);

        mockMvc.perform(post("/organisationen/{slug}/vertraege/{vertragId}/rechnung/erstellen",
                        SLUG, VERTRAG_ID).with(csrf()))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser
    @DisplayName("RCTRL-08: POST bezahlt ohne Edit-Recht → 403")
    void bezahltOhneRecht() throws Exception {
        when(rechnungService.findeViewNachId(RECHNUNG_ID)).thenReturn(testView());
        when(accessControl.kannOrgEditierenNachSlug(eq(SLUG), any())).thenReturn(false);

        mockMvc.perform(post("/organisationen/{slug}/rechnungen/{id}/bezahlt",
                        SLUG, RECHNUNG_ID).with(csrf()))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser
    @DisplayName("RCTRL-09: POST stornieren ohne Edit-Recht → 403")
    void stornierenOhneRecht() throws Exception {
        when(rechnungService.findeViewNachId(RECHNUNG_ID)).thenReturn(testView());
        when(accessControl.kannOrgEditierenNachSlug(eq(SLUG), any())).thenReturn(false);

        mockMvc.perform(post("/organisationen/{slug}/rechnungen/{id}/stornieren",
                        SLUG, RECHNUNG_ID)
                        .param("grund", "Irrtum")
                        .with(csrf()))
                .andExpect(status().isForbidden());
    }
}

