package ch.sponsorplatz.benutzer;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.EnumMap;
import java.util.Map;

/**
 * Konfiguration für OIDC-Group → PlatformRolle-Mapping.
 *
 * <p>Properties (alle optional, leer = kein Group-Mapping):
 * <pre>
 * sponsorplatz.oidc.rollen-mapping.PLATFORM_ADMIN=sponsorplatz-admins
 * sponsorplatz.oidc.rollen-mapping.PLATFORM_MODERATOR=sponsorplatz-moderatoren
 * sponsorplatz.oidc.rollen-mapping.PLATFORM_SUPPORT=sponsorplatz-support
 * </pre>
 *
 * @see <a href="../../../../../specs/AUTH_SSO_OIDC.md">AUTH_SSO_OIDC.md §7.2</a>
 */
@Configuration
public class OidcConfig {

    @Bean(name = "oidcRollenMapping")
    public Map<PlatformRolle, String> oidcRollenMapping(
            @Value("${sponsorplatz.oidc.rollen-mapping.PLATFORM_ADMIN:}") String adminGroup,
            @Value("${sponsorplatz.oidc.rollen-mapping.PLATFORM_MODERATOR:}") String moderatorGroup,
            @Value("${sponsorplatz.oidc.rollen-mapping.PLATFORM_SUPPORT:}") String supportGroup) {
        Map<PlatformRolle, String> mapping = new EnumMap<>(PlatformRolle.class);
        if (!adminGroup.isBlank()) mapping.put(PlatformRolle.PLATFORM_ADMIN, adminGroup);
        if (!moderatorGroup.isBlank()) mapping.put(PlatformRolle.PLATFORM_MODERATOR, moderatorGroup);
        if (!supportGroup.isBlank()) mapping.put(PlatformRolle.PLATFORM_SUPPORT, supportGroup);
        return mapping;
    }
}
