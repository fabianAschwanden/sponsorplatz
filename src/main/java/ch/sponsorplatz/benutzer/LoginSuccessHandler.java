package ch.sponsorplatz.benutzer;

import java.io.IOException;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler;

import ch.sponsorplatz.shared.config.LoginBruteForceSchutz;

/**
 * Custom Success-Handler: setzt Brute-Force-Zähler bei erfolgreichem Login
 * zurück und respektiert {@code savedRequest} — wenn der User auf eine
 * geschützte Seite (z.B. {@code /organisationen/x}) wollte und auf
 * {@code /login} umgeleitet wurde, landet er nach dem Login wieder dort.
 * Ohne savedRequest fällt er auf {@code /dashboard} zurück.
 *
 * <p>Liegt im Paket {@code benutzer/}, weil er User-Daten (Sprache aus
 * {@link AppUser}) liest. {@code shared/} darf nicht auf Feature-Pakete
 * zeigen (ARCH-07); deshalb wandert der Handler hierher, und
 * {@link BenutzerSecurityConfig} definiert das Bean.
 */
public class LoginSuccessHandler extends SavedRequestAwareAuthenticationSuccessHandler {

    private final LoginBruteForceSchutz bruteForceSchutz;
    private final AppUserRepository appUserRepository;

    public LoginSuccessHandler(LoginBruteForceSchutz bruteForceSchutz,
            AppUserRepository appUserRepository) {
        setDefaultTargetUrl("/dashboard");
        this.bruteForceSchutz = bruteForceSchutz;
        this.appUserRepository = appUserRepository;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication) throws IOException, ServletException {
        bruteForceSchutz.erfolgreichenLoginRegistrieren(authentication.getName());
        synchronisiereSprache(authentication, response);
        super.onAuthenticationSuccess(request, response, authentication);
    }

    private void synchronisiereSprache(Authentication auth, HttpServletResponse response) {
        if (appUserRepository == null) {
            return;
        }
        appUserRepository.findByEmail(auth.getName()).ifPresent(user -> {
            String sprache = user.getSprache();
            if (sprache != null && !sprache.isBlank()) {
                Cookie langCookie = new Cookie("lang", sprache.replace('_', '-'));
                langCookie.setPath("/");
                langCookie.setMaxAge(365 * 24 * 60 * 60);
                langCookie.setHttpOnly(false); // JS-Zugriff moeglich fuer UI
                response.addCookie(langCookie);
            }
        });
    }
}
