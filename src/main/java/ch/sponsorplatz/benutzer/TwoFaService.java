package ch.sponsorplatz.benutzer;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Kapselt alle 2-Faktor-Operationen auf {@link AppUser}.
 *
 * <p>Controller ({@link TwoFaSetupController}) sieht ausschliesslich die
 * record-DTOs in dieser Klasse — keine JPA-Entity (ARCH-02), kein
 * Repository (ARCH-01).
 *
 * <p>Audit-Logging läuft über {@link TwoFaEvents}; ein Listener im
 * {@code audit/}-Paket reagiert darauf. Direkter Compile-Time-Import in
 * audit ist bewusst nicht erlaubt (ARCH-06 Cycle).
 */
@Service
@Transactional
public class TwoFaService {

    private final AppUserRepository repository;
    private final TotpService totpService;
    private final PasswordEncoder passwordEncoder;
    private final ApplicationEventPublisher events;

    public TwoFaService(AppUserRepository repository,
                        TotpService totpService,
                        PasswordEncoder passwordEncoder,
                        ApplicationEventPublisher events) {
        this.repository = repository;
        this.totpService = totpService;
        this.passwordEncoder = passwordEncoder;
        this.events = events;
    }

    /** Aktueller 2FA-Status des Users. Wirft, wenn User nicht existiert. */
    @Transactional(readOnly = true)
    public TwoFaStatus findStatus(String email) {
        AppUser user = ladeOderWirf(email);
        if (!user.hatTotpAktiv()) {
            return new TwoFaStatus(false, null, 0);
        }
        int verbleibend = TotpService.parseHashedCodes(user.getTotpBackupCodesHashed()).size();
        return new TwoFaStatus(true, user.getTotpAktiviertAm(), verbleibend);
    }

    /**
     * Liefert einen Setup-Kontext für den noch nicht bestätigten Setup-Flow.
     * Wird ein bestehendes {@code pendingSecret} übergeben (aus der HTTP-
     * Session), wird damit weitergearbeitet, sonst frisch generiert.
     */
    @Transactional(readOnly = true)
    public TwoFaSetupContext bereiteSetupVor(String email, String pendingSecretOrNull) {
        AppUser user = ladeOderWirf(email);
        if (user.hatTotpAktiv()) {
            throw new IllegalStateException("2FA bereits aktiv für " + email);
        }
        String secret = (pendingSecretOrNull != null && !pendingSecretOrNull.isBlank())
                ? pendingSecretOrNull
                : totpService.generateSecret();
        String qr = totpService.generateQrDataUrl(secret, user.getEmail());
        return new TwoFaSetupContext(secret, qr, secret);
    }

    /**
     * Aktiviert 2FA, wenn der vom User eingegebene Code zum Pending-Secret
     * passt. Erzeugt + speichert 10 Backup-Codes (BCrypt-gehasht). Klartext
     * der Codes kommt im Ergebnis ZURÜCK — Controller zeigt sie genau einmal.
     */
    public TwoFaAktivierungsErgebnis aktivieren(String email, String pendingSecret, String code) {
        AppUser user = ladeOderWirf(email);
        if (user.hatTotpAktiv()) {
            return TwoFaAktivierungsErgebnis.BEREITS_AKTIV;
        }
        if (pendingSecret == null || pendingSecret.isBlank()
                || !totpService.verifyCode(pendingSecret, code)) {
            return TwoFaAktivierungsErgebnis.UNGUELTIG;
        }
        TotpService.BackupCodeBatch batch = totpService.generateBackupCodes();
        user.setTotpSecret(pendingSecret);
        user.setTotpAktiviertAm(Instant.now());
        user.setTotpBackupCodesHashed(batch.hashedJson());
        repository.save(user);
        events.publishEvent(new TwoFaEvents.TwoFaAktiviertEvent(user.getId(), user.getEmail()));
        return new TwoFaAktivierungsErgebnis(true, false, batch.codes());
    }

    /**
     * Deaktiviert 2FA. Verlangt korrekte Re-Auth (Passwort + TOTP).
     * @return true bei Erfolg, false bei falschem Passwort oder Code
     */
    public boolean deaktivieren(String email, String aktuellesPasswort, String code) {
        AppUser user = ladeOderWirf(email);
        if (!user.hatTotpAktiv()) {
            return true; // idempotent: schon weg
        }
        if (!passwordEncoder.matches(aktuellesPasswort, user.getPasswortHash())) {
            return false;
        }
        if (!totpService.verifyCode(user.getTotpSecret(), code)) {
            return false;
        }
        user.setTotpSecret(null);
        user.setTotpAktiviertAm(null);
        user.setTotpBackupCodesHashed(null);
        repository.save(user);
        events.publishEvent(new TwoFaEvents.TwoFaDeaktiviertEvent(user.getId(), user.getEmail()));
        return true;
    }

    /**
     * Prüft beim Login-Flow den eingegebenen Code — zuerst als TOTP, dann
     * als Backup-Code. Bei Backup-Treffer wird der Code aus der Liste
     * entfernt (single-use). Bei Treffer wird ein {@link TwoFaEvents.TwoFaLoginOkEvent}
     * publiziert, bei Miss ein {@link TwoFaEvents.TwoFaLoginFailEvent} mit
     * der Versuch-Nummer.
     *
     * @param versuchNummer 1-basierter Zähler des aktuellen Versuchs (Caller hält Session-Counter)
     * @return {@link LoginVerifyResult} mit Treffer-Flag + Backup-Code-Flag (für UI/Audit)
     */
    public LoginVerifyResult verifyForLogin(String email, String code, int versuchNummer) {
        AppUser user = ladeOderWirf(email);
        if (!user.hatTotpAktiv()) {
            return LoginVerifyResult.NICHT_AKTIV;
        }
        if (totpService.verifyCode(user.getTotpSecret(), code)) {
            events.publishEvent(new TwoFaEvents.TwoFaLoginOkEvent(user.getId(), user.getEmail(), false));
            return new LoginVerifyResult(true, false);
        }
        TotpService.BackupCodeResult bcr = totpService.consumeBackupCode(user.getTotpBackupCodesHashed(), code);
        if (bcr.matched()) {
            user.setTotpBackupCodesHashed(bcr.neuesJson());
            repository.save(user);
            events.publishEvent(new TwoFaEvents.TwoFaLoginOkEvent(user.getId(), user.getEmail(), true));
            return new LoginVerifyResult(true, true);
        }
        events.publishEvent(new TwoFaEvents.TwoFaLoginFailEvent(user.getId(), user.getEmail(), versuchNummer));
        return LoginVerifyResult.MISS;
    }

    /**
     * Publiziert das {@link TwoFaEvents.TwoFaLockoutEvent} — vom Controller
     * aufgerufen, wenn der Session-Counter 5 erreicht und die Session
     * invalidiert wird.
     */
    public void protokolliereLockout(String email) {
        AppUser user = ladeOderWirf(email);
        events.publishEvent(new TwoFaEvents.TwoFaLockoutEvent(user.getId(), user.getEmail()));
    }

    /**
     * Recovery-Pfad: PLATFORM_ADMIN setzt 2FA eines anderen Users zurück
     * (z.B. wenn der User Authenticator + alle Backup-Codes verloren hat).
     * Löscht Secret + aktiviertAm + Backup-Codes; idempotent.
     *
     * <p>Das Audit-Log-Eintrag schreibt der Aufrufer (AdminBenutzerController
     * hat den Admin im SecurityContext), nicht dieser Service — die
     * Quell-Identität liegt nicht in benutzer/, sondern in admin/.
     *
     * @return die E-Mail des Zielnutzers (für die Flash-Meldung), oder
     *         {@link Optional#empty()} wenn der User nicht existiert.
     */
    public Optional<AdminResetErgebnis> adminResetFuerUser(java.util.UUID targetUserId) {
        return repository.findById(targetUserId).map(user -> {
            boolean warAktiv = user.hatTotpAktiv();
            user.setTotpSecret(null);
            user.setTotpAktiviertAm(null);
            user.setTotpBackupCodesHashed(null);
            repository.save(user);
            return new AdminResetErgebnis(user.getEmail(), warAktiv);
        });
    }

    /**
     * Erzeugt einen frischen Satz Backup-Codes. Verlangt einen gültigen
     * TOTP-Code. Alte Codes werden ersetzt (alle ungültig).
     */
    public Optional<List<String>> regeneriereBackupCodes(String email, String code) {
        AppUser user = ladeOderWirf(email);
        if (!user.hatTotpAktiv()) {
            return Optional.empty();
        }
        if (!totpService.verifyCode(user.getTotpSecret(), code)) {
            return Optional.empty();
        }
        TotpService.BackupCodeBatch batch = totpService.generateBackupCodes();
        user.setTotpBackupCodesHashed(batch.hashedJson());
        repository.save(user);
        events.publishEvent(new TwoFaEvents.TwoFaBackupCodesNeuEvent(user.getId(), user.getEmail()));
        return Optional.of(batch.codes());
    }

    private AppUser ladeOderWirf(String email) {
        if (email == null) {
            throw new IllegalArgumentException("E-Mail darf nicht null sein");
        }
        return repository.findByEmail(email)
                .orElseThrow(() -> new IllegalStateException("User nicht gefunden: " + email));
    }

    // ── DTOs ────────────────────────────────────────────────────────────

    public record TwoFaStatus(boolean aktiv, Instant aktiviertAm, int backupCodesVerbleibend) {}

    public record TwoFaSetupContext(String pendingSecret, String qrDataUrl, String manuellerCode) {}

    public record TwoFaAktivierungsErgebnis(boolean erfolgreich, boolean bereitsAktiv, List<String> neueBackupCodes) {
        public static final TwoFaAktivierungsErgebnis UNGUELTIG =
                new TwoFaAktivierungsErgebnis(false, false, List.of());
        public static final TwoFaAktivierungsErgebnis BEREITS_AKTIV =
                new TwoFaAktivierungsErgebnis(false, true, List.of());
    }

    /**
     * Ergebnis von {@link #verifyForLogin}: matched=true wenn Code gültig war,
     * backupCodeGenutzt=true wenn der Treffer über die Backup-Code-Liste lief
     * (für UI-Hinweis "noch N Codes übrig" + Audit-Differenzierung).
     */
    public record LoginVerifyResult(boolean matched, boolean backupCodeGenutzt) {
        public static final LoginVerifyResult MISS = new LoginVerifyResult(false, false);
        public static final LoginVerifyResult NICHT_AKTIV = new LoginVerifyResult(false, false);
    }

    /**
     * Ergebnis von {@link #adminResetFuerUser}: E-Mail des Zielnutzers (für
     * UI-Meldung) + Flag ob der User vorher überhaupt 2FA aktiv hatte
     * (für Audit-Detail "war bereits ohne 2FA").
     */
    public record AdminResetErgebnis(String email, boolean warVorhAktiv) {}
}
