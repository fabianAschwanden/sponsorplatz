package ch.sponsorplatz.benutzer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Bridge zwischen Spring Security OIDC und Sponsorplatz-{@link AppUser}.
 *
 * <p>Drei Lookup-Stufen (Spec §6.1):
 * <ol>
 *   <li>{@code (provider, subject)} — stabiler IdP-Identifier</li>
 *   <li>Email-Match auf bestehenden AppUser → automatische Verknüpfung
 *       (sicher in Single-Tenant-Setup, siehe Spec §6.3)</li>
 *   <li>Just-in-Time-Provisionierung neuer AppUser</li>
 * </ol>
 *
 * <p>Group-Mapping (Spec §7.2): bei jedem Login werden die {@code groups}-
 * Claims des ID-Tokens gegen die konfigurierte Map ausgewertet — Treffer setzt
 * die {@link PlatformRolle}, kein Treffer zieht eine zuvor gesetzte Rolle ein
 * (Re-Sync, damit entzogene Group-Membership wirkt).
 *
 * @see <a href="../../../../../specs/AUTH_SSO_OIDC.md">AUTH_SSO_OIDC.md</a>
 */
@Service
@Transactional
public class SponsorplatzOidcUserService extends OidcUserService {

    private static final Logger log = LoggerFactory.getLogger(SponsorplatzOidcUserService.class);

    /** Marker im passwortHash für SSO-Only-User — verhindert Form-Login (Spec §6.2). */
    public static final String OIDC_ONLY_PASSWORT_MARKER = "OIDC-ONLY";

    private final AppUserRepository appUserRepository;
    private final FederierteIdentitaetRepository identitaetRepository;
    /** Map<PlatformRolle, Entra-Group-Name>. Leer → kein Group-Mapping aktiv. */
    private final Map<PlatformRolle, String> rollenMapping;
    /**
     * Erlaubte Email-Domains für OIDC-Logins. Leer → keine Beschränkung
     * (Backward-Compat). Gefüllt → jeder Login (auch bestehender User per
     * Email-Match) muss eine Email mit einer dieser Domains haben — schützt
     * vor Account-Takeover in Multi-Tenant-IdPs (Spec §6.3).
     */
    private final Set<String> emailDomainWhitelist;

    public SponsorplatzOidcUserService(
            AppUserRepository appUserRepository,
            FederierteIdentitaetRepository identitaetRepository,
            @Qualifier("oidcRollenMapping") Map<PlatformRolle, String> rollenMapping,
            @Qualifier("oidcEmailDomainWhitelist") Set<String> emailDomainWhitelist) {
        this.appUserRepository = appUserRepository;
        this.identitaetRepository = identitaetRepository;
        this.rollenMapping = rollenMapping;
        this.emailDomainWhitelist = emailDomainWhitelist;
    }

    @Override
    public OidcUser loadUser(OidcUserRequest userRequest) {
        // Spring's OidcUserService verifiziert ID-Token (JWKS-Signatur, Issuer,
        // Audience, Expiry, Nonce) — ist der das durchläuft, ist der Token valid.
        OidcUser oidc = super.loadUser(userRequest);
        IdentityProvider provider = resolveProvider(userRequest.getClientRegistration().getRegistrationId());

        AppUser user = identifizierenOderAnlegen(
                oidc.getSubject(),
                oidc.getEmail(),
                oidc.getFullName(),
                extrahiereGroups(oidc),
                provider);

        // 'email' als nameAttributeKey: Spring-Default ist 'sub' (IdP-interne ID).
        // Der Rest der App nutzt aber 'authentication.getName()' als Email-Lookup-
        // Schlüssel (CurrentUserAdvice, LoginSuccessHandler, EinstellungenController
        // etc.) — mit 'sub' würde dort 'Benutzer nicht gefunden' fliegen.
        // Voraussetzung: Provider liefert 'email' im ID-Token (gilt für alle in
        // IdentityProvider registrierten — scope=openid,profile,email).
        return new DefaultOidcUser(
                buildAuthorities(user),
                oidc.getIdToken(),
                oidc.getUserInfo(),
                "email");
    }

    /**
     * Mappt die Spring-{@code registrationId} (der Property-Key in
     * {@code spring.security.oauth2.client.registration.X}) auf den
     * {@link IdentityProvider}-Enum-Wert. Erlaubt mehrere Provider mit
     * konsistentem Mapping — case-insensitive Match.
     *
     * <p>Unbekannte registrationIds werfen {@link OAuth2AuthenticationException}
     * — verhindert dass ein versehentlich registrierter Provider ohne Enum-
     * Eintrag Login-Versuche mit unbekanntem Provider in der DB landen lässt.
     */
    private IdentityProvider resolveProvider(String registrationId) {
        String normalisiert = registrationId.toUpperCase(Locale.ROOT).replace('-', '_');
        // Spring-übliche Kurz-IDs: 'entra' → ENTRA_ID, 'edu' → EDU_ID
        if (normalisiert.equals("ENTRA")) return IdentityProvider.ENTRA_ID;
        if (normalisiert.equals("EDU"))   return IdentityProvider.EDU_ID;
        try {
            return IdentityProvider.valueOf(normalisiert);
        } catch (IllegalArgumentException ex) {
            log.warn("OIDC-Login mit unbekanntem registrationId '{}' — kein Mapping auf IdentityProvider", registrationId);
            throw new OAuth2AuthenticationException(new OAuth2Error("server_error",
                    "OIDC-Provider '" + registrationId + "' ist nicht in IdentityProvider-Enum registriert.",
                    null));
        }
    }

    /**
     * Kern-Mapping-Logik — direkt aufrufbar im Unit-Test ohne Network-Call.
     * Persistiert {@link AppUser} und {@link FederierteIdentitaet} bei Bedarf
     * und gibt den (möglicherweise frisch angelegten) AppUser zurück.
     */
    public AppUser identifizierenOderAnlegen(String subject, String email, String name,
                                             List<String> groups, IdentityProvider provider) {
        pruefeWhitelist(email);

        // Stufe 1: stabiler Lookup via (provider, subject)
        Optional<FederierteIdentitaet> bestehende =
                identitaetRepository.findByProviderAndSubject(provider, subject);
        if (bestehende.isPresent()) {
            FederierteIdentitaet identitaet = bestehende.get();
            identitaet.setLetzterLoginAm(Instant.now());
            identitaetRepository.save(identitaet);
            AppUser user = identitaet.getUser();
            wendeGroupMappingAn(user, groups);
            return appUserRepository.save(user);
        }

        // Stufe 2: Email-Match auf bestehenden AppUser → Verknüpfung
        Optional<AppUser> ueberEmail = appUserRepository.findByEmail(email);
        if (ueberEmail.isPresent()) {
            AppUser user = ueberEmail.get();
            verknuepfeIdentitaet(user, provider, subject, email);
            wendeGroupMappingAn(user, groups);
            return appUserRepository.save(user);
        }

        // Stufe 3: JIT-Provisionierung
        AppUser neu = jitProvisionieren(email, name);
        AppUser gespeichert = appUserRepository.save(neu);
        verknuepfeIdentitaet(gespeichert, provider, subject, email);
        wendeGroupMappingAn(gespeichert, groups);
        return appUserRepository.save(gespeichert);
    }

    private AppUser jitProvisionieren(String email, String name) {
        AppUser neu = new AppUser();
        neu.setEmail(email);
        neu.setAnzeigename(name != null ? name : email);
        neu.setPasswortHash(OIDC_ONLY_PASSWORT_MARKER);
        neu.setAktiv(true);
        neu.setEmailVerifiziert(true); // Entra hat die Email schon validiert
        log.info("OIDC JIT-Provisioning: neuer AppUser für {}", email);
        return neu;
    }

    private void verknuepfeIdentitaet(AppUser user, IdentityProvider provider,
                                       String subject, String email) {
        FederierteIdentitaet identitaet = new FederierteIdentitaet();
        identitaet.setUser(user);
        identitaet.setProvider(provider);
        identitaet.setSubject(subject);
        identitaet.setEmailAtProvider(email);
        identitaet.setLetzterLoginAm(Instant.now());
        identitaetRepository.save(identitaet);
        log.info("OIDC Identitaet verknuepft: user={}, provider={}, subject={}",
                user.getEmail(), provider, subject);
    }

    /**
     * Setzt {@link PlatformRolle} basierend auf den Entra-Groups. Bei jedem
     * Login wird neu evaluiert — entzogene Group-Membership zieht die Rolle.
     */
    private void wendeGroupMappingAn(AppUser user, List<String> groups) {
        if (rollenMapping.isEmpty()) {
            return;
        }
        Set<String> groupSet = new HashSet<>(groups);
        PlatformRolle gefunden = null;
        for (Map.Entry<PlatformRolle, String> e : rollenMapping.entrySet()) {
            if (groupSet.contains(e.getValue())) {
                gefunden = e.getKey();
                break;
            }
        }
        user.setPlatformRolle(gefunden);
    }

    @SuppressWarnings("unchecked")
    private List<String> extrahiereGroups(OidcUser oidc) {
        Object claim = oidc.getClaim("groups");
        if (claim instanceof List<?>) {
            return ((List<Object>) claim).stream().map(Object::toString).toList();
        }
        return List.of();
    }

    /**
     * Wirft {@link OAuth2AuthenticationException} wenn die Whitelist aktiv ist
     * und die Email-Domain nicht in der Allowlist steht. Greift vor allen
     * Lookups — auch bestehende AppUser werden über die Whitelist geschützt
     * (Account-Takeover-Mitigation, Spec §6.3).
     */
    private void pruefeWhitelist(String email) {
        if (emailDomainWhitelist.isEmpty()) {
            return;
        }
        if (email == null) {
            throw new OAuth2AuthenticationException(new OAuth2Error("access_denied",
                    "OIDC-Login ohne Email-Claim — Whitelist-Check nicht möglich.", null));
        }
        int at = email.lastIndexOf('@');
        if (at <= 0 || at == email.length() - 1) {
            throw new OAuth2AuthenticationException(new OAuth2Error("access_denied",
                    "OIDC-Email '" + email + "' ohne Domain — Whitelist-Check fehlgeschlagen.", null));
        }
        String domain = email.substring(at + 1).toLowerCase(Locale.ROOT);
        if (!emailDomainWhitelist.contains(domain)) {
            log.warn("OIDC-Login verweigert: Email-Domain '{}' nicht in Whitelist {}",
                    domain, emailDomainWhitelist);
            throw new OAuth2AuthenticationException(new OAuth2Error("access_denied",
                    "OIDC-Email-Domain '" + domain + "' ist für diese Plattform nicht freigeschaltet.",
                    null));
        }
    }

    private Set<GrantedAuthority> buildAuthorities(AppUser user) {
        Set<GrantedAuthority> authorities = new HashSet<>();
        authorities.add(new SimpleGrantedAuthority("ROLE_USER"));
        if (user.getPlatformRolle() != null) {
            authorities.add(new SimpleGrantedAuthority("ROLE_" + user.getPlatformRolle().name()));
        }
        return authorities;
    }
}
