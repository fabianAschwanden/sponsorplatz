package ch.sponsorplatz.service;

import ch.sponsorplatz.dto.AppUserFormDto;
import ch.sponsorplatz.model.AppUser;
import ch.sponsorplatz.repository.AppUserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AppUserServiceTest {

    private AppUserRepository repository;
    private VerifikationsService verifikationsService;
    private AppUserService service;

    @BeforeEach
    void setUp() {
        repository = mock(AppUserRepository.class);
        verifikationsService = mock(VerifikationsService.class);
        PasswordEncoder encoder = new BCryptPasswordEncoder();
        service = new AppUserService(repository, encoder, verifikationsService);
    }

    /** AU-03: registriere hasht Passwort via BCrypt (Klartext ≠ gespeicherter Hash). */
    @Test
    void registriereHashtPasswort() {
        when(repository.findByEmail("neu@example.com")).thenReturn(Optional.empty());
        when(repository.save(any(AppUser.class))).thenAnswer(inv -> inv.getArgument(0));

        AppUserFormDto dto = neuesDto("neu@example.com", "Neuer User", "klartext123");

        AppUser gespeichert = service.registriere(dto);

        assertThat(gespeichert.getPasswortHash()).isNotEqualTo("klartext123");
        assertThat(gespeichert.getPasswortHash()).startsWith("$2a$");
    }

    /** AU-04: registriere bei doppelter E-Mail → IllegalArgumentException. */
    @Test
    void registriereWirftBeiDoppelterEmail() {
        AppUser bestehend = new AppUser();
        bestehend.setEmail("gibt-es@example.com");
        when(repository.findByEmail("gibt-es@example.com")).thenReturn(Optional.of(bestehend));

        AppUserFormDto dto = neuesDto("gibt-es@example.com", "Egal", "passwort123");

        assertThatThrownBy(() -> service.registriere(dto))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("E-Mail");
    }

    /** AU-05: registriere mit leerem Anzeigename → IllegalArgumentException. */
    @Test
    void registriereWirftBeiLeeremAnzeigename() {
        AppUserFormDto dto = neuesDto("ok@example.com", "", "passwort123");

        assertThatThrownBy(() -> service.registriere(dto))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Anzeigename");
    }

    private AppUserFormDto neuesDto(String email, String anzeigename, String passwort) {
        AppUserFormDto dto = new AppUserFormDto();
        dto.setEmail(email);
        dto.setAnzeigename(anzeigename);
        dto.setPasswort(passwort);
        return dto;
    }
}

