package ch.sponsorplatz.benutzer;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * SSO-11/12: Repository-Vertrag für föderierte Identitäten.
 *
 * <p>Diese Tests laufen gegen H2 (PostgreSQL-Mode) wie der Rest der Repo-Tests
 * (siehe {@link AppUserRepositoryTest}) — fängt SQL-Constraints + Lookup-Pfad
 * end-to-end ab, was reine Mocks verschluckt hätten.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("dev")
class FederierteIdentitaetRepositoryTest {

    @Autowired
    private FederierteIdentitaetRepository repository;

    @Autowired
    private TestEntityManager em;

    @Test
    @DisplayName("SSO-11: findByProviderAndSubject — Treffer bei vorhandenem Subject, leer sonst")
    void findByProviderAndSubject() {
        AppUser user = persistUser("max@example.ch");
        persistIdentitaet(user, IdentityProvider.ENTRA_ID, "entra-subject-123");

        Optional<FederierteIdentitaet> treffer =
                repository.findByProviderAndSubject(IdentityProvider.ENTRA_ID, "entra-subject-123");
        Optional<FederierteIdentitaet> miss =
                repository.findByProviderAndSubject(IdentityProvider.ENTRA_ID, "unbekannt");

        assertThat(treffer).isPresent();
        assertThat(treffer.get().getUser().getEmail()).isEqualTo("max@example.ch");
        assertThat(miss).isEmpty();
    }

    @Test
    @DisplayName("SSO-12: UNIQUE (provider, subject) — Duplikat wirft ConstraintViolation")
    void uniqueProviderSubject() {
        AppUser user1 = persistUser("user1@example.ch");
        AppUser user2 = persistUser("user2@example.ch");
        persistIdentitaet(user1, IdentityProvider.ENTRA_ID, "shared-subject");

        FederierteIdentitaet duplikat = new FederierteIdentitaet();
        duplikat.setUser(user2);
        duplikat.setProvider(IdentityProvider.ENTRA_ID);
        duplikat.setSubject("shared-subject");

        // persistAndFlush wirft Hibernate's ConstraintViolationException —
        // Spring würde das im Service-Layer auf DataIntegrityViolation wrappen,
        // im DataJpaTest-Slice ist die Hibernate-Variante das, was wir sehen.
        assertThatThrownBy(() -> em.persistAndFlush(duplikat))
                .isInstanceOf(org.hibernate.exception.ConstraintViolationException.class);
    }

    private AppUser persistUser(String email) {
        AppUser u = new AppUser();
        u.setEmail(email);
        u.setAnzeigename(email);
        u.setPasswortHash("$2a$test");
        return em.persistAndFlush(u);
    }

    private void persistIdentitaet(AppUser user, IdentityProvider provider, String subject) {
        FederierteIdentitaet f = new FederierteIdentitaet();
        f.setUser(user);
        f.setProvider(provider);
        f.setSubject(subject);
        em.persistAndFlush(f);
    }
}
