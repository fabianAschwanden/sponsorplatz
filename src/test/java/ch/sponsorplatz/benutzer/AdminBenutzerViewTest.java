package ch.sponsorplatz.benutzer;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

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
}
