package ch.sponsorplatz.shared.config;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;

import java.io.IOException;

/**
 * Custom Failure-Handler: registriert Fehlversuche im Brute-Force-Schutz.
 */
public class LoginFailureHandler extends SimpleUrlAuthenticationFailureHandler {

    private final LoginBruteForceSchutz bruteForceSchutz;

    public LoginFailureHandler(LoginBruteForceSchutz bruteForceSchutz) {
        super("/login?error");
        this.bruteForceSchutz = bruteForceSchutz;
    }

    @Override
    public void onAuthenticationFailure(HttpServletRequest request,
                                         HttpServletResponse response,
                                         AuthenticationException exception) throws IOException, ServletException {
        String email = request.getParameter("username");
        if (email != null && !email.isBlank()) {
            bruteForceSchutz.fehlversuchRegistrieren(email);
        }
        super.onAuthenticationFailure(request, response, exception);
    }
}

