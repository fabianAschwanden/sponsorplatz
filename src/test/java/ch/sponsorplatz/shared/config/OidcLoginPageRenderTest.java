package ch.sponsorplatz.shared.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * SSO-05: Mit konfiguriertem OAuth2-Client zeigt {@code /login} sowohl das
 * Form-Login-Formular als auch den OAuth2-Anbieter-Button. Verifiziert, dass
 * beide Pfade auf der Login-Seite erreichbar sind (Spec §3.2).
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@ActiveProfiles("dev")
@TestPropertySource(properties = {
        "spring.security.oauth2.client.registration.entra.client-id=test-client",
        "spring.security.oauth2.client.registration.entra.client-secret=test-secret",
        "spring.security.oauth2.client.registration.entra.scope=openid,profile,email",
        "spring.security.oauth2.client.registration.entra.client-authentication-method=client_secret_basic",
        "spring.security.oauth2.client.registration.entra.authorization-grant-type=authorization_code",
        "spring.security.oauth2.client.registration.entra.redirect-uri={baseUrl}/login/oauth2/code/{registrationId}",
        "spring.security.oauth2.client.registration.entra.client-name=CSS-Konto",
        "spring.security.oauth2.client.provider.entra.authorization-uri=http://localhost/oauth2/authorize",
        "spring.security.oauth2.client.provider.entra.token-uri=http://localhost/oauth2/token",
        "spring.security.oauth2.client.provider.entra.jwk-set-uri=http://localhost/jwks",
        "spring.security.oauth2.client.provider.entra.user-info-uri=http://localhost/userinfo",
        "spring.security.oauth2.client.provider.entra.user-name-attribute=sub"
})
class OidcLoginPageRenderTest {

    @Autowired
    private WebApplicationContext context;

    private MockMvc mockMvc;

    @org.junit.jupiter.api.BeforeEach
    void setUp() {
        this.mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(springSecurity())
                .build();
    }

    @Test
    @DisplayName("SSO-05: /login zeigt Form-Login UND OAuth2-Anbieter-Button")
    void loginZeigtBeideAnmeldepfade() throws Exception {
        mockMvc.perform(get("/login"))
                .andExpect(status().isOk())
                // Form-Login (E-Mail/Passwort-Felder)
                .andExpect(content().string(allOf(
                        containsString("name=\"username\""),
                        containsString("name=\"password\""))))
                // OAuth2-Button auf konfigurierten Provider
                .andExpect(content().string(allOf(
                        containsString("/oauth2/authorization/entra"),
                        containsString("CSS-Konto"))));
    }
}
