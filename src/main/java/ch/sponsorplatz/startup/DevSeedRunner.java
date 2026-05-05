package ch.sponsorplatz.startup;

import ch.sponsorplatz.model.AppUser;
import ch.sponsorplatz.model.PlatformRolle;
import ch.sponsorplatz.repository.AppUserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Legt im dev-Profil einen sofort einloggbaren Test-User an.
 * Vermeidet die E-Mail-Verifikations-Schleife für lokale UI-Reviews.
 */
@Component
@Profile("dev")
public class DevSeedRunner implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DevSeedRunner.class);
    private static final String DEV_EMAIL = "dev@sponsorplatz.ch";
    private static final String DEV_PASSWORT = "dev";

    private final AppUserRepository userRepository;
    private final PasswordEncoder encoder;

    public DevSeedRunner(AppUserRepository userRepository, PasswordEncoder encoder) {
        this.userRepository = userRepository;
        this.encoder = encoder;
    }

    @Override
    public void run(String... args) {
        if (userRepository.findByEmail(DEV_EMAIL).isPresent()) {
            return;
        }
        AppUser user = new AppUser();
        user.setEmail(DEV_EMAIL);
        user.setAnzeigename("Dev User");
        user.setPasswortHash(encoder.encode(DEV_PASSWORT));
        user.setAktiv(true);
        user.setEmailVerifiziert(true);
        user.setPlatformRolle(PlatformRolle.PLATFORM_ADMIN);
        userRepository.save(user);

        log.warn("DEV-Seed: Test-User mit PLATFORM_ADMIN angelegt -> Login: {} / {}", DEV_EMAIL, DEV_PASSWORT);
    }
}
