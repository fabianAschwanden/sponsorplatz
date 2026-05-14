package ch.sponsorplatz.benutzer;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Service für Benutzer-Verwaltung.
 */
@Service
@Transactional
public class AppUserService {

    private final AppUserRepository repository;
    private final PasswordEncoder passwordEncoder;
    private final VerifikationsService verifikationsService;

    public AppUserService(AppUserRepository repository, PasswordEncoder passwordEncoder,
                          VerifikationsService verifikationsService) {
        this.repository = repository;
        this.passwordEncoder = passwordEncoder;
        this.verifikationsService = verifikationsService;
    }

    /**
     * Registriert einen neuen Benutzer. Hasht das Passwort via BCrypt.
     *
     * @throws IllegalArgumentException bei doppelter E-Mail oder leerem Anzeigename
     */
    public AppUser registriere(AppUserFormDto dto) {
        if (dto.getAnzeigename() == null || dto.getAnzeigename().isBlank()) {
            throw new IllegalArgumentException("Anzeigename darf nicht leer sein");
        }
        if (dto.getEmail() == null || dto.getEmail().isBlank()) {
            throw new IllegalArgumentException("E-Mail darf nicht leer sein");
        }
        if (repository.findByEmail(dto.getEmail()).isPresent()) {
            throw new IllegalArgumentException("E-Mail ist bereits vergeben");
        }

        AppUser user = new AppUser();
        user.setEmail(dto.getEmail().trim().toLowerCase());
        user.setAnzeigename(dto.getAnzeigename().trim());
        user.setPasswortHash(passwordEncoder.encode(dto.getPasswort()));

        AppUser gespeichert = repository.save(user);
        verifikationsService.sendeVerifikationsMail(gespeichert);
        return gespeichert;
    }

    @Transactional(readOnly = true)
    public Optional<AppUser> findeNachEmail(String email) {
        return repository.findByEmail(email);
    }

    /**
     * Liefert die User-UUID anhand der Email — Komfort-Methode für Controller,
     * damit sie {@link AppUserRepository} nicht direkt injizieren müssen
     * (ARCH-01: Controller → Service, nicht → Repository).
     *
     * @throws ch.sponsorplatz.shared.exception.NotFoundException wenn die Email
     *         keinem User zugeordnet ist
     */
    /**
     * Alle User für das Admin-UI, neueste Registrierungen zuerst — Komfort-
     * Methode für Controller (ARCH-01).
     */
    @Transactional(readOnly = true)
    public List<AppUser> findeAlleNeuesteZuerst() {
        return repository.findAllByOrderByRegistriertAmDesc();
    }

    /**
     * Setzt das {@code aktiv}-Flag eines Users und speichert. Wird vom
     * Admin-UI ({@code /admin/benutzer/{id}/{sperren,entsperren}}) genutzt.
     *
     * @return die zugehörige Email-Adresse (für Audit-Log + Flash-Message)
     */
    public AppUser setzeAktiv(UUID userId, boolean aktiv) {
        AppUser user = repository.findById(userId)
                .orElseThrow(() -> new ch.sponsorplatz.shared.exception.NotFoundException(
                        "Benutzer nicht gefunden: " + userId));
        user.setAktiv(aktiv);
        return repository.save(user);
    }

    /**
     * Setzt oder entfernt (null) die Plattform-Rolle eines Users — Admin-Aktion.
     *
     * @return den aktualisierten User (für Audit-Log + Flash-Message)
     */
    public AppUser setzePlatformRolle(UUID userId, PlatformRolle rolle) {
        AppUser user = repository.findById(userId)
                .orElseThrow(() -> new ch.sponsorplatz.shared.exception.NotFoundException(
                        "Benutzer nicht gefunden: " + userId));
        user.setPlatformRolle(rolle);
        return repository.save(user);
    }

    /**
     * Markiert das Onboarding für einen User als gesehen — idempotent.
     * Wird vom Onboarding-Controller einmal beim ersten Anzeigen aufgerufen,
     * damit Folge-Logins nicht erneut auf das Wizard umleiten.
     */
    public void markiereOnboardingGesehen(UUID userId) {
        repository.findById(userId).ifPresent(user -> {
            if (!user.isOnboardingGesehen()) {
                user.setOnboardingGesehen(true);
                repository.save(user);
            }
        });
    }

    @Transactional(readOnly = true)
    public UUID findeIdNachEmail(String email) {
        return repository.findByEmail(email)
                .map(AppUser::getId)
                .orElseThrow(() -> new ch.sponsorplatz.shared.exception.NotFoundException(
                        "User nicht gefunden: " + email));
    }

    @Transactional(readOnly = true)
    public Optional<AppUser> findeNachId(UUID id) {
        return repository.findById(id);
    }

    /**
     * Ändert das Passwort eines Benutzers.
     *
     * @throws IllegalArgumentException wenn altes Passwort falsch oder neues leer
     */
    public void aenderePasswort(UUID userId, String altesPasswort, String neuesPasswort) {
        if (neuesPasswort == null || neuesPasswort.isBlank()) {
            throw new IllegalArgumentException("Neues Passwort darf nicht leer sein");
        }
        if (neuesPasswort.length() < 8) {
            throw new IllegalArgumentException("Neues Passwort muss mindestens 8 Zeichen haben");
        }

        AppUser user = repository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Benutzer nicht gefunden"));

        if (!passwordEncoder.matches(altesPasswort, user.getPasswortHash())) {
            throw new IllegalArgumentException("Aktuelles Passwort ist falsch");
        }

        user.setPasswortHash(passwordEncoder.encode(neuesPasswort));
        repository.save(user);
    }

    /**
     * Aktualisiert die Profil-Daten eines Benutzers.
     *
     * @throws IllegalArgumentException wenn User nicht gefunden oder Anzeigename leer
     */
    public AppUser aktualisiereProfil(UUID userId, ProfilFormDto dto) {
        AppUser user = repository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Benutzer nicht gefunden"));

        if (dto.getAnzeigename() == null || dto.getAnzeigename().isBlank()) {
            throw new IllegalArgumentException("Anzeigename darf nicht leer sein");
        }

        user.setAnzeigename(dto.getAnzeigename().trim());
        user.setSprache(dto.getSprache() != null ? dto.getSprache().trim() : "de_CH");
        user.setTelefon(leereAlsNull(dto.getTelefon()));
        user.setBio(leereAlsNull(dto.getBio()));
        user.setOrt(leereAlsNull(dto.getOrt()));
        user.setWebsiteUrl(leereAlsNull(dto.getWebsiteUrl()));
        user.setPositionTitel(leereAlsNull(dto.getPositionTitel()));

        return repository.save(user);
    }

    /**
     * Setzt das Profilbild eines Benutzers.
     */
    public void setzeProfilbild(UUID userId, UUID medienAssetId) {
        AppUser user = repository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Benutzer nicht gefunden"));
        user.setProfilbildId(medienAssetId);
        repository.save(user);
    }

    private String leereAlsNull(String s) {
        return (s == null || s.isBlank()) ? null : s.trim();
    }
}
