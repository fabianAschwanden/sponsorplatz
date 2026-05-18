package ch.sponsorplatz.admin;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

/**
 * REST-API für Backlog-Items — ermöglicht das Anlegen und Verwalten
 * von Backlog-Einträgen aus der IDE, CLI oder einem MCP-Server.
 *
 * <p>Authentifizierung via {@code X-API-Key}-Header (siehe
 * {@link ch.sponsorplatz.shared.config.ApiKeyFilter}).
 *
 * <h3>Beispiel (curl):</h3>
 * <pre>{@code
 * curl -X POST http://localhost:8080/api/backlog \
 *   -H "Content-Type: application/json" \
 *   -H "X-API-Key: mein-geheimer-schluessel" \
 *   -d '{"titel":"N+1 Query in ProjektService fixen","prioritaet":"HOCH"}'
 * }</pre>
 */
@RestController
@RequestMapping("/api/backlog")
public class BacklogApiController {

    private final BacklogService backlogService;

    public BacklogApiController(BacklogService backlogService) {
        this.backlogService = backlogService;
    }

    /**
     * Alle Backlog-Items (sortiert nach Priorität/Status).
     */
    @GetMapping
    public List<BacklogItemView> liste() {
        return BacklogItemView.von(backlogService.findeAlleSortiert());
    }

    /**
     * Neues Backlog-Item anlegen.
     *
     * <pre>
     * POST /api/backlog
     * {
     *   "titel": "Bug: Login-Redirect",
     *   "beschreibung": "Nach Login wird auf /error statt /dashboard geleitet",
     *   "prioritaet": "HOCH"   // optional, Default: MITTEL
     * }
     * </pre>
     */
    @PostMapping
    public ResponseEntity<BacklogItemView> erstelle(@Valid @RequestBody BacklogApiRequest request) {
        BacklogItem item = backlogService.erstelle(
                request.titel(),
                request.beschreibung(),
                request.prioritaet(),
                "api"
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(BacklogItemView.von(item));
    }

    /**
     * Status eines Backlog-Items ändern.
     *
     * <pre>
     * PATCH /api/backlog/{id}/status
     * { "status": "IN_ARBEIT" }
     * </pre>
     */
    @PatchMapping("/{id}/status")
    public BacklogItemView aendereStatus(@PathVariable UUID id,
                                         @RequestBody BacklogStatusRequest request) {
        BacklogItem item = backlogService.aendereStatus(id, request.status());
        return BacklogItemView.von(item);
    }

    /**
     * Request-DTO für POST /api/backlog.
     */
    public record BacklogApiRequest(
            @jakarta.validation.constraints.NotBlank(message = "Titel ist Pflichtfeld")
            @jakarta.validation.constraints.Size(max = 200)
            String titel,
            String beschreibung,
            BacklogPrioritaet prioritaet
    ) {}

    /**
     * Request-DTO für PATCH /api/backlog/{id}/status.
     */
    public record BacklogStatusRequest(
            @jakarta.validation.constraints.NotNull(message = "Status ist Pflichtfeld")
            BacklogStatus status
    ) {}
}

