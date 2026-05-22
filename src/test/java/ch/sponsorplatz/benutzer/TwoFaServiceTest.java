package ch.sponsorplatz.benutzer;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests für {@link TwoFaService} — Service-Schicht der 2-Faktor-
 * Authentifizierung. Test-IDs AUTH-2FA-S-01..06 in
 * {@code specs/AUTH_2FA_TOTP.md}.
 */
class TwoFaServiceTest {

    private static final String EMAIL = "user@sponsorplatz.ch";

    private AppUserRepository repository;
    private TotpService totpService;
    private BCryptPasswordEncoder encoder;
    private ApplicationEventPublisher events;
    private TwoFaService service;
    private AppUser user;

    @BeforeEach
    void setUp() {
        repository = mock(AppUserRepository.class);
        encoder = new BCryptPasswordEncoder();
        totpService = new TotpService(encoder);
        events = mock(ApplicationEventPublisher.class);
        service = new TwoFaService(repository, totpService, encoder, events);

        user = new AppUser();
        user.setId(UUID.randomUUID());
        user.setEmail(EMAIL);
        user.setPasswortHash(encoder.encode("dev"));
        when(repository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
        when(repository.save(any(AppUser.class))).thenAnswer(i -> i.getArgument(0));
    }

    @Test
    @DisplayName("AUTH-2FA-S-01: findStatus liefert aktiv=false ohne Secret")
    void findStatusOhneSecret() {
        TwoFaService.TwoFaStatus st = service.findStatus(EMAIL);
        assertThat(st.aktiv()).isFalse();
        assertThat(st.aktiviertAm()).isNull();
        assertThat(st.backupCodesVerbleibend()).isZero();
    }

    @Test
    @DisplayName("AUTH-2FA-S-02: bereiteSetupVor liefert frisches Secret + QR + Manuell-Code")
    void bereiteSetupVor() {
        TwoFaService.TwoFaSetupContext ctx = service.bereiteSetupVor(EMAIL, null);
        assertThat(ctx.pendingSecret()).isNotBlank();
        assertThat(ctx.qrDataUrl()).startsWith("data:image/png;base64,");
        assertThat(ctx.manuellerCode()).isEqualTo(ctx.pendingSecret());
    }

    @Test
    @DisplayName("AUTH-2FA-S-03: aktivieren mit korrektem Code setzt Secret + publiziert Event")
    void aktivierenErfolg() throws Exception {
        String secret = totpService.generateSecret();
        String code = totpService.generateCodeFor(secret, System.currentTimeMillis() / 1000);

        TwoFaService.TwoFaAktivierungsErgebnis res = service.aktivieren(EMAIL, secret, code);

        assertThat(res.erfolgreich()).isTrue();
        assertThat(res.neueBackupCodes()).hasSize(10);
        assertThat(user.hatTotpAktiv()).isTrue();
        assertThat(user.getTotpSecret()).isEqualTo(secret);

        ArgumentCaptor<Object> ev = ArgumentCaptor.forClass(Object.class);
        verify(events).publishEvent(ev.capture());
        assertThat(ev.getValue()).isInstanceOf(TwoFaEvents.TwoFaAktiviertEvent.class);
    }

    @Test
    @DisplayName("AUTH-2FA-S-04: aktivieren mit falschem Code → UNGUELTIG, kein Save, kein Event")
    void aktivierenFalscherCode() {
        String secret = totpService.generateSecret();

        TwoFaService.TwoFaAktivierungsErgebnis res = service.aktivieren(EMAIL, secret, "000000");

        assertThat(res.erfolgreich()).isFalse();
        assertThat(res.neueBackupCodes()).isEmpty();
        verify(repository, never()).save(any());
        verify(events, never()).publishEvent(any());
    }

    @Test
    @DisplayName("AUTH-2FA-S-05: deaktivieren verlangt korrektes Passwort UND TOTP")
    void deaktivieren() throws Exception {
        String secret = totpService.generateSecret();
        user.setTotpSecret(secret);
        user.setTotpAktiviertAm(Instant.now());
        user.setTotpBackupCodesHashed("[]");
        String code = totpService.generateCodeFor(secret, System.currentTimeMillis() / 1000);

        assertThat(service.deaktivieren(EMAIL, "falsch", code)).isFalse();
        assertThat(service.deaktivieren(EMAIL, "dev", "000000")).isFalse();
        assertThat(user.hatTotpAktiv()).isTrue();

        assertThat(service.deaktivieren(EMAIL, "dev", code)).isTrue();
        assertThat(user.hatTotpAktiv()).isFalse();
        assertThat(user.getTotpSecret()).isNull();

        // Genau ein Event publiziert (nur beim erfolgreichen Lauf)
        verify(events).publishEvent(any(TwoFaEvents.TwoFaDeaktiviertEvent.class));
    }

    @Test
    @DisplayName("AUTH-2FA-S-06: regeneriereBackupCodes — falscher Code = empty, korrekt = 10 frische Codes")
    void regeneriereBackupCodes() throws Exception {
        String secret = totpService.generateSecret();
        user.setTotpSecret(secret);
        user.setTotpAktiviertAm(Instant.now());
        user.setTotpBackupCodesHashed("[\"$2a$10$dummyhash\"]");
        String code = totpService.generateCodeFor(secret, System.currentTimeMillis() / 1000);

        assertThat(service.regeneriereBackupCodes(EMAIL, "000000")).isEmpty();
        Optional<List<String>> neue = service.regeneriereBackupCodes(EMAIL, code);
        assertThat(neue).isPresent();
        assertThat(neue.get()).hasSize(10);
        verify(events).publishEvent(any(TwoFaEvents.TwoFaBackupCodesNeuEvent.class));
    }
}
