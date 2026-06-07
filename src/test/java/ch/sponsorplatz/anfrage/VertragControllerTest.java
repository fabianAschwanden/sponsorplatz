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
 * Controller-Tests für {@link VertragController}.
 * Test-IDs: VCTRL-01..08
 */
@WebMvcTest(controllers = VertragController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class})
@ActiveProfiles("dev")
class VertragControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private VertragService vertragService;

    @MockitoBean
    private PdfGeneratorService pdfGenerator;

    @MockitoBean
    private AccessControl accessControl;

    @MockitoBean
    private ch.sponsorplatz.shared.storage.StorageService storageService;

    @MockitoBean
    private SponsorplatzUserDetailsService userDetailsService;

    private static final String SLUG = "fc-test";
    private static final UUID VERTRAG_ID = UUID.randomUUID();
    private static final UUID ANFRAGE_ID = UUID.randomUUID();

    private VertragView testView() {
        return testView(false, null);
    }

    private VertragView testView(boolean mitDokument, String dokumentName) {
        return new VertragView(
                VERTRAG_ID, ANFRAGE_ID, VertragsStatus.ENTWURF,
                "FC Test", SLUG, "Sponsor", "s@t.ch", "Sponsor AG",
                "Gold", "Beschreibung", BigDecimal.valueOf(5000),
                LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31),
                "Logo", "5000 CHF", Instant.now(), "admin@t.ch", null, null,
                mitDokument, dokumentName, mitDokument ? 1234L : null,
                mitDokument ? Instant.now() : null, mitDokument ? "admin@t.ch" : null
        );
    }

    @Test
    @WithMockUser
    @DisplayName("VCTRL-01: POST erstellen → Redirect auf Vertrag-Detail")
    void erstellenRedirects() throws Exception {
        when(accessControl.kannOrgEditierenNachSlug(eq(SLUG), any())).thenReturn(true);
        when(vertragService.erstelleAlsView(eq(ANFRAGE_ID), any())).thenReturn(testView());

        mockMvc.perform(post("/organisationen/{slug}/anfragen/{anfrageId}/vertrag/erstellen",
                        SLUG, ANFRAGE_ID).with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/organisationen/" + SLUG + "/vertraege/" + VERTRAG_ID));
    }

    @Test
    @WithMockUser
    @DisplayName("VCTRL-02: GET detail zeigt Vertrag-View im Model")
    void detailZeigtVertrag() throws Exception {
        when(accessControl.kannOrgEditierenNachSlug(eq(SLUG), any())).thenReturn(true);
        when(vertragService.findeViewNachId(VERTRAG_ID)).thenReturn(testView());
        VertragFormDto form = new VertragFormDto();
        form.setPreisChf(BigDecimal.valueOf(5000));
        when(vertragService.findeFormularNachId(VERTRAG_ID)).thenReturn(form);

        mockMvc.perform(get("/organisationen/{slug}/vertraege/{id}", SLUG, VERTRAG_ID))
                .andExpect(status().isOk())
                .andExpect(model().attributeExists("vertrag", "form"))
                .andExpect(view().name("anfrage/vertrag-detail"));
    }

    @Test
    @WithMockUser
    @DisplayName("VCTRL-03: POST speichern → Redirect mit Erfolgsmeldung")
    void speichernErfolgreich() throws Exception {
        when(accessControl.kannOrgEditierenNachSlug(eq(SLUG), any())).thenReturn(true);

        mockMvc.perform(post("/organisationen/{slug}/vertraege/{id}", SLUG, VERTRAG_ID)
                        .param("preisChf", "5000")
                        .param("leistungVerein", "Logo")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attributeExists("erfolgsMeldung"));

        verify(vertragService).aktualisiereAusForm(eq(VERTRAG_ID), any());
    }

    @Test
    @WithMockUser
    @DisplayName("VCTRL-04: POST unterzeichnen ohne Owner-Recht → 403")
    void unterzeichnenOhneOwner() throws Exception {
        when(accessControl.kannOrgVerwaltenNachSlug(eq(SLUG), any())).thenReturn(false);

        mockMvc.perform(post("/organisationen/{slug}/vertraege/{id}/unterzeichnen",
                        SLUG, VERTRAG_ID).with(csrf()))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "owner@test.ch")
    @DisplayName("VCTRL-05: POST unterzeichnen mit Owner-Recht → Redirect + Username im Audit")
    void unterzeichnenMitOwner() throws Exception {
        when(accessControl.kannOrgVerwaltenNachSlug(eq(SLUG), any())).thenReturn(true);

        mockMvc.perform(post("/organisationen/{slug}/vertraege/{id}/unterzeichnen",
                        SLUG, VERTRAG_ID).with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attributeExists("erfolgsMeldung"));

        verify(vertragService).markiereUnterzeichnet(VERTRAG_ID, "owner@test.ch");
    }

    @Test
    @WithMockUser
    @DisplayName("VCTRL-06: GET pdf liefert application/pdf")
    void pdfDownload() throws Exception {
        when(accessControl.kannOrgEditierenNachSlug(eq(SLUG), any())).thenReturn(true);
        when(vertragService.findeViewNachId(VERTRAG_ID)).thenReturn(testView());
        when(pdfGenerator.erzeuge(any(), any(), any())).thenReturn("PDF-CONTENT".getBytes());

        mockMvc.perform(get("/organisationen/{slug}/vertraege/{id}/pdf", SLUG, VERTRAG_ID))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/pdf"))
                .andExpect(header().exists("Content-Disposition"));
    }

    @Test
    @WithMockUser
    @DisplayName("VCTRL-07: POST erstellen ohne Edit-Recht → 403")
    void erstellenOhneRecht() throws Exception {
        when(accessControl.kannOrgEditierenNachSlug(eq(SLUG), any())).thenReturn(false);

        mockMvc.perform(post("/organisationen/{slug}/anfragen/{anfrageId}/vertrag/erstellen",
                        SLUG, ANFRAGE_ID).with(csrf()))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser
    @DisplayName("VCTRL-08: POST speichern ohne Edit-Recht → 403")
    void speichernOhneRecht() throws Exception {
        when(accessControl.kannOrgEditierenNachSlug(eq(SLUG), any())).thenReturn(false);

        mockMvc.perform(post("/organisationen/{slug}/vertraege/{id}", SLUG, VERTRAG_ID)
                        .param("preisChf", "5000")
                        .with(csrf()))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "admin@t.ch")
    @DisplayName("VCTRL-09: POST dokument lädt PDF hoch → Redirect + Service-Aufruf")
    void dokumentHochladen() throws Exception {
        when(accessControl.kannOrgEditierenNachSlug(eq(SLUG), any())).thenReturn(true);
        when(vertragService.findeViewNachId(VERTRAG_ID)).thenReturn(testView());
        var pdf = new org.springframework.mock.web.MockMultipartFile(
                "dokument", "vertrag.pdf", "application/pdf", "PDF".getBytes());

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .multipart("/organisationen/{slug}/vertraege/{id}/dokument", SLUG, VERTRAG_ID)
                        .file(pdf).with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attributeExists("erfolgsMeldung"));

        verify(vertragService).speichereDokument(eq(VERTRAG_ID), any(), eq("admin@t.ch"));
    }

    @Test
    @WithMockUser
    @DisplayName("VCTRL-10: POST dokument ohne Edit-Recht → 403")
    void dokumentHochladenOhneRecht() throws Exception {
        when(accessControl.kannOrgEditierenNachSlug(eq(SLUG), any())).thenReturn(false);
        var pdf = new org.springframework.mock.web.MockMultipartFile(
                "dokument", "vertrag.pdf", "application/pdf", "PDF".getBytes());

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .multipart("/organisationen/{slug}/vertraege/{id}/dokument", SLUG, VERTRAG_ID)
                        .file(pdf).with(csrf()))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser
    @DisplayName("VCTRL-11: GET pdf liefert hochgeladenes Dokument (Vorrang vor generiertem)")
    void pdfLiefertHochgeladenesDokument() throws Exception {
        when(accessControl.kannOrgEditierenNachSlug(eq(SLUG), any())).thenReturn(true);
        when(vertragService.findeViewNachId(VERTRAG_ID)).thenReturn(testView(true, "extern-unterschrieben.pdf"));
        when(vertragService.findeDokumentSnapshot(VERTRAG_ID)).thenReturn(
                new VertragService.DokumentSnapshot("vertraege/x/dokument.pdf",
                        "application/pdf", "extern-unterschrieben.pdf"));
        when(storageService.ladeAlsResource("vertraege/x/dokument.pdf"))
                .thenReturn(new org.springframework.core.io.ByteArrayResource("HOCHGELADEN".getBytes()));

        mockMvc.perform(get("/organisationen/{slug}/vertraege/{id}/pdf", SLUG, VERTRAG_ID))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/pdf"))
                .andExpect(header().string("Content-Disposition",
                        org.hamcrest.Matchers.containsString("extern-unterschrieben.pdf")));

        // generiertes PDF darf NICHT erzeugt werden, wenn ein Dokument vorliegt
        verify(pdfGenerator, org.mockito.Mockito.never()).erzeuge(any(), any(), any());
    }

    @Test
    @WithMockUser(username = "admin@t.ch")
    @DisplayName("VCTRL-12: POST dokument/entfernen → Redirect + Service-Aufruf")
    void dokumentEntfernen() throws Exception {
        when(accessControl.kannOrgEditierenNachSlug(eq(SLUG), any())).thenReturn(true);
        when(vertragService.findeViewNachId(VERTRAG_ID)).thenReturn(testView(true, "vertrag.pdf"));

        mockMvc.perform(post("/organisationen/{slug}/vertraege/{id}/dokument/entfernen", SLUG, VERTRAG_ID)
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attributeExists("erfolgsMeldung"));

        verify(vertragService).entferneDokument(VERTRAG_ID, "admin@t.ch");
    }
}

