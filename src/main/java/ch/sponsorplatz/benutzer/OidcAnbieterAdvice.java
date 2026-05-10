package ch.sponsorplatz.benutzer;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import java.util.List;
import java.util.Map;

/**
 * Stellt für die Login-Seite die Liste der konfigurierten OAuth2-Anbieter
 * bereit. Wenn keine Registration existiert (z.B. dev ohne Entra-Konfig),
 * wird eine leere Liste geliefert — Template versteckt den SSO-Bereich dann.
 *
 * <p>Format pro Eintrag: {@code Map<String,String>} mit Keys
 * {@code registrationId} und {@code clientName}. Der Link wird im Template
 * gebaut: {@code /oauth2/authorization/{registrationId}}.
 *
 * @see <a href="../../../../../specs/AUTH_SSO_OIDC.md">AUTH_SSO_OIDC.md §3.2</a>
 */
@ControllerAdvice(basePackages = "ch.sponsorplatz")
public class OidcAnbieterAdvice {

    private final ObjectProvider<ClientRegistrationRepository> repoProvider;

    public OidcAnbieterAdvice(ObjectProvider<ClientRegistrationRepository> repoProvider) {
        this.repoProvider = repoProvider;
    }

    @ModelAttribute("oauth2Anbieter")
    public List<Map<String, String>> oauth2Anbieter() {
        ClientRegistrationRepository repo = repoProvider.getIfAvailable();
        if (!(repo instanceof InMemoryClientRegistrationRepository inMemory)) {
            return List.of();
        }
        // InMemoryClientRegistrationRepository ist Iterable<ClientRegistration>
        return java.util.stream.StreamSupport.stream(inMemory.spliterator(), false)
                .map(this::zuMap)
                .toList();
    }

    private Map<String, String> zuMap(ClientRegistration reg) {
        String anzeige = reg.getClientName() != null ? reg.getClientName() : reg.getRegistrationId();
        return Map.of(
                "registrationId", reg.getRegistrationId(),
                "clientName", anzeige);
    }
}
