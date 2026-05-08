package ch.sponsorplatz.shared.config;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler;

import java.io.IOException;

/**
 * Custom Success-Handler: setzt Brute-Force-Zähler bei erfolgreichem Login
 * zurück und respektiert {@code savedRequest} — wenn der User auf eine
 * geschützte Seite (z.B. {@code /organisationen/x}) wollte und auf {@code /login}
 * umgeleitet wurde, landet er nach dem Login wieder dort. Ohne savedRequest
 * fällt er auf {@code /dashboard} zurück.
 */
public class LoginSuccessHandler extends SavedRequestAwareAuthenticationSuccessHandler {

    private final LoginBruteForceSchutz bruteForceSchutz;

    public LoginSuccessHandler(LoginBruteForceSchutz bruteForceSchutz) {
        setDefaultTargetUrl("/dashboard");
        this.bruteForceSchutz = bruteForceSchutz;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                         HttpServletResponse response,
                                         Authentication authentication) throws IOException, ServletException {
        bruteForceSchutz.erfolgreichenLoginRegistrieren(authentication.getName());
        super.onAuthenticationSuccess(request, response, authentication);
    }
}

