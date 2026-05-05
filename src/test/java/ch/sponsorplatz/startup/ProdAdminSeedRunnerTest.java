package ch.sponsorplatz.startup;

import ch.sponsorplatz.model.AppUser;
import ch.sponsorplatz.model.PlatformRolle;
import ch.sponsorplatz.repository.AppUserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProdAdminSeedRunnerTest {

    @Mock private AppUserRepository userRepository;
    @Mock private PasswordEncoder encoder;

    private ProdAdminSeedRunner runner;

    @BeforeEach
    void setUp() {
        runner = new ProdAdminSeedRunner(userRepository, encoder);
    }

    @Test
    @DisplayName("Seed erstellt Admin wenn ENV gesetzt und kein Admin existiert")
    void erstelltAdmin() throws Exception {
        ReflectionTestUtils.setField(runner, "adminEmail", "admin@sponsorplatz.ch");
        ReflectionTestUtils.setField(runner, "adminPassword", "super-sicheres-pw");
        ReflectionTestUtils.setField(runner, "adminName", "Chef");

        when(userRepository.findByEmail("admin@sponsorplatz.ch")).thenReturn(Optional.empty());
        when(encoder.encode("super-sicheres-pw")).thenReturn("$2a$10$hash");
        when(userRepository.save(any(AppUser.class))).thenAnswer(i -> i.getArgument(0));

        runner.run();

        ArgumentCaptor<AppUser> captor = ArgumentCaptor.forClass(AppUser.class);
        verify(userRepository).save(captor.capture());
        AppUser admin = captor.getValue();
        assertThat(admin.getEmail()).isEqualTo("admin@sponsorplatz.ch");
        assertThat(admin.getAnzeigename()).isEqualTo("Chef");
        assertThat(admin.getPlatformRolle()).isEqualTo(PlatformRolle.PLATFORM_ADMIN);
        assertThat(admin.isEmailVerifiziert()).isTrue();
        assertThat(admin.isAktiv()).isTrue();
    }

    @Test
    @DisplayName("Seed überspringt wenn Email leer")
    void ueberspringtOhneEmail() throws Exception {
        ReflectionTestUtils.setField(runner, "adminEmail", "");
        ReflectionTestUtils.setField(runner, "adminPassword", "pw");
        ReflectionTestUtils.setField(runner, "adminName", "X");

        runner.run();

        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Seed überspringt wenn Admin bereits existiert (idempotent)")
    void idempotent() throws Exception {
        ReflectionTestUtils.setField(runner, "adminEmail", "admin@sponsorplatz.ch");
        ReflectionTestUtils.setField(runner, "adminPassword", "pw");
        ReflectionTestUtils.setField(runner, "adminName", "X");

        AppUser existing = new AppUser();
        existing.setEmail("admin@sponsorplatz.ch");
        when(userRepository.findByEmail("admin@sponsorplatz.ch")).thenReturn(Optional.of(existing));

        runner.run();

        verify(userRepository, never()).save(any());
    }
}

