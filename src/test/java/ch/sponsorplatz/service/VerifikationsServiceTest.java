package ch.sponsorplatz.service;

import ch.sponsorplatz.model.AppUser;
import ch.sponsorplatz.repository.AppUserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class VerifikationsServiceTest {

    private AppUserRepository repository;
    private MailService mailService;
    private VerifikationsService service;

    @BeforeEach
    void setUp() {
        repository = mock(AppUserRepository.class);
        mailService = mock(MailService.class);
        service = new VerifikationsService(repository, mailService, "http://localhost:8080");
    }

    /** EV-01: sendeVerifikationsMail setzt Token + Ablaufdatum auf User. */
    @Test
    void sendeVerifikationsMailSetztToken() {
        AppUser user = new AppUser();
        user.setEmail("test@example.com");
        user.setAnzeigename("Test");
        when(repository.save(any(AppUser.class))).thenAnswer(inv -> inv.getArgument(0));

        service.sendeVerifikationsMail(user);

        assertThat(user.getVerifikationsToken()).isNotNull();
        assertThat(user.getVerifikationsToken()).hasSize(64);
        assertThat(user.getTokenGueltigBis()).isAfter(Instant.now().plus(23, ChronoUnit.HOURS));
        verify(mailService).sendeHtml(eq("test@example.com"), any(String.class), any());
    }

    /** EV-02: verifiziere mit gültigem Token → emailVerifiziert = true, Token gelöscht. */
    @Test
    void verifiziereMitGueltigemToken() {
        AppUser user = new AppUser();
        user.setVerifikationsToken("gueltig-token");
        user.setTokenGueltigBis(Instant.now().plus(1, ChronoUnit.HOURS));
        user.setEmailVerifiziert(false);
        when(repository.findByVerifikationsToken("gueltig-token")).thenReturn(Optional.of(user));
        when(repository.save(any(AppUser.class))).thenAnswer(inv -> inv.getArgument(0));

        service.verifiziere("gueltig-token");

        assertThat(user.isEmailVerifiziert()).isTrue();
        assertThat(user.getVerifikationsToken()).isNull();
        assertThat(user.getTokenGueltigBis()).isNull();
    }

    /** EV-03: verifiziere mit abgelaufenem Token → IllegalStateException. */
    @Test
    void verifiziereAbgelaufenWirft() {
        AppUser user = new AppUser();
        user.setVerifikationsToken("abgelaufen");
        user.setTokenGueltigBis(Instant.now().minus(1, ChronoUnit.HOURS));
        when(repository.findByVerifikationsToken("abgelaufen")).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> service.verifiziere("abgelaufen"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("abgelaufen");
    }

    /** EV-04: verifiziere mit unbekanntem Token → IllegalArgumentException. */
    @Test
    void verifiziereUnbekanntWirft() {
        when(repository.findByVerifikationsToken("unbekannt")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.verifiziere("unbekannt"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("ungültig");
    }
}
