package ch.sponsorplatz.shared.config;

import java.io.IOException;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler;

import ch.sponsorplatz.benutzer.AppUserRepository;

/**
 * Custom Success-Handler: setzt Brute-Force-Zähler bei erfolgreichem Login
 * zurück und respektiert {@code savedRequest} — wenn der User auf eine
 * geschützte Seite (z.B. {@code /organisationen/x}) wollte und auf
 * {@code /login}
 * umgeleitet wurde, landet er nach dem Login wieder dort. Ohne savedRequest
 * fällt er auf {@code /dashboard} zurück.
 */
public class LoginSuccessHandler extends SavedRequestAwareAuthenticationSuccessHandler {

    private final LoginBruteForceSchutz bruteForceSchutz;
    private final ObjectProvider<AppUserRepository> appUserRepositoryProvider;

    public LoginSuccessHandler(LoginBruteForceSchutz bruteForceSchutz,
            ObjectProvider<AppUserRepository> appUserRepositoryProvider) {
        setDefaultTargetUrl("/dashboard");
        this.bruteForceSchutz = bruteForceSchutz;
        this.appUserRepositoryProvider = appUserRepositoryProvider;
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
        AppUserRepository repo = appUserRepositoryProvider.getIfAvailable();
        if (repo == null)
            return;

        repo.findByEmail(auth.getName()).ifPresent(user -> {
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
