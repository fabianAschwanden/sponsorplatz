package ch.sponsorplatz.shared.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Map;

/**
 * API-Key-Authentifizierung für {@code /api/**}-Routen.
 *
 * <p>Prüft den {@code X-API-Key}-Header gegen den in
 * {@code sponsorplatz.api.key} konfigurierten Schlüssel.
 * Ohne konfigurierten Key (leer/null) sind API-Routen gesperrt (503).
 *
 * <p>Sicherheit:
 * <ul>
 *   <li>Constant-Time-Vergleich via {@link MessageDigest#isEqual(byte[], byte[])}
 *       — verhindert Timing-Side-Channel-Attacken auf den Key.</li>
 *   <li>JSON-Fehler-Responses via {@link ObjectMapper} — kein
 *       String-Concat, keine Escaping-Bugs bei zukünftigen Messages.</li>
 *   <li>{@code RateLimitFilter} läuft global vor diesem Filter (Phase 1.1)
 *       → Brute-Force gegen den Key ist token-bucket-limitiert pro IP.
 *       Reihenfolge ist explizit in {@code SecurityConfig} über lineare
 *       {@code addFilterAfter}-Aufrufe verdrahtet (LoginSperre → RateLimit →
 *       ApiKey → UsernamePasswordAuthenticationFilter), nicht via
 *       {@code @Order} — das wirkt nur auf Servlet-Auto-Registrierung,
 *       nicht auf den Spring-Security-Chain.</li>
 * </ul>
 *
 * <p>Die Route {@code /api/**} ist in der SecurityConfig als
 * {@code permitAll} + CSRF-exempt konfiguriert — der API-Key
 * übernimmt die Authentifizierung.
 */
@Component
public class ApiKeyFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(ApiKeyFilter.class);
    private static final String HEADER = "X-API-Key";

    private final String apiKey;
    private final byte[] apiKeyBytes;
    private final ObjectMapper objectMapper;

    public ApiKeyFilter(@Value("${sponsorplatz.api.key:}") String apiKey,
                        ObjectMapper objectMapper) {
        this.apiKey = apiKey;
        this.apiKeyBytes = (apiKey == null ? "" : apiKey).getBytes(StandardCharsets.UTF_8);
        this.objectMapper = objectMapper;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().startsWith("/api/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        if (apiKey == null || apiKey.isBlank()) {
            log.warn("API-Zugriff abgelehnt: sponsorplatz.api.key ist nicht konfiguriert");
            antworteMitFehler(response, HttpServletResponse.SC_SERVICE_UNAVAILABLE,
                    "API nicht aktiviert — sponsorplatz.api.key muss konfiguriert sein.");
            return;
        }

        String gelieferterKey = request.getHeader(HEADER);
        if (gelieferterKey == null
                || !MessageDigest.isEqual(apiKeyBytes,
                        gelieferterKey.getBytes(StandardCharsets.UTF_8))) {
            log.warn("API-Zugriff abgelehnt: ungültiger API-Key von {}", request.getRemoteAddr());
            antworteMitFehler(response, HttpServletResponse.SC_UNAUTHORIZED,
                    "Ungültiger oder fehlender API-Key. Header 'X-API-Key' setzen.");
            return;
        }

        // Caller-Source als Request-Attribute — Controller kann das für
        // Audit-Log oder erstelltVon-Felder ausgewerten. Aktuell ein
        // einziger Key → Source = "api"; sobald mehrere Keys via
        // sponsorplatz.api.keys.<name>=... existieren, kann hier der
        // Name (statt "api") gesetzt werden.
        request.setAttribute("apiCallerSource", "api");

        filterChain.doFilter(request, response);
    }

    private void antworteMitFehler(HttpServletResponse response, int status, String message) throws IOException {
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        objectMapper.writeValue(response.getWriter(), Map.of("error", message));
    }
}
