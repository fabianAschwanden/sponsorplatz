package ch.sponsorplatz.admin;

import ch.sponsorplatz.benutzer.SponsorplatzUserDetailsService;
import ch.sponsorplatz.shared.config.ApiKeyFilter;
import ch.sponsorplatz.shared.config.SecurityConfig;
import ch.sponsorplatz.shared.exception.GlobalExceptionHandler;
import ch.sponsorplatz.shared.exception.NotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tests für {@link BacklogApiController} + {@link ApiKeyFilter}.
 * Test-IDs: BAPI-01..08
 */
@WebMvcTest(controllers = BacklogApiController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class, ApiKeyFilter.class})
@ActiveProfiles("dev")
@TestPropertySource(properties = "sponsorplatz.api.key=test-key-123")
class BacklogApiControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private BacklogService backlogService;

    @MockitoBean
    private SponsorplatzUserDetailsService userDetailsService;

    private static final String API_KEY = "test-key-123";

    private BacklogItem testItem() {
        BacklogItem item = new BacklogItem();
        item.setId(UUID.randomUUID());
        item.setTitel("Test Item");
        item.setBeschreibung("Beschreibung");
        item.setStatus(BacklogStatus.OFFEN);
        item.setPrioritaet(BacklogPrioritaet.MITTEL);
        item.setErstelltVon("api");
        return item;
    }

    @Test
    @DisplayName("BAPI-01: GET /api/backlog ohne API-Key → 401")
    void ohneApiKey() throws Exception {
        mockMvc.perform(get("/api/backlog"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("BAPI-02: GET /api/backlog mit falschem Key → 401")
    void falscherKey() throws Exception {
        mockMvc.perform(get("/api/backlog").header("X-API-Key", "wrong"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("BAPI-03: GET /api/backlog mit gültigem Key → 200 + Liste")
    void listeErfolgreich() throws Exception {
        BacklogItem item = testItem();
        when(backlogService.findeAlleSortiert()).thenReturn(List.of(item));

        mockMvc.perform(get("/api/backlog").header("X-API-Key", API_KEY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].titel").value("Test Item"))
                .andExpect(jsonPath("$[0].status").value("OFFEN"));
    }

    @Test
    @DisplayName("BAPI-04: POST /api/backlog erstellt neues Item → 201")
    void erstelleErfolgreich() throws Exception {
        BacklogItem item = testItem();
        when(backlogService.erstelle(eq("Neuer Bug"), any(), any(), eq("api")))
                .thenReturn(item);

        mockMvc.perform(post("/api/backlog")
                        .header("X-API-Key", API_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"titel":"Neuer Bug","beschreibung":"Details","prioritaet":"HOCH"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.titel").value("Test Item"));

        verify(backlogService).erstelle(eq("Neuer Bug"), eq("Details"), eq(BacklogPrioritaet.HOCH), eq("api"));
    }

    @Test
    @DisplayName("BAPI-05: POST /api/backlog ohne Titel → 400")
    void erstelleOhneTitel() throws Exception {
        mockMvc.perform(post("/api/backlog")
                        .header("X-API-Key", API_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"beschreibung":"Nur Beschreibung, kein Titel"}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("BAPI-06: POST /api/backlog nur mit Titel (Defaults) → 201")
    void erstelleNurMitTitel() throws Exception {
        BacklogItem item = testItem();
        when(backlogService.erstelle(eq("Schnellnotiz"), any(), any(), any()))
                .thenReturn(item);

        mockMvc.perform(post("/api/backlog")
                        .header("X-API-Key", API_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"titel":"Schnellnotiz"}
                                """))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("BAPI-07: PATCH /api/backlog/{id}/status ändert Status → 200")
    void statusAendern() throws Exception {
        BacklogItem item = testItem();
        item.setStatus(BacklogStatus.IN_ARBEIT);
        when(backlogService.aendereStatus(any(UUID.class), eq(BacklogStatus.IN_ARBEIT)))
                .thenReturn(item);

        mockMvc.perform(patch("/api/backlog/{id}/status", item.getId())
                        .header("X-API-Key", API_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"status":"IN_ARBEIT"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("IN_ARBEIT"));
    }

    @Test
    @DisplayName("BAPI-08: PATCH nicht existierendes Item → 404")
    void statusAendernNichtGefunden() throws Exception {
        UUID id = UUID.randomUUID();
        when(backlogService.aendereStatus(eq(id), any()))
                .thenThrow(new NotFoundException("Nicht gefunden"));

        mockMvc.perform(patch("/api/backlog/{id}/status", id)
                        .header("X-API-Key", API_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"status":"ERLEDIGT"}
                                """))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("BAPI-09: PATCH ohne status-Feld im Body → 400 (Bean-Validation)")
    void statusAendernOhneStatusFeld() throws Exception {
        UUID id = UUID.randomUUID();

        mockMvc.perform(patch("/api/backlog/{id}/status", id)
                        .header("X-API-Key", API_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }
}

