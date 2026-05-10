package ch.sponsorplatz.benutzer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.HashSet;
import java.util.List;
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

    public SponsorplatzOidcUserService(
            AppUserRepository appUserRepository,
            FederierteIdentitaetRepository identitaetRepository,
            @Qualifier("oidcRollenMapping") Map<PlatformRolle, String> rollenMapping) {
        this.appUserRepository = appUserRepository;
        this.identitaetRepository = identitaetRepository;
        this.rollenMapping = rollenMapping;
    }

    @Override
    public OidcUser loadUser(OidcUserRequest userRequest) {
        // Spring's OidcUserService verifiziert ID-Token (JWKS-Signatur, Issuer,
        // Audience, Expiry, Nonce) — ist der das durchläuft, ist der Token valid.
        OidcUser oidc = super.loadUser(userRequest);

        AppUser user = identifizierenOderAnlegen(
                oidc.getSubject(),
                oidc.getEmail(),
                oidc.getFullName(),
                extrahiereGroups(oidc),
                IdentityProvider.ENTRA_ID);

        return new DefaultOidcUser(buildAuthorities(user), oidc.getIdToken(), oidc.getUserInfo());
    }

    /**
     * Kern-Mapping-Logik — direkt aufrufbar im Unit-Test ohne Network-Call.
     * Persistiert {@link AppUser} und {@link FederierteIdentitaet} bei Bedarf
     * und gibt den (möglicherweise frisch angelegten) AppUser zurück.
     */
    public AppUser identifizierenOderAnlegen(String subject, String email, String name,
                                             List<String> groups, IdentityProvider provider) {
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

    private Set<GrantedAuthority> buildAuthorities(AppUser user) {
        Set<GrantedAuthority> authorities = new HashSet<>();
        authorities.add(new SimpleGrantedAuthority("ROLE_USER"));
        if (user.getPlatformRolle() != null) {
            authorities.add(new SimpleGrantedAuthority("ROLE_" + user.getPlatformRolle().name()));
        }
        return authorities;
    }
}
