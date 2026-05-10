package ch.sponsorplatz.benutzer;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests für die drei Lookup-Stufen + Group-Mapping (Spec §6.1, §7.2).
 *
 * <p>Wir testen die {@code identifizierenOderAnlegen}-Methode direkt — der
 * {@code loadUser(OidcUserRequest)}-Override ruft sie via {@code super.loadUser}
 * + Adapter auf, hat aber Network-Calls drin (UserInfo-Endpoint), die im
 * Unit-Test schwer zu mocken wären. Der Integrations-Test
 * {@code OidcLoginFlowIT} (SSO-01/08/09) deckt den vollen Flow ab.
 */
@ExtendWith(MockitoExtension.class)
class SponsorplatzOidcUserServiceTest {

    @Mock private AppUserRepository appUserRepository;
    @Mock private FederierteIdentitaetRepository identitaetRepository;

    private SponsorplatzOidcUserService service;

    @BeforeEach
    void setUp() {
        service = new SponsorplatzOidcUserService(
                appUserRepository,
                identitaetRepository,
                Map.of(PlatformRolle.PLATFORM_ADMIN, "sponsorplatz-admins"));
    }

    @Test
    @DisplayName("SSO-02: Email-Match auf bestehenden AppUser → Verknüpfung erstellt")
    void emailMatchVerknuepftBestehenden() {
        AppUser bestehend = appUser("max@css.ch", "Max Muster");
        when(identitaetRepository.findByProviderAndSubject(IdentityProvider.ENTRA_ID, "subject-1"))
                .thenReturn(Optional.empty());
        when(appUserRepository.findByEmail("max@css.ch")).thenReturn(Optional.of(bestehend));
        when(appUserRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        AppUser ergebnis = service.identifizierenOderAnlegen(
                "subject-1", "max@css.ch", "Max Muster", List.of(),
                IdentityProvider.ENTRA_ID);

        assertThat(ergebnis).isEqualTo(bestehend);
        // Neue federierte_identitaet wurde gespeichert (Verknüpfung)
        ArgumentCaptor<FederierteIdentitaet> cap = ArgumentCaptor.forClass(FederierteIdentitaet.class);
        verify(identitaetRepository).save(cap.capture());
        assertThat(cap.getValue().getUser()).isEqualTo(bestehend);
        assertThat(cap.getValue().getSubject()).isEqualTo("subject-1");
        assertThat(cap.getValue().getProvider()).isEqualTo(IdentityProvider.ENTRA_ID);
    }

    @Test
    @DisplayName("SSO-03: Neuer User wird JIT erstellt mit emailVerifiziert=true, passwortHash leer")
    void neuerUserViaJit() {
        when(identitaetRepository.findByProviderAndSubject(IdentityProvider.ENTRA_ID, "subject-neu"))
                .thenReturn(Optional.empty());
        when(appUserRepository.findByEmail("neu@css.ch")).thenReturn(Optional.empty());
        when(appUserRepository.save(any())).thenAnswer(inv -> {
            AppUser u = inv.getArgument(0);
            u.setId(UUID.randomUUID());
            return u;
        });

        AppUser ergebnis = service.identifizierenOderAnlegen(
                "subject-neu", "neu@css.ch", "Neu User", List.of(),
                IdentityProvider.ENTRA_ID);

        assertThat(ergebnis.getEmail()).isEqualTo("neu@css.ch");
        assertThat(ergebnis.getAnzeigename()).isEqualTo("Neu User");
        assertThat(ergebnis.isEmailVerifiziert()).isTrue();
        assertThat(ergebnis.isAktiv()).isTrue();
        // passwortHash auf OIDC-Marker — verhindert Form-Login
        assertThat(ergebnis.getPasswortHash()).isEqualTo("OIDC-ONLY");
        verify(identitaetRepository).save(any());
    }

    @Test
    @DisplayName("SSO-04: Subsequent Login findet via (provider, subject), aktualisiert letzterLoginAm")
    void subsequentLoginAktualisiertTimestamp() {
        AppUser user = appUser("max@css.ch", "Max");
        FederierteIdentitaet identitaet = new FederierteIdentitaet();
        identitaet.setUser(user);
        identitaet.setProvider(IdentityProvider.ENTRA_ID);
        identitaet.setSubject("subject-1");
        when(identitaetRepository.findByProviderAndSubject(IdentityProvider.ENTRA_ID, "subject-1"))
                .thenReturn(Optional.of(identitaet));
        when(appUserRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        AppUser ergebnis = service.identifizierenOderAnlegen(
                "subject-1", "max@css.ch", "Max", List.of(),
                IdentityProvider.ENTRA_ID);

        assertThat(ergebnis).isEqualTo(user);
        assertThat(identitaet.getLetzterLoginAm()).isNotNull();
        // Email-Match-Pfad wird NICHT betreten
        verify(appUserRepository, never()).findByEmail(any());
    }

    @Test
    @DisplayName("SSO-06: groups-Claim mit konfigurierter Group → PLATFORM_ADMIN gesetzt")
    void groupMappingSetztPlatformAdmin() {
        AppUser user = appUser("admin@css.ch", "Admin");
        user.setPlatformRolle(null);
        FederierteIdentitaet identitaet = new FederierteIdentitaet();
        identitaet.setUser(user);
        identitaet.setProvider(IdentityProvider.ENTRA_ID);
        identitaet.setSubject("admin-sub");
        when(identitaetRepository.findByProviderAndSubject(IdentityProvider.ENTRA_ID, "admin-sub"))
                .thenReturn(Optional.of(identitaet));
        when(appUserRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        AppUser ergebnis = service.identifizierenOderAnlegen(
                "admin-sub", "admin@css.ch", "Admin",
                List.of("sponsorplatz-admins", "another-group"),
                IdentityProvider.ENTRA_ID);

        assertThat(ergebnis.getPlatformRolle()).isEqualTo(PlatformRolle.PLATFORM_ADMIN);
    }

    @Test
    @DisplayName("SSO-07: groups-Claim ohne konfigurierte Group → PLATFORM_ADMIN entzogen (Re-Sync)")
    void groupMappingEntziehtRolle() {
        AppUser user = appUser("admin@css.ch", "Admin");
        user.setPlatformRolle(PlatformRolle.PLATFORM_ADMIN); // war früher Admin
        FederierteIdentitaet identitaet = new FederierteIdentitaet();
        identitaet.setUser(user);
        identitaet.setProvider(IdentityProvider.ENTRA_ID);
        identitaet.setSubject("admin-sub");
        when(identitaetRepository.findByProviderAndSubject(IdentityProvider.ENTRA_ID, "admin-sub"))
                .thenReturn(Optional.of(identitaet));
        when(appUserRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        AppUser ergebnis = service.identifizierenOderAnlegen(
                "admin-sub", "admin@css.ch", "Admin",
                List.of("only-irrelevant-groups"),
                IdentityProvider.ENTRA_ID);

        assertThat(ergebnis.getPlatformRolle()).isNull();
    }

    private AppUser appUser(String email, String anzeigename) {
        AppUser u = new AppUser();
        u.setId(UUID.randomUUID());
        u.setEmail(email);
        u.setAnzeigename(anzeigename);
        u.setPasswortHash("$2a$test");
        return u;
    }
}
