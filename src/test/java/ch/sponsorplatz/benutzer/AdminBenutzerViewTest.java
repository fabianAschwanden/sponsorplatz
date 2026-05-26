package ch.sponsorplatz.benutzer;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * View-Mapping-Tests für AdminBenutzerView.
 */
class AdminBenutzerViewTest {

    @Test
    @DisplayName("VIEW-09: kein Profilbild -> profilbildUrl null, kein passwortHash")
    void ohneProfilbild() {
        AppUser u = new AppUser();
        u.setId(UUID.randomUUID());
        u.setEmail("a@example.ch");
        u.setAnzeigename("Anna");
        u.setPasswortHash("$2a$10$geheim");

        AdminBenutzerView view = AdminBenutzerView.von(u);

        assertThat(view.email()).isEqualTo("a@example.ch");
        assertThat(view.anzeigename()).isEqualTo("Anna");
        assertThat(view.profilbildUrl()).isNull();
        assertThat(view.toString()).doesNotContain("$2a$10$geheim");
    }

    @Test
    @DisplayName("VIEW-09b: mit Profilbild -> /medien/{id}")
    void mitProfilbild() {
        UUID bildId = UUID.randomUUID();
        AppUser u = new AppUser();
        u.setId(UUID.randomUUID());
        u.setEmail("b@example.ch");
        u.setAnzeigename("Ben");
        u.setProfilbildId(bildId);

        AdminBenutzerView view = AdminBenutzerView.von(u);

        assertThat(view.profilbildUrl()).isEqualTo("/medien/" + bildId);
    }

    // ── Auth-Quellen-Anzeige (UC-SSO-4) ────────────────────────────────────

    @Test
    @DisplayName("VIEW-09c: Form-Login-User → hatFormLogin=true, federierteProvider leer")
    void nurFormLogin() {
        AppUser u = new AppUser();
        u.setId(UUID.randomUUID());
        u.setEmail("form@example.ch");
        u.setPasswortHash("$2a$10$realbcrypthash");

        AdminBenutzerView view = AdminBenutzerView.von(u, List.of());

        assertThat(view.hatFormLogin()).isTrue();
        assertThat(view.federierteProvider()).isEmpty();
    }

    @Test
    @DisplayName("VIEW-09d: OIDC-Only-User (JIT) → hatFormLogin=false, federierteProvider=[GOOGLE]")
    void nurOidc() {
        AppUser u = new AppUser();
        u.setId(UUID.randomUUID());
        u.setEmail("oidc@css.ch");
        u.setPasswortHash(SponsorplatzOidcUserService.OIDC_ONLY_PASSWORT_MARKER);

        AdminBenutzerView view = AdminBenutzerView.von(u, List.of(IdentityProvider.GOOGLE));

        assertThat(view.hatFormLogin()).isFalse();
        assertThat(view.federierteProvider()).containsExactly(IdentityProvider.GOOGLE);
    }

    @Test
    @DisplayName("VIEW-09e: Hybrid-User (Email-Match-Verknüpfung) → hatFormLogin=true + Provider")
    void hybridFormLoginUndOidc() {
        AppUser u = new AppUser();
        u.setId(UUID.randomUUID());
        u.setEmail("hybrid@css.ch");
        u.setPasswortHash("$2a$10$echterhash");

        AdminBenutzerView view = AdminBenutzerView.von(u,
                List.of(IdentityProvider.GOOGLE, IdentityProvider.ENTRA_ID));

        assertThat(view.hatFormLogin()).isTrue();
        assertThat(view.federierteProvider())
                .containsExactly(IdentityProvider.GOOGLE, IdentityProvider.ENTRA_ID);
    }

    @Test
    @DisplayName("VIEW-09f: passwortHash=null → hatFormLogin=false (defensive)")
    void passwortHashNull() {
        AppUser u = new AppUser();
        u.setId(UUID.randomUUID());
        u.setEmail("nopw@example.ch");
        u.setPasswortHash(null);

        AdminBenutzerView view = AdminBenutzerView.von(u, List.of());

        assertThat(view.hatFormLogin()).isFalse();
    }

    @Test
    @DisplayName("VIEW-09g: alte von(user)-Signatur bleibt bestehen, leere Provider-Liste")
    void backwardCompatVon() {
        AppUser u = new AppUser();
        u.setId(UUID.randomUUID());
        u.setEmail("legacy@example.ch");
        u.setPasswortHash("$2a$10$x");

        AdminBenutzerView view = AdminBenutzerView.von(u);

        assertThat(view.federierteProvider()).isEmpty();
        assertThat(view.hatFormLogin()).isTrue();
    }
}
