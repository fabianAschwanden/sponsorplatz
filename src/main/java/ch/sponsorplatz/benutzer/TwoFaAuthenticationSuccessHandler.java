package ch.sponsorplatz.benutzer;

import ch.sponsorplatz.shared.config.LoginBruteForceSchutz;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.IOException;

/**
 * Erweitert {@link LoginSuccessHandler} um die 2-Faktor-Authentifizierungs-
 * Challenge (Phase 13.2 Slice B).
 *
 * <ul>
 *   <li>User hat <strong>2FA aktiv</strong>: führt die Brute-Force-Reset +
 *       Sprache-Sync-Buchhaltung weiter aus, stash dann die Authentication
 *       in der HTTP-Session unter {@link #SESSION_PENDING_AUTH}, leert den
 *       SecurityContext und leitet auf {@code /login/2fa} um.</li>
 *   <li>User <strong>ohne 2FA</strong>: regulärer Erfolgs-Redirect (saved
 *       request oder {@code /dashboard}).</li>
 * </ul>
 *
 * <p>Damit ist kein zusätzliches {@code ROLE_PRE_2FA} nötig: ein User der
 * von {@code /login/2fa} wegnavigiert ist einfach wieder anonym (SecurityContext
 * leer) und kommt nirgendwo rein.
 *
 * <p>Spec: {@code specs/AUTH_2FA_TOTP.md} Slice B. Tests AUTH-2FA-10..11.
 */
public class TwoFaAuthenticationSuccessHandler extends LoginSuccessHandler {

    private static final Logger log = LoggerFactory.getLogger(TwoFaAuthenticationSuccessHandler.class);

    public static final String SESSION_PENDING_AUTH = "totp.pendingAuth";
    public static final String SESSION_FAIL_COUNTER = "totp.failureCount";

    private final TwoFaService twoFaService;

    public TwoFaAuthenticationSuccessHandler(LoginBruteForceSchutz bruteForceSchutz,
                                             AppUserRepository appUserRepository,
                                             TwoFaService twoFaService) {
        super(bruteForceSchutz, appUserRepository);
        this.twoFaService = twoFaService;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication)
            throws IOException, ServletException {
        postLoginBookkeeping(authentication, response);

        String email = authentication.getName();
        TwoFaService.TwoFaStatus status = twoFaService.findStatus(email);
        if (status.aktiv()) {
            HttpSession session = request.getSession(true);
            session.setAttribute(SESSION_PENDING_AUTH, authentication);
            session.removeAttribute(SESSION_FAIL_COUNTER);
            SecurityContextHolder.clearContext();
            log.debug("2FA-Challenge erforderlich für {} — Redirect /login/2fa", email);
            response.sendRedirect(request.getContextPath() + "/login/2fa");
            return;
        }

        super.onAuthenticationSuccess(request, response, authentication);
    }
}
