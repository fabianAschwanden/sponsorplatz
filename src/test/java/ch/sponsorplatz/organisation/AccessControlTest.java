package ch.sponsorplatz.organisation;

import ch.sponsorplatz.benutzer.AppUser;
import ch.sponsorplatz.benutzer.AppUserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.Collections;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AccessControlTest {

    private MitgliedschaftRepository mitgliedschaftRepository;
    private AppUserRepository appUserRepository;
    private OrganisationRepository organisationRepository;
    private AccessControl accessControl;

    private final UUID orgId = UUID.randomUUID();
    private final UUID userId = UUID.randomUUID();
    private final String orgSlug = "fc-test";

    @BeforeEach
    void setUp() {
        mitgliedschaftRepository = mock(MitgliedschaftRepository.class);
        appUserRepository = mock(AppUserRepository.class);
        organisationRepository = mock(OrganisationRepository.class);
        accessControl = new AccessControl(mitgliedschaftRepository, appUserRepository, organisationRepository);
    }

    /** AC-01: nicht eingeloggt (null auth) → kannOrgEditieren false. */
    @Test
    void nichtEingeloggtKannNichtEditieren() {
        assertThat(accessControl.kannOrgEditieren(orgId, null)).isFalse();
    }

    /** AC-02: PLATFORM_ADMIN → kannOrgEditieren immer true. */
    @Test
    void plattformAdminKannImmerEditieren() {
        Authentication auth = authMitRolle("ROLE_PLATFORM_ADMIN", "admin@example.com");
        assertThat(accessControl.kannOrgEditieren(orgId, auth)).isTrue();
    }

    /** AC-03: ORG_EDITOR → kannOrgEditieren true. */
    @Test
    void orgEditorKannEditieren() {
        Authentication auth = authOhneRolle("editor@example.com");
        mockUserMitMitgliedschaft(auth, Set.of(Rolle.ORG_OWNER, Rolle.ORG_EDITOR), true);
        assertThat(accessControl.kannOrgEditieren(orgId, auth)).isTrue();
    }

    /** AC-04: ORG_VIEWER → kannOrgEditieren false. */
    @Test
    void orgViewerKannNichtEditieren() {
        Authentication auth = authOhneRolle("viewer@example.com");
        mockUserMitMitgliedschaft(auth, Set.of(Rolle.ORG_OWNER, Rolle.ORG_EDITOR), false);
        assertThat(accessControl.kannOrgEditieren(orgId, auth)).isFalse();
    }

    /** AC-05: ORG_OWNER → kannOrgVerwalten true. */
    @Test
    void orgOwnerKannVerwalten() {
        Authentication auth = authOhneRolle("owner@example.com");
        mockUserMitRolle(auth, Rolle.ORG_OWNER, true);
        assertThat(accessControl.kannOrgVerwalten(orgId, auth)).isTrue();
    }

    /** AC-06: ORG_EDITOR → kannOrgVerwalten false. */
    @Test
    void orgEditorKannNichtVerwalten() {
        Authentication auth = authOhneRolle("editor@example.com");
        mockUserMitRolle(auth, Rolle.ORG_OWNER, false);
        assertThat(accessControl.kannOrgVerwalten(orgId, auth)).isFalse();
    }

    /** AC-07: PLATFORM_ADMIN → kannOrgVerwalten true. */
    @Test
    void plattformAdminKannVerwalten() {
        Authentication auth = authMitRolle("ROLE_PLATFORM_ADMIN", "admin@example.com");
        assertThat(accessControl.kannOrgVerwalten(orgId, auth)).isTrue();
    }

    /** AC-08: kein Mitglied dieser Org → kannOrgEditieren false. */
    @Test
    void keinMitgliedDieserOrgKannNichtEditieren() {
        Authentication auth = authOhneRolle("fremd@example.com");
        mockUserMitMitgliedschaft(auth, Set.of(Rolle.ORG_OWNER, Rolle.ORG_EDITOR), false);
        assertThat(accessControl.kannOrgEditieren(orgId, auth)).isFalse();
    }

    /** AC-09: kannOrgEditierenNachSlug delegiert für bekannten Slug auf UUID-Variante. */
    @Test
    void editierenNachSlugDelegiertFuerBekanntenSlug() {
        Authentication auth = authOhneRolle("editor@example.com");
        mockOrgMitSlug(orgSlug, orgId);
        mockUserMitMitgliedschaft(auth, Set.of(Rolle.ORG_OWNER, Rolle.ORG_EDITOR), true);

        assertThat(accessControl.kannOrgEditierenNachSlug(orgSlug, auth)).isTrue();
    }

    /** AC-10: kannOrgEditierenNachSlug für unbekannten Slug → false (kein Throw). */
    @Test
    void editierenNachSlugFalseFuerUnbekanntenSlug() {
        Authentication auth = authOhneRolle("user@example.com");
        when(organisationRepository.findBySlug("gibts-nicht")).thenReturn(Optional.empty());

        assertThat(accessControl.kannOrgEditierenNachSlug("gibts-nicht", auth)).isFalse();
    }

    /** AC-11: kannOrgVerwaltenNachSlug delegiert für bekannten Slug auf UUID-Variante. */
    @Test
    void verwaltenNachSlugDelegiertFuerBekanntenSlug() {
        Authentication auth = authOhneRolle("owner@example.com");
        mockOrgMitSlug(orgSlug, orgId);
        mockUserMitRolle(auth, Rolle.ORG_OWNER, true);

        assertThat(accessControl.kannOrgVerwaltenNachSlug(orgSlug, auth)).isTrue();
    }

    /** AC-12: kannOrgVerwaltenNachSlug für unbekannten Slug → false. */
    @Test
    void verwaltenNachSlugFalseFuerUnbekanntenSlug() {
        Authentication auth = authOhneRolle("user@example.com");
        when(organisationRepository.findBySlug("gibts-nicht")).thenReturn(Optional.empty());

        assertThat(accessControl.kannOrgVerwaltenNachSlug("gibts-nicht", auth)).isFalse();
    }

    // --- Hilfs-Methoden ---

    @SuppressWarnings("unchecked")
    private Authentication authMitRolle(String rolle, String email) {
        Authentication auth = mock(Authentication.class);
        when(auth.isAuthenticated()).thenReturn(true);
        when(auth.getName()).thenReturn(email);
        when(auth.getAuthorities()).thenAnswer(inv ->
                Collections.singletonList(new SimpleGrantedAuthority(rolle)));
        return auth;
    }

    @SuppressWarnings("unchecked")
    private Authentication authOhneRolle(String email) {
        Authentication auth = mock(Authentication.class);
        when(auth.isAuthenticated()).thenReturn(true);
        when(auth.getName()).thenReturn(email);
        when(auth.getAuthorities()).thenAnswer(inv -> Collections.emptyList());
        return auth;
    }

    private void mockUserMitMitgliedschaft(Authentication auth, Set<Rolle> rollen, boolean existiert) {
        AppUser user = new AppUser();
        user.setId(userId);
        when(appUserRepository.findByEmail(auth.getName())).thenReturn(Optional.of(user));
        Set<String> rollenAlsString = rollen.stream().map(Rolle::name).collect(Collectors.toSet());
        when(mitgliedschaftRepository.zaehleMitgliedschaftenInHierarchie(
                eq(userId), eq(orgId), argThat(c -> c != null && c.containsAll(rollenAlsString))))
                .thenReturn(existiert ? 1L : 0L);
    }

    private void mockUserMitRolle(Authentication auth, Rolle rolle, boolean existiert) {
        AppUser user = new AppUser();
        user.setId(userId);
        when(appUserRepository.findByEmail(auth.getName())).thenReturn(Optional.of(user));
        when(mitgliedschaftRepository.zaehleMitgliedschaftenInHierarchie(
                eq(userId), eq(orgId), argThat(c -> c != null && c.contains(rolle.name()))))
                .thenReturn(existiert ? 1L : 0L);
    }

    private void mockOrgMitSlug(String slug, UUID id) {
        Organisation org = new Organisation();
        org.setId(id);
        org.setSlug(slug);
        org.setName("Test-Org");
        org.setTyp(OrgTyp.VEREIN);
        when(organisationRepository.findBySlug(slug)).thenReturn(Optional.of(org));
    }

    // --- Vererbungs-Tests ---

    /**
     * AC-HIER-01: Owner auf Eltern-Org → kann Kind-Org editieren.
     * Der rekursive CTE-Query findet die Mitgliedschaft auf der Eltern-Org
     * obwohl der Auth-Check für die Kind-Org-ID läuft.
     */
    @Test
    void elternOwnerKannKindEditieren() {
        UUID kindOrgId = UUID.randomUUID();
        Authentication auth = authMitRolle("ROLE_USER", "editor@sp.ch");
        AppUser user = new AppUser();
        user.setId(userId);
        when(appUserRepository.findByEmail("editor@sp.ch")).thenReturn(Optional.of(user));

        // CTE traversiert Kind→Eltern, findet Mitgliedschaft auf Eltern
        when(mitgliedschaftRepository.zaehleMitgliedschaftenInHierarchie(
                eq(userId), eq(kindOrgId), any()))
                .thenReturn(1L);

        assertThat(accessControl.kannOrgEditieren(kindOrgId, auth)).isTrue();
    }

    /**
     * AC-HIER-02: Owner einer FREMDEN Hierarchie → kein Zugriff.
     * Der CTE läuft die Eltern-Kette des Kindes hoch, findet aber keine
     * Mitgliedschaft, weil der User in einem komplett separaten Konzern-Baum
     * Mitglied ist. Negative Vererbungs-Grenze.
     */
    @Test
    void ownerFremderHierarchieHatKeinenZugriff() {
        UUID kindOrgId = UUID.randomUUID();
        Authentication auth = authMitRolle("ROLE_USER", "fremder-owner@sp.ch");
        AppUser user = new AppUser();
        user.setId(userId);
        when(appUserRepository.findByEmail("fremder-owner@sp.ch")).thenReturn(Optional.of(user));

        // CTE traversiert die Eltern-Kette des Kindes, der User ist in keiner
        // Org dieser Kette Mitglied (auch wenn er anderswo Owner ist)
        when(mitgliedschaftRepository.zaehleMitgliedschaftenInHierarchie(
                eq(userId), eq(kindOrgId), any()))
                .thenReturn(0L);

        assertThat(accessControl.kannOrgEditieren(kindOrgId, auth)).isFalse();
    }
}

