package ch.sponsorplatz.benutzer;

/**
 * Identity-Provider für föderierte Anmeldung.
 *
 * <p>Neuer Provider hinzufügen:
 * <ol>
 *   <li>Neuen Enum-Wert hier</li>
 *   <li>Spring-Properties: neue {@code spring.security.oauth2.client.registration.X}-Sektion</li>
 *   <li>{@code SponsorplatzOidcUserService.loadUser} ggf. Provider-Detection
 *       erweitern (aktuell: registrationId → IdentityProvider-Mapping)</li>
 * </ol>
 *
 * <p>Hinweis: Seit V46 gibt es keinen {@code CHECK}-Constraint mehr — der
 * Enum ist die alleinige Source of Truth (analog Pattern V44/V45).
 *
 * @see <a href="../../../../../specs/AUTH_SSO_OIDC.md">AUTH_SSO_OIDC.md §5</a>
 */
public enum IdentityProvider {
    /** Microsoft Entra ID (vormals Azure AD) — Corporate-User mit CSS-Tenant. */
    ENTRA_ID,
    /** Google Workspace — Verbands-/Schul-Partner mit Google-Konto. */
    GOOGLE,
    /** SwissID — Schweizer Bürger-Identität (UID/AHV-validiert). */
    SWISSID,
    /** Switch edu-ID — Schweizer Hochschul-Verbund (Universitäten, FH). */
    EDU_ID
}
