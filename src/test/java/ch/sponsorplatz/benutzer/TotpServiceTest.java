package ch.sponsorplatz.benutzer;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import dev.samstevens.totp.exceptions.CodeGenerationException;

/**
 * Tests für {@link TotpService} — RFC 6238 TOTP + Backup-Codes.
 *
 * Test-IDs: AUTH-2FA-01..05 in {@code specs/AUTH_2FA_TOTP.md}.
 */
class TotpServiceTest {

    private TotpService service;
    private BCryptPasswordEncoder encoder;

    @BeforeEach
    void setUp() {
        encoder = new BCryptPasswordEncoder();
        service = new TotpService(encoder);
    }

    @Test
    @DisplayName("AUTH-2FA-01: generateSecret liefert Base32-konformes ≥ 32-Zeichen-Secret")
    void generateSecretLiefertBase32() {
        String secret = service.generateSecret();
        assertThat(secret).isNotBlank();
        assertThat(secret.length()).isGreaterThanOrEqualTo(32);
        // Base32: A-Z + 2-7
        assertThat(secret).matches("[A-Z2-7]+");
    }

    @Test
    @DisplayName("AUTH-2FA-02: verifyCode akzeptiert aktuellen Code, lehnt falschen ab")
    void verifyCodeAkzeptiertKorrekt() throws CodeGenerationException {
        String secret = service.generateSecret();
        String aktuellerCode = generateAktuellenCode(secret);

        assertThat(service.verifyCode(secret, aktuellerCode)).isTrue();
        assertThat(service.verifyCode(secret, "000000")).isFalse();
        assertThat(service.verifyCode(secret, "")).isFalse();
        assertThat(service.verifyCode(secret, null)).isFalse();
    }

    @Test
    @DisplayName("AUTH-2FA-03: Replay-Window akzeptiert ±1 Step (30s), lehnt 2 Steps ab")
    void verifyCodeReplayWindow() throws CodeGenerationException {
        // 'samstevens.totp' DefaultCodeVerifier hat 'allowedTimePeriodDiscrepancy'
        // (Steps), Default 0. TotpService setzt es auf 1 → ±30s.
        // Diesen Test stützen wir auf das Service-Verhalten, nicht auf interne API.
        String secret = service.generateSecret();
        long jetzt = System.currentTimeMillis() / 1000;

        // -1 Step (30s in der Vergangenheit): muss akzeptiert werden
        String codeVor30s = service.generateCodeFor(secret, jetzt - 30);
        assertThat(service.verifyCode(secret, codeVor30s)).isTrue();

        // -2 Steps (60s in der Vergangenheit): muss abgelehnt werden
        String codeVor60s = service.generateCodeFor(secret, jetzt - 60);
        assertThat(service.verifyCode(secret, codeVor60s)).isFalse();
    }

    @Test
    @DisplayName("AUTH-2FA-04: generateBackupCodes liefert 10 unique 8-stellige Codes, BCrypt-Hashes serialisiert")
    void generateBackupCodes() {
        TotpService.BackupCodeBatch batch = service.generateBackupCodes();

        assertThat(batch.codes()).hasSize(10);
        // Eindeutig
        assertThat(batch.codes().stream().distinct().count()).isEqualTo(10);
        // 8-stellig, alphanumerisch, ohne mehrdeutige 0/O/1/l/I
        assertThat(batch.codes()).allMatch(c -> c.length() == 8 && c.matches("[A-HJ-NP-Z2-9]+"));

        // BCrypt-Hash-JSON enthält 10 Einträge
        List<String> hashes = TotpService.parseHashedCodes(batch.hashedJson());
        assertThat(hashes).hasSize(10);
        // Jeder Klartext-Code matcht einen seiner Hashes (Reihenfolge irrelevant)
        assertThat(batch.codes())
                .allSatisfy(code -> assertThat(hashes.stream().anyMatch(h -> encoder.matches(code, h))).isTrue());
    }

    @Test
    @DisplayName("AUTH-2FA-05: consumeBackupCode entfernt verwendeten Code aus dem Hash-Array")
    void consumeBackupCode() {
        TotpService.BackupCodeBatch batch = service.generateBackupCodes();
        String code0 = batch.codes().get(0);

        TotpService.BackupCodeResult result = service.consumeBackupCode(batch.hashedJson(), code0);

        assertThat(result.matched()).isTrue();
        List<String> verbleibend = TotpService.parseHashedCodes(result.neuesJson());
        assertThat(verbleibend).hasSize(9);
        // Code0 lässt sich nicht erneut einlösen
        TotpService.BackupCodeResult result2 = service.consumeBackupCode(result.neuesJson(), code0);
        assertThat(result2.matched()).isFalse();
        assertThat(result2.neuesJson()).isEqualTo(result.neuesJson()); // unverändert

        // Falscher Code: keine Änderung
        TotpService.BackupCodeResult falsch = service.consumeBackupCode(batch.hashedJson(), "AAAAAAAA");
        assertThat(falsch.matched()).isFalse();
    }

    // --- Helfer ----------------------------------------------------------

    /** TOTP-Code für den aktuellen Zeit-Step. */
    private String generateAktuellenCode(String secret) throws CodeGenerationException {
        return service.generateCodeFor(secret, System.currentTimeMillis() / 1000);
    }
}
