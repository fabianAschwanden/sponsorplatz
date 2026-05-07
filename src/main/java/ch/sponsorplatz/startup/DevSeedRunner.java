package ch.sponsorplatz.startup;

import ch.sponsorplatz.model.AppUser;
import ch.sponsorplatz.model.PlatformRolle;
import ch.sponsorplatz.repository.AppUserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Legt im dev-Profil einen sofort einloggbaren Test-User an.
 * Vermeidet die E-Mail-Verifikations-Schleife für lokale UI-Reviews.
 *
 * <p>E-Mail und Passwort sind über Properties überschreibbar:
 * <pre>
 * sponsorplatz.dev.email=dev@sponsorplatz.ch
 * sponsorplatz.dev.passwort=dev
 * </pre>
 * Der Default {@code dev} ist absichtlich kurz für lokale UI-Reviews,
 * lebt aber nur im {@code dev}-Profile (siehe {@link Profile}).
 */
@Component
@Profile("dev")
public class DevSeedRunner implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DevSeedRunner.class);

    private final AppUserRepository userRepository;
    private final PasswordEncoder encoder;
    private final String devEmail;
    private final String devPasswort;

    public DevSeedRunner(AppUserRepository userRepository,
                         PasswordEncoder encoder,
                         @Value("${sponsorplatz.dev.email:dev@sponsorplatz.ch}") String devEmail,
                         @Value("${sponsorplatz.dev.passwort:dev}") String devPasswort) {
        this.userRepository = userRepository;
        this.encoder = encoder;
        this.devEmail = devEmail;
        this.devPasswort = devPasswort;
    }

    @Override
    public void run(String... args) {
        if (userRepository.findByEmail(devEmail).isPresent()) {
            return;
        }
        AppUser user = new AppUser();
        user.setEmail(devEmail);
        user.setAnzeigename("Dev User");
        user.setPasswortHash(encoder.encode(devPasswort));
        user.setAktiv(true);
        user.setEmailVerifiziert(true);
        user.setPlatformRolle(PlatformRolle.PLATFORM_ADMIN);
        userRepository.save(user);

        log.warn("DEV-Seed: Test-User mit PLATFORM_ADMIN angelegt -> Login: {} (Passwort siehe sponsorplatz.dev.passwort)", devEmail);
    }
}
