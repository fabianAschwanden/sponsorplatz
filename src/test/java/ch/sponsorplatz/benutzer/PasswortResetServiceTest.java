package ch.sponsorplatz.benutzer;
import ch.sponsorplatz.shared.mail.MailService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;


@ExtendWith(MockitoExtension.class)
class PasswortResetServiceTest {

    @Mock
    private AppUserRepository repository;
    @Mock
    private MailService mailService;
    @Mock
    private PasswordEncoder passwordEncoder;

    private PasswortResetService service;

    @BeforeEach
    void setUp() {
        service = new PasswortResetService(repository, mailService, passwordEncoder, "http://localhost:8080");
    }

    @Test
    @DisplayName("PWRESET-01: sendeResetMail generiert Token und sendet Mail")
    void sendeResetMailErfolgreich() {
        AppUser user = new AppUser();
        user.setId(UUID.randomUUID());
        user.setEmail("test@sp.ch");
        user.setAnzeigename("Test");
        when(repository.findByEmail("test@sp.ch")).thenReturn(Optional.of(user));
        when(repository.save(any(AppUser.class))).thenAnswer(i -> i.getArgument(0));

        service.sendeResetMail("test@sp.ch");

        verify(repository).save(user);
        assertThat(user.getResetToken()).isNotNull();
        assertThat(user.getResetTokenGueltigBis()).isAfter(Instant.now());
        verify(mailService).sendeHtml(eq("test@sp.ch"), eq("Sponsorplatz — Passwort zurücksetzen"), any());
    }

    @Test
    @DisplayName("PWRESET-02: sendeResetMail bei unbekannter Mail → still, kein Leak")
    void sendeResetMailUnbekannt() {
        when(repository.findByEmail("nope@sp.ch")).thenReturn(Optional.empty());

        service.sendeResetMail("nope@sp.ch");

        verify(repository, never()).save(any());
        verify(mailService, never()).sendeHtml(any(), any(), any());
    }

    @Test
    @DisplayName("PWRESET-03: validiereToken mit gültigem Token → gibt User zurück")
    void validiereTokenGueltig() {
        AppUser user = new AppUser();
        user.setResetToken("abc123");
        user.setResetTokenGueltigBis(Instant.now().plus(1, ChronoUnit.HOURS));
        when(repository.findByResetToken("abc123")).thenReturn(Optional.of(user));

        AppUser result = service.validiereToken("abc123");
        assertThat(result).isEqualTo(user);
    }

    @Test
    @DisplayName("PWRESET-04: validiereToken mit unbekanntem Token → wirft")
    void validiereTokenUnbekannt() {
        when(repository.findByResetToken("xxx")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.validiereToken("xxx"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("ungültig");
    }

    @Test
    @DisplayName("PWRESET-05: validiereToken mit abgelaufenem Token → wirft")
    void validiereTokenAbgelaufen() {
        AppUser user = new AppUser();
        user.setResetToken("old");
        user.setResetTokenGueltigBis(Instant.now().minus(1, ChronoUnit.HOURS));
        when(repository.findByResetToken("old")).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> service.validiereToken("old"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("abgelaufen");
    }

    @Test
    @DisplayName("PWRESET-06: setzeNeuesPasswort setzt Hash und löscht Token")
    void setzePasswortErfolgreich() {
        AppUser user = new AppUser();
        user.setResetToken("valid");
        user.setResetTokenGueltigBis(Instant.now().plus(1, ChronoUnit.HOURS));
        when(repository.findByResetToken("valid")).thenReturn(Optional.of(user));
        when(passwordEncoder.encode("neues-pw-123")).thenReturn("$2a$hashed");
        when(repository.save(any())).thenAnswer(i -> i.getArgument(0));

        service.setzeNeuesPasswort("valid", "neues-pw-123");

        assertThat(user.getPasswortHash()).isEqualTo("$2a$hashed");
        assertThat(user.getResetToken()).isNull();
        assertThat(user.getResetTokenGueltigBis()).isNull();
    }

    @Test
    @DisplayName("PWRESET-07: setzeNeuesPasswort mit kurzem PW → wirft")
    void setzePasswortZuKurz() {
        assertThatThrownBy(() -> service.setzeNeuesPasswort("token", "kurz"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("8 Zeichen");
    }
}
