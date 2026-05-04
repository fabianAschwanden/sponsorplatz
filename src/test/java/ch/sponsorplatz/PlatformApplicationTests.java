package ch.sponsorplatz;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * Smoke-Test SP-01: Spring-Context startet sauber.
 */
@SpringBootTest
@ActiveProfiles("dev")
class PlatformApplicationTests {

    @Test
    void contextLaedt() {
        // Wenn diese Methode ohne Exception durchläuft, ist der Context oben.
    }
}
