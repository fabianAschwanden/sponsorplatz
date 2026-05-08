package ch.sponsorplatz.shared.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.lang.NonNull;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Filter der gesperrte Accounts beim Login-POST sofort mit 302-Redirect auf
 * {@code /login?gesperrt=<sek>} blockt, BEVOR Spring Security die Credentials
 * prüft (spart BCrypt-Kosten und gibt dem User die Sperr-Meldung statt einer
 * generischen Fehlermeldung).
 */
public class LoginSperreFilter extends OncePerRequestFilter {

    private final LoginBruteForceSchutz bruteForceSchutz;

    public LoginSperreFilter(LoginBruteForceSchutz bruteForceSchutz) {
        this.bruteForceSchutz = bruteForceSchutz;
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain chain) throws ServletException, IOException {
        if (!"POST".equalsIgnoreCase(request.getMethod()) || !"/login".equals(request.getRequestURI())) {
            chain.doFilter(request, response);
            return;
        }

        String email = request.getParameter("username");
        if (email != null) {
            long verbleibend = bruteForceSchutz.istGesperrt(email);
            if (verbleibend > 0) {
                response.sendRedirect("/login?gesperrt=" + verbleibend);
                return;
            }
        }

        chain.doFilter(request, response);
    }
}

