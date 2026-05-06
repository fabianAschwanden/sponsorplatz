package ch.sponsorplatz.config;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * IP-basierter Rate-Limit-Filter für Public-POST-Endpoints (Anti-Bot).
 *
 * <p>Strategie: pro Client-IP ein Token-Bucket mit konfigurierbarer Kapazität
 * pro Zeitfenster. Vier konkrete Pfade sind geschützt:
 * <ul>
 *   <li>{@code POST /registrieren}            — Account-Anlage</li>
 *   <li>{@code POST /sponsor/registrieren}    — Sponsor-Self-Reg</li>
 *   <li>{@code POST /login}                   — Brute-Force-Schutz</li>
 *   <li>{@code POST /organisationen/.../anfragen}  — Sponsoring-Anfrage-Spam</li>
 * </ul>
 *
 * <p>Bei 429 setzt der Filter {@code Retry-After}-Header und schreibt einen
 * kurzen JSON-Body. Der Filter läuft VOR Spring Security via Filter-Bean.
 *
 * <p>Storage: {@link ConcurrentHashMap} — reicht für Single-VM. Bei multi-
 * Instance auf Hazelcast/Redis wechseln.
 *
 * <p>Client-IP-Erkennung: {@code X-Forwarded-For}-Header (Caddy reicht ihn weiter),
 * Fallback {@code request.getRemoteAddr()}.
 */
public class RateLimitFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(RateLimitFilter.class);

    /** Geschützte Pfade — exact match (Anfragen-Pfad ist Pattern, siehe matches). */
    private static final Set<String> EXACT_PATHS = Set.of(
            "/registrieren",
            "/sponsor/registrieren",
            "/login"
    );

    private final long capacity;
    private final Duration window;
    private final ConcurrentHashMap<String, Bucket> buckets = new ConcurrentHashMap<>();

    public RateLimitFilter(long capacity, Duration window) {
        this.capacity = capacity;
        this.window = window;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        if (!istGeschuetzt(request)) {
            chain.doFilter(request, response);
            return;
        }

        String ip = ermittleIp(request);
        Bucket bucket = buckets.computeIfAbsent(ip, this::neuerBucket);

        if (bucket.tryConsume(1)) {
            chain.doFilter(request, response);
        } else {
            log.warn("Rate-Limit überschritten für IP {} auf {} {}", ip, request.getMethod(), request.getRequestURI());
            response.setStatus(429);
            response.setHeader("Retry-After", String.valueOf(window.toSeconds()));
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write(
                    "{\"error\":\"too_many_requests\",\"message\":\"Zu viele Anfragen — bitte später erneut versuchen.\"}");
        }
    }

    private boolean istGeschuetzt(HttpServletRequest request) {
        if (!"POST".equalsIgnoreCase(request.getMethod())) {
            return false;
        }
        String path = request.getRequestURI();
        if (EXACT_PATHS.contains(path)) {
            return true;
        }
        // /organisationen/{slug}/anfragen — schützt Anfrage-Spam
        return path.matches("^/organisationen/[^/]+/anfragen$");
    }

    private String ermittleIp(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            // X-Forwarded-For kann Komma-Liste sein — erster Eintrag = original Client
            int komma = xff.indexOf(',');
            return komma > 0 ? xff.substring(0, komma).trim() : xff.trim();
        }
        return request.getRemoteAddr();
    }

    private Bucket neuerBucket(String ip) {
        return Bucket.builder()
                .addLimit(Bandwidth.builder()
                        .capacity(capacity)
                        .refillIntervally(capacity, window)
                        .build())
                .build();
    }
}
