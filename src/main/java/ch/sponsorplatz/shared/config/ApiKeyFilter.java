package ch.sponsorplatz.shared.config;

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

/**
 * API-Key-Authentifizierung für {@code /api/**}-Routen.
 *
 * <p>Prüft den {@code X-API-Key}-Header gegen den in
 * {@code sponsorplatz.api.key} konfigurierten Schlüssel.
 * Ohne konfigurierten Key (leer/null) sind API-Routen gesperrt.
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

    public ApiKeyFilter(@Value("${sponsorplatz.api.key:}") String apiKey) {
        this.apiKey = apiKey;
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
        if (gelieferterKey == null || !apiKey.equals(gelieferterKey)) {
            log.warn("API-Zugriff abgelehnt: ungültiger API-Key von {}", request.getRemoteAddr());
            antworteMitFehler(response, HttpServletResponse.SC_UNAUTHORIZED,
                    "Ungültiger oder fehlender API-Key. Header 'X-API-Key' setzen.");
            return;
        }

        filterChain.doFilter(request, response);
    }

    private void antworteMitFehler(HttpServletResponse response, int status, String message) throws IOException {
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write("{\"error\":\"" + message + "\"}");
    }
}

