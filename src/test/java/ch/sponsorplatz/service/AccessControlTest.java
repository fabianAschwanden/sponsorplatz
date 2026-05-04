package ch.sponsorplatz.service;

import ch.sponsorplatz.model.AppUser;
import ch.sponsorplatz.model.Rolle;
import ch.sponsorplatz.repository.AppUserRepository;
import ch.sponsorplatz.repository.MitgliedschaftRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.Collections;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AccessControlTest {

    private MitgliedschaftRepository mitgliedschaftRepository;
    private AppUserRepository appUserRepository;
    private AccessControl accessControl;

    private final UUID orgId = UUID.randomUUID();
    private final UUID userId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        mitgliedschaftRepository = mock(MitgliedschaftRepository.class);
        appUserRepository = mock(AppUserRepository.class);
        accessControl = new AccessControl(mitgliedschaftRepository, appUserRepository);
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
        when(mitgliedschaftRepository.existsByUserIdAndOrgIdAndRolleIn(eq(userId), eq(orgId), eq(rollen)))
                .thenReturn(existiert);
    }

    private void mockUserMitRolle(Authentication auth, Rolle rolle, boolean existiert) {
        AppUser user = new AppUser();
        user.setId(userId);
        when(appUserRepository.findByEmail(auth.getName())).thenReturn(Optional.of(user));
        when(mitgliedschaftRepository.existsByUserIdAndOrgIdAndRolle(eq(userId), eq(orgId), eq(rolle)))
                .thenReturn(existiert);
    }
}

