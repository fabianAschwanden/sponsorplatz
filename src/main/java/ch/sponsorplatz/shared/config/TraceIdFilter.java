package ch.sponsorplatz.shared.config;

import java.io.IOException;
import java.util.UUID;
import java.util.regex.Pattern;

import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Servlet-Filter, der eine Trace-ID in den MDC (Mapped Diagnostic Context) setzt.
 *
 * <p>Ablauf:
 * <ol>
 *   <li>Prüft, ob ein {@code X-Trace-ID}-Header im Request vorhanden ist
 *       (z.B. vom Load-Balancer / Reverse-Proxy).</li>
 *   <li>Validiert das Format gegen {@link #TRACE_ID_PATTERN} — nur Hex-/Base64URL-Zeichen
 *       1-64 Stellen. Ein Wert ausserhalb dieses Bereichs wird ignoriert, der Filter
 *       generiert dann eine neue UUID. Damit ist Log-Injection durch Newline / Quotes
 *       / unbegrenzte Längen ausgeschlossen.</li>
 *   <li>Setzt die Trace-ID in den MDC unter dem Key {@code traceId} sowie HTTP-Methode und URI.</li>
 *   <li>Gibt die Trace-ID im Response-Header {@code X-Trace-ID} zurück.</li>
 *   <li>Räumt den MDC nach der Verarbeitung auf (auch bei Exception via try/finally —
 *       sonst leakt der Wert in Spring-Threadpool-Threads zum nächsten Request).</li>
 * </ol>
 *
 * <p><b>Roadmap — W3C Trace Context:</b> Aktuell verwenden wir einen proprietären
 * {@code X-Trace-ID}-Header mit UUID-Format. Für Distributed-Tracing mit
 * OpenTelemetry/Jaeger/Tempo ist der W3C-Standard {@code traceparent}
 * ({@code 00-<32hex>-<16hex>-01}) das zukünftige Ziel. Migration: zusätzlicher
 * Read des {@code traceparent}-Headers + Forwarding an Downstream-Services.
 * Siehe Roadmap-Eintrag MON-W3C.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class TraceIdFilter implements Filter {

    private static final String HEADER_NAME = "X-Trace-ID";
    private static final String MDC_TRACE_ID = "traceId";
    private static final String MDC_METHOD = "method";
    private static final String MDC_URI = "uri";

    /**
     * Zulässiges Trace-ID-Format. Bewusst auf den lesbaren Subset begrenzt:
     * Alphanumerik + {@code .}, {@code _}, {@code -}. Damit fallen Newlines,
     * Quotes und Sonderzeichen raus, die Log-Injection ermöglichen würden.
     * Länge {@code 1..64} — passt für UUIDs (36) und für W3C-traceparent-IDs (32).
     */
    static final Pattern TRACE_ID_PATTERN = Pattern.compile("^[A-Za-z0-9._-]{1,64}$");

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        String traceId = httpRequest.getHeader(HEADER_NAME);
        if (traceId == null || !TRACE_ID_PATTERN.matcher(traceId).matches()) {
            traceId = UUID.randomUUID().toString();
        }

        MDC.put(MDC_TRACE_ID, traceId);
        MDC.put(MDC_METHOD, httpRequest.getMethod());
        MDC.put(MDC_URI, httpRequest.getRequestURI());

        httpResponse.setHeader(HEADER_NAME, traceId);

        try {
            chain.doFilter(request, response);
        } finally {
            MDC.remove(MDC_TRACE_ID);
            MDC.remove(MDC_METHOD);
            MDC.remove(MDC_URI);
        }
    }
}

