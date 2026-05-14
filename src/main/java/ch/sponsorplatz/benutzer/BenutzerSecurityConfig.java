package ch.sponsorplatz.benutzer;

import ch.sponsorplatz.shared.config.LoginBruteForceSchutz;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;

/**
 * Bean-Definitionen, die {@code benutzer/}-Klassen für das Sicherheits-Setup
 * der Plattform exposen. Liegt im Feature-Package, damit
 * {@link ch.sponsorplatz.shared.config.SecurityConfig} keine direkten
 * Abhängigkeiten auf {@link LoginSuccessHandler}/{@link AppUserRepository}
 * mehr braucht (ARCH-07: shared darf nicht auf Features zeigen).
 *
 * <p>SecurityConfig injiziert das Bean als Spring-Interface-Typ
 * ({@link AuthenticationSuccessHandler}) und kennt unsere konkrete
 * Implementierung damit nicht mehr.
 */
@Configuration
public class BenutzerSecurityConfig {

    @Bean
    public AuthenticationSuccessHandler loginSuccessHandler(LoginBruteForceSchutz bruteForceSchutz,
            AppUserRepository appUserRepository) {
        return new LoginSuccessHandler(bruteForceSchutz, appUserRepository);
    }
}
