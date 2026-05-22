package ch.sponsorplatz.benutzer;

import ch.sponsorplatz.shared.config.SecurityConfig;
import ch.sponsorplatz.shared.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

/**
 * Controller-Tests für {@link TwoFaLoginController}.
 * Test-IDs AUTH-2FA-10..11 in {@code specs/AUTH_2FA_TOTP.md}.
 */
@WebMvcTest(controllers = TwoFaLoginController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class})
@ActiveProfiles("dev")
class TwoFaLoginControllerTest {

    private static final String EMAIL = "user@sponsorplatz.ch";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean private TwoFaService twoFaService;
    @MockitoBean private TwoFaLoginSession loginSession;
    @MockitoBean private SponsorplatzUserDetailsService userDetailsService;

    private static Authentication pendingAuth() {
        return new UsernamePasswordAuthenticationToken(
                EMAIL, "n/a", List.of(new SimpleGrantedAuthority("ROLE_USER")));
    }

    @Test
    @DisplayName("AUTH-2FA-10a: GET /login/2fa ohne pending-Auth → Redirect /login")
    void getOhnePendingRedirectsLogin() throws Exception {
        mockMvc.perform(get("/login/2fa"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }

    @Test
    @DisplayName("AUTH-2FA-10b: GET /login/2fa mit pending-Auth zeigt Form")
    void getMitPendingZeigtForm() throws Exception {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute(TwoFaAuthenticationSuccessHandler.SESSION_PENDING_AUTH, pendingAuth());

        mockMvc.perform(get("/login/2fa").session(session))
                .andExpect(status().isOk())
                .andExpect(view().name("benutzer/2fa-login"));
    }

    @Test
    @DisplayName("AUTH-2FA-10c: POST /login/2fa korrekter Code → Auth installiert + Redirect /dashboard")
    void postKorrekterCode() throws Exception {
        MockHttpSession session = new MockHttpSession();
        Authentication auth = pendingAuth();
        session.setAttribute(TwoFaAuthenticationSuccessHandler.SESSION_PENDING_AUTH, auth);
        when(twoFaService.verifyForLogin(eq(EMAIL), eq("123456"), eq(1)))
                .thenReturn(new TwoFaService.LoginVerifyResult(true, false));

        mockMvc.perform(post("/login/2fa").param("code", "123456").session(session).with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/dashboard"));

        verify(loginSession).installAuthentication(eq(auth), any(), any());
    }

    @Test
    @DisplayName("AUTH-2FA-10d: POST /login/2fa falscher Code → Redirect /login/2fa?error, Counter +1")
    void postFalscherCode() throws Exception {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute(TwoFaAuthenticationSuccessHandler.SESSION_PENDING_AUTH, pendingAuth());
        when(twoFaService.verifyForLogin(eq(EMAIL), eq("000000"), eq(1)))
                .thenReturn(TwoFaService.LoginVerifyResult.MISS);

        mockMvc.perform(post("/login/2fa").param("code", "000000").session(session).with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login/2fa?error"));

        // Counter wurde gesetzt
        Object counter = session.getAttribute(TwoFaAuthenticationSuccessHandler.SESSION_FAIL_COUNTER);
        org.assertj.core.api.Assertions.assertThat(counter).isEqualTo(1);
    }

    @Test
    @DisplayName("AUTH-2FA-11: POST /login/2fa beim 5. Fehlversuch → Lockout-Audit + Session-Invalidate + Redirect")
    void postFuenfterFehlversuchLockout() throws Exception {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute(TwoFaAuthenticationSuccessHandler.SESSION_PENDING_AUTH, pendingAuth());
        session.setAttribute(TwoFaAuthenticationSuccessHandler.SESSION_FAIL_COUNTER, 4);
        when(twoFaService.verifyForLogin(eq(EMAIL), eq("000000"), eq(5)))
                .thenReturn(TwoFaService.LoginVerifyResult.MISS);

        mockMvc.perform(post("/login/2fa").param("code", "000000").session(session).with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?error=2fa_locked"));

        verify(twoFaService).protokolliereLockout(EMAIL);
        // Session sollte invalidated sein — neue MockHttpSession-Inspection
        org.assertj.core.api.Assertions.assertThat(session.isInvalid()).isTrue();
    }
}
