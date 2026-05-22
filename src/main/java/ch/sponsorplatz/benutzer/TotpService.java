package ch.sponsorplatz.benutzer;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.samstevens.totp.code.CodeGenerator;
import dev.samstevens.totp.code.DefaultCodeGenerator;
import dev.samstevens.totp.code.DefaultCodeVerifier;
import dev.samstevens.totp.code.HashingAlgorithm;
import dev.samstevens.totp.exceptions.CodeGenerationException;
import dev.samstevens.totp.exceptions.QrGenerationException;
import dev.samstevens.totp.qr.QrData;
import dev.samstevens.totp.qr.QrGenerator;
import dev.samstevens.totp.qr.ZxingPngQrGenerator;
import dev.samstevens.totp.secret.DefaultSecretGenerator;
import dev.samstevens.totp.secret.SecretGenerator;
import dev.samstevens.totp.time.SystemTimeProvider;
import dev.samstevens.totp.time.TimeProvider;
import dev.samstevens.totp.util.Utils;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * RFC 6238 TOTP-Service für 2-Faktor-Authentifizierung (Phase 13.2).
 *
 * <p>Operationen:
 * <ul>
 *   <li>{@link #generateSecret()} — Base32-Secret für Authenticator-Setup</li>
 *   <li>{@link #buildOtpAuthUri(String, String)} — OTPAuth-URI für QR-Code</li>
 *   <li>{@link #generateQrDataUrl(String, String)} — PNG-QR als data-URL für inline HTML</li>
 *   <li>{@link #verifyCode(String, String)} — 6-stelligen TOTP-Code prüfen, ±1 Step (30s)</li>
 *   <li>{@link #generateBackupCodes()} — 10 Backup-Codes + BCrypt-Hashes (JSON)</li>
 *   <li>{@link #consumeBackupCode(String, String)} — einmaliger Code-Verbrauch (hard delete)</li>
 * </ul>
 *
 * <p>Spec: {@code specs/AUTH_2FA_TOTP.md}, Tests AUTH-2FA-01..05.
 */
@Service
public class TotpService {

    private static final String ISSUER = "Sponsorplatz";
    private static final int BACKUP_CODE_COUNT = 10;
    private static final int BACKUP_CODE_LENGTH = 8;
    /** Alphanumerisch ohne 0/O, 1/l/I — Tipp-Fehler-resistent. */
    private static final char[] BACKUP_CODE_ALPHABET =
            "ABCDEFGHJKLMNPQRSTUVWXYZ23456789".toCharArray();
    private static final int ALLOWED_TIME_DISCREPANCY_STEPS = 1;
    private static final TypeReference<List<String>> LIST_OF_STRING = new TypeReference<>() {};

    private final SecretGenerator secretGenerator = new DefaultSecretGenerator();
    private final CodeGenerator codeGenerator = new DefaultCodeGenerator(HashingAlgorithm.SHA1);
    private final TimeProvider timeProvider = new SystemTimeProvider();
    private final DefaultCodeVerifier codeVerifier;
    private final QrGenerator qrGenerator = new ZxingPngQrGenerator();
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final SecureRandom secureRandom = new SecureRandom();
    private final BCryptPasswordEncoder encoder;

    public TotpService(BCryptPasswordEncoder encoder) {
        this.encoder = encoder;
        this.codeVerifier = new DefaultCodeVerifier(codeGenerator, timeProvider);
        this.codeVerifier.setAllowedTimePeriodDiscrepancy(ALLOWED_TIME_DISCREPANCY_STEPS);
    }

    public String generateSecret() {
        return secretGenerator.generate();
    }

    /**
     * Baut den OTPAuth-URI ({@code otpauth://totp/...}) für Authenticator-Apps.
     * {@code label} ist üblicherweise die User-E-Mail.
     */
    public String buildOtpAuthUri(String secret, String label) {
        return new QrData.Builder()
                .label(label)
                .secret(secret)
                .issuer(ISSUER)
                .algorithm(HashingAlgorithm.SHA1)
                .digits(6)
                .period(30)
                .build()
                .getUri();
    }

    /**
     * Liefert den QR-Code als {@code data:image/png;base64,...}-URL — direkt
     * in ein {@code <img src="">} setzbar, kein extra HTTP-Roundtrip.
     */
    public String generateQrDataUrl(String secret, String label) {
        QrData data = new QrData.Builder()
                .label(label)
                .secret(secret)
                .issuer(ISSUER)
                .algorithm(HashingAlgorithm.SHA1)
                .digits(6)
                .period(30)
                .build();
        try {
            return Utils.getDataUriForImage(
                    qrGenerator.generate(data),
                    qrGenerator.getImageMimeType());
        } catch (QrGenerationException e) {
            throw new IllegalStateException("QR-Code-Generierung fehlgeschlagen", e);
        }
    }

    /**
     * Prüft den vom User eingegebenen 6-stelligen Code gegen das Secret.
     * Window ±1 Step (30s zurück/vor) für Clock-Drift-Toleranz.
     */
    public boolean verifyCode(String secret, String code) {
        if (secret == null || code == null || code.isBlank()) {
            return false;
        }
        return codeVerifier.isValidCode(secret, code.trim());
    }

    /**
     * Erzeugt einen TOTP-Code für einen bestimmten Unix-Timestamp (Sekunden).
     * Nur für Tests + interne Helfer — Produktion nutzt {@link #verifyCode}.
     */
    String generateCodeFor(String secret, long unixSeconds) throws CodeGenerationException {
        long bucket = Math.floorDiv(unixSeconds, 30L);
        return codeGenerator.generate(secret, bucket);
    }

    /**
     * Erzeugt einen frischen Batch von Backup-Codes (Klartext + BCrypt-Hashes).
     * Klartext wird **nur einmal** dem User angezeigt (Setup-/Re-Generate-Flow).
     */
    public BackupCodeBatch generateBackupCodes() {
        List<String> codes = new ArrayList<>(BACKUP_CODE_COUNT);
        List<String> hashes = new ArrayList<>(BACKUP_CODE_COUNT);
        for (int i = 0; i < BACKUP_CODE_COUNT; i++) {
            String code = randomCode();
            codes.add(code);
            hashes.add(encoder.encode(code));
        }
        return new BackupCodeBatch(Collections.unmodifiableList(codes), serializeHashes(hashes));
    }

    /**
     * Versucht den eingegebenen Code gegen die Hash-Liste zu matchen. Bei
     * Treffer wird der Hash entfernt (single-use) und das aktualisierte JSON
     * zurückgegeben. Bei Miss bleibt das JSON unverändert.
     */
    public BackupCodeResult consumeBackupCode(String hashedJson, String eingegeben) {
        if (hashedJson == null || eingegeben == null || eingegeben.isBlank()) {
            return new BackupCodeResult(false, hashedJson);
        }
        List<String> hashes = parseHashedCodes(hashedJson);
        String normalisiert = eingegeben.trim().toUpperCase();
        for (int i = 0; i < hashes.size(); i++) {
            if (encoder.matches(normalisiert, hashes.get(i))) {
                List<String> verbleibend = new ArrayList<>(hashes);
                verbleibend.remove(i);
                return new BackupCodeResult(true, serializeHashes(verbleibend));
            }
        }
        return new BackupCodeResult(false, hashedJson);
    }

    private String randomCode() {
        StringBuilder sb = new StringBuilder(BACKUP_CODE_LENGTH);
        for (int i = 0; i < BACKUP_CODE_LENGTH; i++) {
            sb.append(BACKUP_CODE_ALPHABET[secureRandom.nextInt(BACKUP_CODE_ALPHABET.length)]);
        }
        return sb.toString();
    }

    private String serializeHashes(List<String> hashes) {
        try {
            return objectMapper.writeValueAsString(hashes);
        } catch (IOException e) {
            throw new IllegalStateException("Backup-Codes-JSON-Serialisierung fehlgeschlagen", e);
        }
    }

    /** Deserialisiert das in {@code app_user.totp_backup_codes_hashed} gespeicherte JSON-Array. */
    public static List<String> parseHashedCodes(String hashedJson) {
        if (hashedJson == null || hashedJson.isBlank()) {
            return List.of();
        }
        try {
            return new ObjectMapper().readValue(hashedJson, LIST_OF_STRING);
        } catch (IOException e) {
            throw new IllegalStateException("Backup-Codes-JSON konnte nicht gelesen werden", e);
        }
    }

    /** Ergebnis von {@link #generateBackupCodes()}: Klartext (einmalige Anzeige) + Hash-JSON (DB). */
    public record BackupCodeBatch(List<String> codes, String hashedJson) {}

    /** Ergebnis von {@link #consumeBackupCode}: Treffer-Flag + (eventuell) aktualisiertes JSON. */
    public record BackupCodeResult(boolean matched, String neuesJson) {}
}
