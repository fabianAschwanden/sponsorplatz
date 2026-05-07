package ch.sponsorplatz.startup;

import ch.sponsorplatz.model.AppUser;
import ch.sponsorplatz.model.PlatformRolle;
import ch.sponsorplatz.repository.AppUserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests für {@link DevSeedRunner} — verifiziert Property-Override und
 * Idempotenz (kein erneutes Anlegen wenn User existiert).
 *
 * Test-IDs: DEV-SEED-01..03 in {@code specs/TESTSTRATEGIE.md}.
 */
@ExtendWith(MockitoExtension.class)
class DevSeedRunnerTest {

    @Test
    @DisplayName("DEV-SEED-01: Property-Override für E-Mail + Passwort wird angewandt")
    void propertyOverrideAngewendet() {
        AppUserRepository repo = mock(AppUserRepository.class);
        PasswordEncoder encoder = mock(PasswordEncoder.class);
        when(repo.findByEmail("custom@dev.local")).thenReturn(Optional.empty());
        when(encoder.encode("mein-secret")).thenReturn("HASH");

        DevSeedRunner runner = new DevSeedRunner(repo, encoder, "custom@dev.local", "mein-secret");
        runner.run();

        ArgumentCaptor<AppUser> captor = ArgumentCaptor.forClass(AppUser.class);
        verify(repo).save(captor.capture());
        AppUser saved = captor.getValue();
        assertThat(saved.getEmail()).isEqualTo("custom@dev.local");
        assertThat(saved.getPasswortHash()).isEqualTo("HASH");
        assertThat(saved.getPlatformRolle()).isEqualTo(PlatformRolle.PLATFORM_ADMIN);
        assertThat(saved.isEmailVerifiziert()).isTrue();
    }

    @Test
    @DisplayName("DEV-SEED-02: Wenn User bereits existiert wird nichts angelegt")
    void idempotent() {
        AppUserRepository repo = mock(AppUserRepository.class);
        PasswordEncoder encoder = mock(PasswordEncoder.class);
        when(repo.findByEmail(any())).thenReturn(Optional.of(new AppUser()));

        DevSeedRunner runner = new DevSeedRunner(repo, encoder, "dev@sponsorplatz.ch", "dev");
        runner.run();

        verify(repo, never()).save(any());
    }

    @Test
    @DisplayName("DEV-SEED-03: Default-Passwort 'dev' wird verwendet, wenn keine Property")
    void defaultPasswort() {
        AppUserRepository repo = mock(AppUserRepository.class);
        PasswordEncoder encoder = mock(PasswordEncoder.class);
        when(repo.findByEmail("dev@sponsorplatz.ch")).thenReturn(Optional.empty());
        when(encoder.encode("dev")).thenReturn("HASH");

        DevSeedRunner runner = new DevSeedRunner(repo, encoder, "dev@sponsorplatz.ch", "dev");
        runner.run();

        verify(encoder).encode("dev");
    }
}
