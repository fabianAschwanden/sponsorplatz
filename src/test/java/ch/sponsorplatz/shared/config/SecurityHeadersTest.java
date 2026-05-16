package ch.sponsorplatz.shared.config;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Tests für Phase 10.5 — Security-Hardening Response-Headers.
 *
 * <p>Verifiziert, dass die SecurityConfig die OWASP-empfohlenen
 * Security-Headers auf allen Responses setzt.
 *
 * <ul>
 *   <li>SEC-HDR-01: X-Content-Type-Options: nosniff</li>
 *   <li>SEC-HDR-02: Referrer-Policy: strict-origin-when-cross-origin</li>
 *   <li>SEC-HDR-03: Permissions-Policy (keine Kamera/Mikrofon/Geo/Payment)</li>
 *   <li>SEC-HDR-04: Content-Security-Policy vorhanden</li>
 *   <li>SEC-HDR-05: CSP enthält script-src mit sentry-cdn</li>
 * </ul>
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@ActiveProfiles("dev")
class SecurityHeadersTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("SEC-HDR-01: X-Content-Type-Options: nosniff")
    void xContentTypeOptionsGesetzt() throws Exception {
        mockMvc.perform(get("/impressum"))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Content-Type-Options", "nosniff"));
    }

    @Test
    @DisplayName("SEC-HDR-02: Referrer-Policy: strict-origin-when-cross-origin")
    void referrerPolicyGesetzt() throws Exception {
        mockMvc.perform(get("/impressum"))
                .andExpect(status().isOk())
                .andExpect(header().string("Referrer-Policy", "strict-origin-when-cross-origin"));
    }

    @Test
    @DisplayName("SEC-HDR-03: Permissions-Policy schraenkt Sensoren ein")
    void permissionsPolicyGesetzt() throws Exception {
        mockMvc.perform(get("/impressum"))
                .andExpect(status().isOk())
                .andExpect(header().string("Permissions-Policy",
                        "camera=(), microphone=(), geolocation=(), payment=()"));
    }

    @Test
    @DisplayName("SEC-HDR-04: Content-Security-Policy Header vorhanden")
    void cspHeaderVorhanden() throws Exception {
        mockMvc.perform(get("/impressum"))
                .andExpect(status().isOk())
                .andExpect(header().exists("Content-Security-Policy"));
    }

    @Test
    @DisplayName("SEC-HDR-05: CSP erlaubt Sentry-CDN für Script-Loading")
    void cspEnthaeltSentryCdn() throws Exception {
        String csp = mockMvc.perform(get("/impressum"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getHeader("Content-Security-Policy");

        org.assertj.core.api.Assertions.assertThat(csp)
                .contains("script-src")
                .contains("https://browser.sentry-cdn.com")
                .contains("default-src 'self'");
    }
}
