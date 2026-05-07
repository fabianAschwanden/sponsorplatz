package ch.sponsorplatz.benutzer;
import ch.sponsorplatz.organisation.Rolle;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Legt im prod-Profil beim ersten Start einen PLATFORM_ADMIN an,
 * sofern noch keiner existiert und die Umgebungsvariablen gesetzt sind.
 *
 * Konfiguration via ENV:
 *   SPONSORPLATZ_ADMIN_EMAIL    (Pflicht)
 *   SPONSORPLATZ_ADMIN_PASSWORD (Pflicht, min. 12 Zeichen empfohlen)
 *   SPONSORPLATZ_ADMIN_NAME     (Optional, Default: "Plattform-Admin")
 *
 * Idempotent: wird nur ausgeführt wenn kein User mit PLATFORM_ADMIN-Rolle existiert.
 */
@Component
@Profile("prod")
public class ProdAdminSeedRunner implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(ProdAdminSeedRunner.class);

    private final AppUserRepository userRepository;
    private final PasswordEncoder encoder;

    @Value("${SPONSORPLATZ_ADMIN_EMAIL:}")
    private String adminEmail;

    @Value("${SPONSORPLATZ_ADMIN_PASSWORD:}")
    private String adminPassword;

    @Value("${SPONSORPLATZ_ADMIN_NAME:Plattform-Admin}")
    private String adminName;

    public ProdAdminSeedRunner(AppUserRepository userRepository, PasswordEncoder encoder) {
        this.userRepository = userRepository;
        this.encoder = encoder;
    }

    @Override
    public void run(String... args) {
        // Nur anlegen wenn ENV gesetzt
        if (adminEmail == null || adminEmail.isBlank()) {
            log.info("SPONSORPLATZ_ADMIN_EMAIL nicht gesetzt — kein Admin-Seed.");
            return;
        }
        if (adminPassword == null || adminPassword.isBlank()) {
            log.warn("SPONSORPLATZ_ADMIN_PASSWORD nicht gesetzt — Admin-Seed übersprungen!");
            return;
        }

        // Idempotent: bereits ein Admin vorhanden?
        if (userRepository.findByEmail(adminEmail.toLowerCase().trim()).isPresent()) {
            log.info("Admin-Account {} existiert bereits — Seed übersprungen.", adminEmail);
            return;
        }

        AppUser admin = new AppUser();
        admin.setEmail(adminEmail.toLowerCase().trim());
        admin.setAnzeigename(adminName.trim());
        admin.setPasswortHash(encoder.encode(adminPassword));
        admin.setAktiv(true);
        admin.setEmailVerifiziert(true);
        admin.setPlatformRolle(PlatformRolle.PLATFORM_ADMIN);
        userRepository.save(admin);

        log.info("PROD-Seed: PLATFORM_ADMIN angelegt für E-Mail: {}", adminEmail);
    }
}

