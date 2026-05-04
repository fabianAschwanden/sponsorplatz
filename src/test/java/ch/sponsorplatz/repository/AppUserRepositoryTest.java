package ch.sponsorplatz.repository;

import ch.sponsorplatz.model.AppUser;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("dev")
class AppUserRepositoryTest {

    @Autowired
    private AppUserRepository repository;

    /** AU-02: Persistieren + laden; aktiv Default = true. */
    @Test
    void persistierenUndLaden() {
        AppUser user = neuerUser("test@example.com", "Max Muster", "$2a$10$hashhashhashhash");

        AppUser gespeichert = repository.saveAndFlush(user);

        assertThat(gespeichert.getId()).isNotNull();
        assertThat(gespeichert.isAktiv()).isTrue();
        assertThat(gespeichert.getRegistriertAm()).isNotNull();
        assertThat(gespeichert.getCreatedAt()).isNotNull();

        Optional<AppUser> gefunden = repository.findByEmail("test@example.com");
        assertThat(gefunden).isPresent();
        assertThat(gefunden.get().getAnzeigename()).isEqualTo("Max Muster");
    }

    /** AU-01: E-Mail UNIQUE — Duplikat wirft Exception. */
    @Test
    void emailIstEindeutig() {
        repository.saveAndFlush(neuerUser("duplikat@example.com", "Erster", "$2a$10$hash1"));

        AppUser zweiter = neuerUser("duplikat@example.com", "Zweiter", "$2a$10$hash2");

        assertThatThrownBy(() -> repository.saveAndFlush(zweiter))
                .isInstanceOf(Exception.class);
    }

    private AppUser neuerUser(String email, String anzeigename, String passwortHash) {
        AppUser user = new AppUser();
        user.setEmail(email);
        user.setAnzeigename(anzeigename);
        user.setPasswortHash(passwortHash);
        return user;
    }
}

