package ch.sponsorplatz.benutzer;

/**
 * Identity-Provider für föderierte Anmeldung.
 *
 * <p>Initial nur Entra ID (Microsoft). Erweiterung verlangt:
 * <ol>
 *   <li>Neuen Enum-Wert hier</li>
 *   <li>Migration: {@code chk_provider}-CHECK-Constraint erweitern</li>
 *   <li>Spring-Properties: neue {@code spring.security.oauth2.client.registration.X}-Sektion</li>
 *   <li>Mapping-Logik in {@code SponsorplatzOidcUserService}</li>
 * </ol>
 *
 * @see <a href="../../../../../specs/AUTH_SSO_OIDC.md">AUTH_SSO_OIDC.md §5</a>
 */
public enum IdentityProvider {
    /** Microsoft Entra ID (vormals Azure AD). */
    ENTRA_ID
}
