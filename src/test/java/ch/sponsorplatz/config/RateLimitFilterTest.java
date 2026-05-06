package ch.sponsorplatz.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.IOException;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * Tests für den IP-basierten Rate-Limit-Filter auf Public-POST-Endpoints.
 *
 * Test-IDs: RATE-01..04 in {@code specs/TESTSTRATEGIE.md}.
 */
class RateLimitFilterTest {

    private RateLimitFilter filter;
    private FilterChain chain;

    @BeforeEach
    void setUp() {
        // 3 Requests pro 1-Minuten-Fenster, alle anderen 429.
        filter = new RateLimitFilter(3, Duration.ofMinutes(1));
        chain = mock(FilterChain.class);
    }

    @Test
    @DisplayName("RATE-01: Filter ignoriert nicht-konfigurierte Pfade (z.B. GET /marktplatz)")
    void ignoriertNichtPostOderPublicPath() throws ServletException, IOException {
        MockHttpServletRequest req = new MockHttpServletRequest("GET", "/marktplatz");
        req.setRemoteAddr("1.2.3.4");
        MockHttpServletResponse res = new MockHttpServletResponse();

        filter.doFilter(req, res, chain);

        verify(chain).doFilter(req, res);
        assertThat(res.getStatus()).isEqualTo(200);
    }

    @Test
    @DisplayName("RATE-02: POST /registrieren — erste Requests gehen durch")
    void erlaubtErsteAnfragen() throws ServletException, IOException {
        for (int i = 0; i < 3; i++) {
            MockHttpServletRequest req = new MockHttpServletRequest("POST", "/registrieren");
            req.setRemoteAddr("9.9.9.9");
            MockHttpServletResponse res = new MockHttpServletResponse();
            filter.doFilter(req, res, chain);
            assertThat(res.getStatus()).as("Request %d", i + 1).isEqualTo(200);
        }
        verify(chain, times(3)).doFilter(any(), any());
    }

    @Test
    @DisplayName("RATE-03: 4. POST in 1 min vom selben IP → 429")
    void blockt429NachLimit() throws ServletException, IOException {
        for (int i = 0; i < 3; i++) {
            MockHttpServletRequest r = new MockHttpServletRequest("POST", "/registrieren");
            r.setRemoteAddr("9.9.9.9");
            filter.doFilter(r, new MockHttpServletResponse(), chain);
        }
        // Vierter Request:
        MockHttpServletRequest req = new MockHttpServletRequest("POST", "/registrieren");
        req.setRemoteAddr("9.9.9.9");
        MockHttpServletResponse res = new MockHttpServletResponse();
        filter.doFilter(req, res, chain);

        assertThat(res.getStatus()).isEqualTo(429);
        verify(chain, times(3)).doFilter(any(), any());
    }

    @Test
    @DisplayName("RATE-04: Zwei verschiedene IPs haben getrennte Buckets")
    void getrennteBucketsProIp() throws ServletException, IOException {
        // IP A erschöpft Bucket
        for (int i = 0; i < 3; i++) {
            MockHttpServletRequest r = new MockHttpServletRequest("POST", "/registrieren");
            r.setRemoteAddr("1.1.1.1");
            filter.doFilter(r, new MockHttpServletResponse(), chain);
        }
        // IP B kommt unbeeindruckt durch
        MockHttpServletRequest req = new MockHttpServletRequest("POST", "/registrieren");
        req.setRemoteAddr("2.2.2.2");
        MockHttpServletResponse res = new MockHttpServletResponse();
        filter.doFilter(req, res, chain);

        assertThat(res.getStatus()).isEqualTo(200);
    }

    @Test
    @DisplayName("RATE-05: Filter respektiert X-Forwarded-For (Caddy-Reverse-Proxy)")
    void honoriertXForwardedFor() throws ServletException, IOException {
        for (int i = 0; i < 3; i++) {
            MockHttpServletRequest r = new MockHttpServletRequest("POST", "/registrieren");
            r.setRemoteAddr("127.0.0.1");                 // Caddy-Localhost
            r.addHeader("X-Forwarded-For", "5.5.5.5");
            filter.doFilter(r, new MockHttpServletResponse(), chain);
        }
        // Vierter Request vom selben echten Client
        MockHttpServletRequest req = new MockHttpServletRequest("POST", "/registrieren");
        req.setRemoteAddr("127.0.0.1");
        req.addHeader("X-Forwarded-For", "5.5.5.5");
        MockHttpServletResponse res = new MockHttpServletResponse();
        filter.doFilter(req, res, chain);

        assertThat(res.getStatus()).isEqualTo(429);

        // Anderer X-Forwarded-For darf passieren
        MockHttpServletRequest req2 = new MockHttpServletRequest("POST", "/registrieren");
        req2.setRemoteAddr("127.0.0.1");
        req2.addHeader("X-Forwarded-For", "6.6.6.6");
        MockHttpServletResponse res2 = new MockHttpServletResponse();
        filter.doFilter(req2, res2, chain);
        assertThat(res2.getStatus()).isEqualTo(200);
    }
}
