package ch.sponsorplatz.service;

import com.oracle.bmc.Region;
import com.oracle.bmc.auth.AbstractAuthenticationDetailsProvider;
import com.oracle.bmc.auth.ConfigFileAuthenticationDetailsProvider;
import com.oracle.bmc.auth.InstancePrincipalsAuthenticationDetailsProvider;
import com.oracle.bmc.objectstorage.ObjectStorageClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;

/**
 * Wiring für den OCI-{@link ObjectStorageClient} — nur aktiv, wenn
 * {@code sponsorplatz.storage.provider=oci}.
 *
 * <p>Auth-Modi (Property {@code sponsorplatz.storage.oci.auth-mode}):
 * <ul>
 *   <li>{@code instance} (Default) — Instance Principal, läuft auf OCI-VMs ohne
 *       Credentials. Empfohlen für Prod.</li>
 *   <li>{@code config} — liest {@code ~/.oci/config} (Default-Profil). Für lokales
 *       Testen mit OCI-CLI-Setup.</li>
 * </ul>
 */
@Configuration
@ConditionalOnProperty(name = "sponsorplatz.storage.provider", havingValue = "oci")
public class OciStorageConfig {

    private static final Logger log = LoggerFactory.getLogger(OciStorageConfig.class);

    @Bean
    AbstractAuthenticationDetailsProvider authenticationDetailsProvider(
            @Value("${sponsorplatz.storage.oci.auth-mode:instance}") String authMode) throws IOException {
        return switch (authMode.toLowerCase()) {
            case "instance" -> {
                log.info("OCI Auth: Instance Principal");
                yield InstancePrincipalsAuthenticationDetailsProvider.builder().build();
            }
            case "config" -> {
                log.info("OCI Auth: Config-File ~/.oci/config (DEFAULT)");
                yield new ConfigFileAuthenticationDetailsProvider("DEFAULT");
            }
            default -> throw new IllegalArgumentException(
                    "Unbekannter sponsorplatz.storage.oci.auth-mode: " + authMode + " (erwartet: instance|config)");
        };
    }

    @Bean(destroyMethod = "close")
    ObjectStorageClient objectStorageClient(
            AbstractAuthenticationDetailsProvider auth,
            @Value("${sponsorplatz.storage.oci.region:}") String region) {
        ObjectStorageClient.Builder builder = ObjectStorageClient.builder();
        if (region != null && !region.isBlank()) {
            builder.region(Region.fromRegionCodeOrId(region));
        }
        return builder.build(auth);
    }
}
