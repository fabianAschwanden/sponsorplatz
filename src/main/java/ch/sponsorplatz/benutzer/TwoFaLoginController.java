package ch.sponsorplatz.benutzer;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import jakarta.servlet.http.HttpServletResponse;

/**
 * Zweiter Login-Schritt: TOTP- oder Backup-Code-Eingabe nach erfolgreicher
 * Passwort-Authentifizierung ({@link TwoFaAuthenticationSuccessHandler}).
 *
 * <ul>
 *   <li>GET  {@code /login/2fa} — Formular (Redirect /login wenn kein Pending-Auth)</li>
 *   <li>POST {@code /login/2fa} — verify Code; bei Erfolg Auth installieren + Redirect /dashboard.
 *       Bei Fehler Session-Counter +1; bei 5 Fehlversuchen Session invalidate +
 *       Redirect /login?error=2fa_locked.</li>
 * </ul>
 *
 * <p>Spec: {@code specs/AUTH_2FA_TOTP.md} Slice B. Tests AUTH-2FA-10..11.
 */
@Controller
@RequestMapping("/login/2fa")
public class TwoFaLoginController {

    private static final Logger log = LoggerFactory.getLogger(TwoFaLoginController.class);
    private static final int MAX_FAILURES = 5;

    private final TwoFaService twoFaService;
    private final TwoFaLoginSession loginSession;

    public TwoFaLoginController(TwoFaService twoFaService, TwoFaLoginSession loginSession) {
        this.twoFaService = twoFaService;
        this.loginSession = loginSession;
    }

    @GetMapping
    public String formular(HttpSession session, Model model) {
        Authentication pending = (Authentication) session.getAttribute(
                TwoFaAuthenticationSuccessHandler.SESSION_PENDING_AUTH);
        if (pending == null) {
            return "redirect:/login";
        }
        Integer fails = (Integer) session.getAttribute(TwoFaAuthenticationSuccessHandler.SESSION_FAIL_COUNTER);
        model.addAttribute("verbleibend", MAX_FAILURES - (fails == null ? 0 : fails));
        return "benutzer/2fa-login";
    }

    @PostMapping
    public String verifizieren(@RequestParam String code,
                               HttpServletRequest request,
                               HttpServletResponse response,
                               HttpSession session) {
        Authentication pending = (Authentication) session.getAttribute(
                TwoFaAuthenticationSuccessHandler.SESSION_PENDING_AUTH);
        if (pending == null) {
            return "redirect:/login";
        }
        String email = pending.getName();
        int versuch = ((Integer) session.getAttribute(TwoFaAuthenticationSuccessHandler.SESSION_FAIL_COUNTER)
                instanceof Integer i ? i : 0) + 1;

        TwoFaService.LoginVerifyResult ergebnis = twoFaService.verifyForLogin(email, code, versuch);

        if (!ergebnis.matched()) {
            if (versuch >= MAX_FAILURES) {
                log.warn("2FA-Lockout für {} nach {} Fehlversuchen — Session-Invalidate", email, versuch);
                twoFaService.protokolliereLockout(email);
                session.invalidate();
                return "redirect:/login?error=2fa_locked";
            }
            session.setAttribute(TwoFaAuthenticationSuccessHandler.SESSION_FAIL_COUNTER, versuch);
            return "redirect:/login/2fa?error";
        }

        // 2FA bestanden — Authentication in den SecurityContext + die Session
        // installieren (Folge-Requests sind dann authentisch).
        loginSession.installAuthentication(pending, request, response);

        session.removeAttribute(TwoFaAuthenticationSuccessHandler.SESSION_PENDING_AUTH);
        session.removeAttribute(TwoFaAuthenticationSuccessHandler.SESSION_FAIL_COUNTER);
        log.debug("2FA-Login OK für {} (backup={})", email, ergebnis.backupCodeGenutzt());
        return "redirect:/dashboard";
    }
}
