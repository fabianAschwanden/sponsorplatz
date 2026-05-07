package ch.sponsorplatz.service;

import ch.sponsorplatz.dto.AppUserFormDto;
import ch.sponsorplatz.dto.ProfilFormDto;
import ch.sponsorplatz.model.AppUser;
import ch.sponsorplatz.repository.AppUserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
