package ch.sponsorplatz.shared.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Tests für Phase 10.1 — Monitoring & Observability.
 * <p>
 * MON-01: Health-Endpoint erreichbar mit Liveness/Readiness<br>
 * MON-02: Prometheus-Endpoint liefert Metriken<br>
 * MON-03: TraceIdFilter setzt X-Trace-ID Header (mit Validierung)<br>
 * MON-04: MDC wird nach jedem Request aufgeräumt
 *
 * <p>Wir setzen hier <b>keine</b> management.*-Properties via {@code @SpringBootTest(properties=…)} —
 * die Konfiguration kommt aus {@code application.properties} via aktiviertem Profil
 * {@code dev}, damit Test und Laufzeit-Setup nicht auseinanderlaufen.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("dev")
class MonitoringTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TestRestTemplate restTemplate;

    @LocalServerPort
    private int port;

    @Test
    @DisplayName("MON-01: GET /actuator/health → 200 mit status UP")
    void healthEndpointErreichbar() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }

    @Test
    @DisplayName("MON-01b: GET /actuator/health/liveness → 200")
    void livenessProbeErreichbar() throws Exception {
        mockMvc.perform(get("/actuator/health/liveness"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }

    @Test
    @DisplayName("MON-01c: GET /actuator/health/readiness → 200")
    void readinessProbeErreichbar() throws Exception {
        mockMvc.perform(get("/actuator/health/readiness"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }

    @Test
    @DisplayName("MON-02: GET /actuator/prometheus → 200 mit Metriken-Text")
    void prometheusEndpointLiefertMetriken() {
        ResponseEntity<String> response = restTemplate.getForEntity(
                "http://localhost:" + port + "/actuator/prometheus", String.class);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).containsAnyOf("jvm_memory", "jvm_info", "system_cpu", "process_uptime");
    }

    @Test
    @DisplayName("MON-03: TraceIdFilter setzt X-Trace-ID Response-Header")
    void traceIdFilterSetztHeader() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(header().exists("X-Trace-ID"));
    }

    @Test
    @DisplayName("MON-03b: TraceIdFilter uebernimmt vorhandenen X-Trace-ID Header")
    void traceIdFilterUebernimmtVorhandenenHeader() throws Exception {
        String eigeneTraceId = "mein-test-trace-123";

        mockMvc.perform(get("/actuator/health")
                        .header("X-Trace-ID", eigeneTraceId))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Trace-ID", eigeneTraceId));
    }

    @Test
    @DisplayName("MON-03c: Ungueltiger X-Trace-ID (Sonderzeichen/Overlong) wird verworfen — Log-Injection-Schutz")
    void traceIdFilterVerwirftUngueltigesFormat() throws Exception {
        // Sonderzeichen ausserhalb von [A-Za-z0-9._-] sind nicht erlaubt:
        // Quotes, Spaces, Backticks, Klammern. Tomcat akzeptiert sie auf
        // Header-Ebene (nicht CR/LF wie Newline → das fängt Tomcat selbst
        // mit 400 ab). Unser TraceIdFilter muss zur UUID-Generierung fallback'n.
        String boesartig = "abc\"FAKE LOGIN OK\"";

        mockMvc.perform(get("/actuator/health")
                        .header("X-Trace-ID", boesartig))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Trace-ID",
                        org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("FAKE"))))
                .andExpect(header().string("X-Trace-ID",
                        org.hamcrest.Matchers.matchesPattern("[A-Za-z0-9._-]{1,64}")));
    }

    @Test
    @DisplayName("MON-03d: Overlong X-Trace-ID (>64 Zeichen) wird verworfen")
    void traceIdFilterVerwirftOverlong() throws Exception {
        // 65 Zeichen — knapp ueber dem 64-Zeichen-Limit; Pattern matched nicht
        String overlong = "a".repeat(65);

        mockMvc.perform(get("/actuator/health")
                        .header("X-Trace-ID", overlong))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Trace-ID",
                        org.hamcrest.Matchers.not(org.hamcrest.Matchers.equalTo(overlong))));
    }

    @Test
    @DisplayName("MON-04: MDC wird nach jedem Request aufgeraeumt (kein Bleed in Thread-Pool)")
    void mdcWirdNachRequestGeleert() throws Exception {
        MDC.clear();
        assertThat(MDC.get("traceId")).isNull();

        mockMvc.perform(get("/actuator/health")).andExpect(status().isOk());

        // Test-Thread bekam zu keinem Zeitpunkt eine traceId; Filter laeuft im
        // Servlet-Thread und raeumt dort auf. Verifiziert wird also indirekt:
        // im Test-Thread ist nichts geleakt. Direktbeweis fuers Servlet-Thread-
        // Cleanup ist ohne Filter-Internalspy schwer — die try/finally-
        // Struktur ist garantiert durch JVM-Semantik.
        assertThat(MDC.get("traceId")).isNull();
        assertThat(MDC.get("spanId")).isNull();
        assertThat(MDC.get("method")).isNull();
        assertThat(MDC.get("uri")).isNull();
    }

    // ─── MON-W3C: W3C Trace Context (traceparent) ────────────────────────────

    /** MON-W3C-01: ohne Header bekommt der Response ein gültiges traceparent + X-Trace-ID. */
    @Test
    @DisplayName("MON-W3C-01: Response trägt traceparent (W3C) + X-Trace-ID (Backcompat)")
    void responseTraegtTraceparentUndLegacyHeader() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(header().exists("traceparent"))
                .andExpect(header().exists("X-Trace-ID"))
                .andExpect(header().string("traceparent",
                        org.hamcrest.Matchers.matchesPattern(
                                "^00-[0-9a-f]{32}-[0-9a-f]{16}-[0-9a-f]{2}$")));
    }

    /** MON-W3C-02: W3C-Header hat Vorrang — trace-id wird übernommen. */
    @Test
    @DisplayName("MON-W3C-02: eingehender traceparent → trace-id wird übernommen")
    void traceparentTraceIdWirdUebernommen() throws Exception {
        String incoming = "00-4bf92f3577b34da6a3ce929d0e0e4736-00f067aa0ba902b7-01";
        String erwartetTraceId = "4bf92f3577b34da6a3ce929d0e0e4736";

        mockMvc.perform(get("/actuator/health")
                        .header("traceparent", incoming))
                .andExpect(status().isOk())
                // Outgoing traceparent enthält die übernommene trace-id (neue span-id pro Hop)
                .andExpect(header().string("traceparent",
                        org.hamcrest.Matchers.startsWith("00-" + erwartetTraceId + "-")))
                // X-Trace-ID-Backcompat trägt die gleiche trace-id
                .andExpect(header().string("X-Trace-ID", erwartetTraceId));
    }

    /** MON-W3C-03: ungültiges traceparent (falsche Länge) → fällt auf fresh Generation zurück. */
    @Test
    @DisplayName("MON-W3C-03: ungültiges traceparent wird verworfen, frische trace-id wird generiert")
    void traceparentUngueltigWirdVerworfen() throws Exception {
        mockMvc.perform(get("/actuator/health")
                        .header("traceparent", "00-DEADBEEF-00f067aa0ba902b7-01"))
                .andExpect(status().isOk())
                .andExpect(header().string("traceparent",
                        org.hamcrest.Matchers.matchesPattern(
                                "^00-[0-9a-f]{32}-[0-9a-f]{16}-[0-9a-f]{2}$")))
                .andExpect(header().string("traceparent",
                        org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("DEADBEEF"))));
    }

    /** MON-W3C-04: all-zero trace-id im traceparent ist per Spec ungültig → fresh. */
    @Test
    @DisplayName("MON-W3C-04: all-zero trace-id wird verworfen (W3C-Spec)")
    void allZeroTraceIdWirdVerworfen() throws Exception {
        String allZero = "00-00000000000000000000000000000000-00f067aa0ba902b7-01";

        mockMvc.perform(get("/actuator/health")
                        .header("traceparent", allZero))
                .andExpect(status().isOk())
                .andExpect(header().string("traceparent",
                        org.hamcrest.Matchers.not(
                                org.hamcrest.Matchers.containsString(
                                        "00000000000000000000000000000000"))));
    }
}

