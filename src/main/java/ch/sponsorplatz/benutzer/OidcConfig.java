package ch.sponsorplatz.benutzer;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

/**
 * Konfiguration für OIDC-Group → PlatformRolle-Mapping.
 *
 * <p>Properties (alle optional, leere Map = kein Group-Mapping):
 * <pre>
 * sponsorplatz.oidc.rollen-mapping.PLATFORM_ADMIN=sponsorplatz-admins
 * sponsorplatz.oidc.rollen-mapping.PLATFORM_MODERATOR=sponsorplatz-moderatoren
 * sponsorplatz.oidc.rollen-mapping.PLATFORM_SUPPORT=sponsorplatz-support
 * </pre>
 *
 * @see <a href="../../../../../specs/AUTH_SSO_OIDC.md">AUTH_SSO_OIDC.md §7.2</a>
 */
@Configuration
@EnableConfigurationProperties(OidcConfig.OidcProperties.class)
public class OidcConfig {

    /**
     * Properties-Holder. {@code Map<String,String>} statt {@code Map<PlatformRolle,String>},
     * damit unbekannte Keys (Tippfehler in Config) den App-Start nicht brechen —
     * die Enum-Konvertierung passiert kontrolliert im Bean unten.
     */
    @ConfigurationProperties(prefix = "sponsorplatz.oidc")
    public static class OidcProperties {
        private Map<String, String> rollenMapping = new HashMap<>();

        public Map<String, String> getRollenMapping() {
            return rollenMapping;
        }

        public void setRollenMapping(Map<String, String> rollenMapping) {
            this.rollenMapping = rollenMapping;
        }
    }

    @Bean(name = "oidcRollenMapping")
    public Map<PlatformRolle, String> oidcRollenMapping(OidcProperties props) {
        Map<PlatformRolle, String> mapping = new EnumMap<>(PlatformRolle.class);
        for (Map.Entry<String, String> e : props.getRollenMapping().entrySet()) {
            String wert = e.getValue();
            if (wert == null || wert.isBlank()) continue;
            try {
                mapping.put(PlatformRolle.valueOf(e.getKey().toUpperCase()), wert);
            } catch (IllegalArgumentException ignored) {
                // Property-Key ist keine valide PlatformRolle — überspringen,
                // damit ein Tippfehler in der Config den Boot nicht abbricht.
            }
        }
        return mapping;
    }
}
