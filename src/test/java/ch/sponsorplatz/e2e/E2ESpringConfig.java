package ch.sponsorplatz.e2e;

import io.cucumber.spring.CucumberContextConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

/**
 * Aktiviert Spring-Boot mit zufaelligem Port + Postgres-Testcontainer fuer
 * die Cucumber-Suite. {@code @CucumberContextConfiguration} sagt Cucumber-
 * Spring, welcher Spring-Kontext fuer die Steps geladen wird.
 *
 * <p>Die Testcontainer-Instanz ist {@code static} und wird einmal pro
 * JVM-Lauf hochgefahren — alle Szenarien teilen sie sich (Kosten amortisiert).
 * Datenbereinigung zwischen Szenarien uebernimmt {@link E2EFixtures}.
 *
 * <p>{@code @ServiceConnection} verbindet die Container-JDBC-URL automatisch
 * mit Spring-DataSource, ohne dass wir
 * {@code spring.datasource.url}-Properties dynamisch setzen muessen.
 */
@CucumberContextConfiguration
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles({"dev", "e2e"})
@Testcontainers
@Import(E2EPlaywrightConfig.class)
public class E2ESpringConfig {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>(
            DockerImageName.parse("postgres:17-alpine"))
            .withDatabaseName("sponsorplatz_e2e")
            .withUsername("test")
            .withPassword("test")
            .withReuse(true);
}
