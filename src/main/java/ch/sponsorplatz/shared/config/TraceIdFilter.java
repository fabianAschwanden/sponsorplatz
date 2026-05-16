package ch.sponsorplatz.shared.config;

import java.io.IOException;
import java.security.SecureRandom;
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
 * <p>Unterstützt zwei Header-Konventionen — der W3C-Standard hat Vorrang,
 * die Legacy-Variante bleibt für Migrations-Frieden bestehen:
 *
 * <ol>
 *   <li><b>{@code traceparent}</b> (W3C Trace Context, OpenTelemetry-kompatibel) —
 *       Format {@code 00-<32hex>-<16hex>-<2hex>}. Wenn gesetzt und gültig,
 *       wird die Trace-ID übernommen. Eine frische Span-ID wird pro Hop generiert
 *       (wir sind der Empfänger-Hop, nicht der Sender).</li>
 *   <li><b>{@code X-Trace-ID}</b> (Legacy) — bleibt akzeptiert für Caller, die
 *       noch nicht migriert sind. Validiert gegen {@link #LEGACY_PATTERN}.</li>
 * </ol>
 *
 * <p>Wenn beide Header fehlen oder ungültig sind, generiert der Filter eine
 * frische W3C-konforme Trace-ID (32 hex) + Span-ID (16 hex).
 *
 * <p>MDC-Keys: {@code traceId} (16-Byte hex, OTel-konform), {@code spanId}
 * (8-Byte hex), {@code method}, {@code uri}. Logback-Encoder rendert sie als
 * strukturierte Felder.
 *
 * <p>Response-Header: sowohl {@code traceparent} (Standard für Downstream-
 * Services) als auch {@code X-Trace-ID} (Backcompat) werden gesetzt.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class TraceIdFilter implements Filter {

    private static final String TRACEPARENT_HEADER = "traceparent";
    private static final String LEGACY_HEADER = "X-Trace-ID";
    private static final String MDC_TRACE_ID = "traceId";
    private static final String MDC_SPAN_ID = "spanId";
    private static final String MDC_METHOD = "method";
    private static final String MDC_URI = "uri";

    /** W3C-traceparent: {@code 00-<32hex>-<16hex>-<2hex>}. Version-`00` ist zur Zeit die einzige spezifizierte. */
    static final Pattern TRACEPARENT_PATTERN = Pattern.compile(
            "^00-[0-9a-f]{32}-[0-9a-f]{16}-[0-9a-f]{2}$");

    /**
     * Legacy-Trace-ID — bewusst auf Alphanumerik + {@code .}, {@code _},
     * {@code -} begrenzt. Damit fallen Newlines, Quotes und Sonderzeichen raus,
     * die Log-Injection ermöglichen würden. Länge 1..64 — passt für UUIDs (36)
     * und für W3C-Trace-IDs (32).
     */
    static final Pattern LEGACY_PATTERN = Pattern.compile("^[A-Za-z0-9._-]{1,64}$");

    /** All-Zero-IDs sind per W3C-Spec ungültig (»not initialised«). */
    private static final String INVALID_ZERO_TRACE = "00000000000000000000000000000000";
    private static final String INVALID_ZERO_SPAN = "0000000000000000";

    private final SecureRandom random = new SecureRandom();

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        String traceId = traceIdAusRequest(httpRequest);
        String spanId = neueId(8); // wir sind der Empfänger-Hop, frische Span

        MDC.put(MDC_TRACE_ID, traceId);
        MDC.put(MDC_SPAN_ID, spanId);
        MDC.put(MDC_METHOD, httpRequest.getMethod());
        MDC.put(MDC_URI, httpRequest.getRequestURI());

        // W3C-Antwort: nur wenn traceId W3C-Format ist, sonst frische erzeugen,
        // damit der Downstream-Hop ein gültiges traceparent sieht.
        String traceparentTraceId = istW3cTraceId(traceId) ? traceId : neueId(16);
        httpResponse.setHeader(TRACEPARENT_HEADER,
                "00-" + traceparentTraceId + "-" + spanId + "-01");
        // Backcompat — interne Caller, die noch X-Trace-ID lesen
        httpResponse.setHeader(LEGACY_HEADER, traceId);

        try {
            chain.doFilter(request, response);
        } finally {
            MDC.remove(MDC_TRACE_ID);
            MDC.remove(MDC_SPAN_ID);
            MDC.remove(MDC_METHOD);
            MDC.remove(MDC_URI);
        }
    }

    private String traceIdAusRequest(HttpServletRequest req) {
        // 1) W3C-Header hat Vorrang
        String traceparent = req.getHeader(TRACEPARENT_HEADER);
        if (traceparent != null && TRACEPARENT_PATTERN.matcher(traceparent).matches()) {
            String[] parts = traceparent.split("-");
            String tid = parts[1];
            String sid = parts[2];
            if (!INVALID_ZERO_TRACE.equals(tid) && !INVALID_ZERO_SPAN.equals(sid)) {
                return tid;
            }
        }
        // 2) Legacy-Header als Fallback (Caller noch nicht migriert)
        String legacy = req.getHeader(LEGACY_HEADER);
        if (legacy != null && LEGACY_PATTERN.matcher(legacy).matches()) {
            return legacy;
        }
        // 3) Fresh — direkt W3C-Format (16 Bytes = 32 hex), passt durch beide Patterns
        return neueId(16);
    }

    private boolean istW3cTraceId(String id) {
        return id.length() == 32 && id.matches("[0-9a-f]{32}")
                && !INVALID_ZERO_TRACE.equals(id);
    }

    private String neueId(int bytes) {
        byte[] b = new byte[bytes];
        random.nextBytes(b);
        StringBuilder sb = new StringBuilder(bytes * 2);
        for (byte x : b) {
            sb.append(String.format("%02x", x & 0xff));
        }
        return sb.toString();
    }
}
