package ch.sponsorplatz.e2e;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Smoke-Tests (Phase 10.4) — minimaler Health-Check der Plattform-Wiring
 * vor jedem Pilot-Launch. Bootstrap einer echten Spring-Instanz auf
 * Random-Port, dann HTTP-GETs gegen die wichtigsten Routen.
 *
 * <p>Was wird geprüft (Smoke ≠ Akzeptanz):
 * <ul>
 *   <li>SMOKE-01: {@code GET /} → 200 + Marketing-Inhalt</li>
 *   <li>SMOKE-02: {@code GET /login} → 200 + Form sichtbar</li>
 *   <li>SMOKE-03: {@code GET /kontakt} → 200 + Formular</li>
 *   <li>SMOKE-04: {@code GET /marktplatz} anon → Redirect auf Login (Auth-Gate)</li>
 *   <li>SMOKE-05: {@code GET /actuator/health} → 200, Status UP</li>
 * </ul>
 *
 * <p>Liegt im {@code e2e}-Paket, damit das vorhandene Failsafe-Setup greift:
 * {@code mvn verify -P e2e}. Surefire ignoriert die Klasse (excludes
 * {@code **\/e2e/**}), damit der schnelle Unit-Lauf nicht den Spring-Context
 * mitstartet.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("dev")
class SmokeIT {

    @Autowired
    private TestRestTemplate rest;

    @Test
    @DisplayName("SMOKE-01: GET / → 200 + Marketing-Inhalt")
    void homeIstErreichbar() {
        ResponseEntity<String> r = rest.getForEntity("/", String.class);
        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(r.getBody())
                .contains("Sponsorplatz")
                .doesNotContain("Whitelabel Error Page");
    }

    @Test
    @DisplayName("SMOKE-02: GET /login → 200 + Form sichtbar")
    void loginSeiteIstErreichbar() {
        ResponseEntity<String> r = rest.getForEntity("/login", String.class);
        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(r.getBody())
                .contains("name=\"username\"")
                .contains("name=\"password\"");
    }

    @Test
    @DisplayName("SMOKE-03: GET /kontakt → 200 + Anfrage-Formular")
    void kontaktSeiteIstErreichbar() {
        ResponseEntity<String> r = rest.getForEntity("/kontakt", String.class);
        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(r.getBody())
                .contains("name=\"betreff\"")
                .contains("name=\"nachricht\"");
    }

    /**
     * SMOKE-04: Anonyme Anfrage auf {@code /marktplatz} muss zum Login
     * redirecten — Schutz vor versehentlich offenem Marketplace.
     * TestRestTemplate folgt dem 302 automatisch, also verifizieren wir
     * dass am Ende die Login-Seite ankommt (Form + URL-Pattern).
     */
    @Test
    @DisplayName("SMOKE-04: GET /marktplatz anon → endet auf Login")
    void marktplatzIstAuthGeschuetzt() {
        ResponseEntity<String> r = rest.getForEntity("/marktplatz", String.class);
        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(r.getBody())
                .as("nach Redirect landet ein anonymer User auf /login")
                .contains("name=\"username\"")
                .contains("name=\"password\"");
    }

    @Test
    @DisplayName("SMOKE-05: GET /actuator/health → 200 + UP")
    void healthIstUp() {
        ResponseEntity<String> r = rest.getForEntity("/actuator/health", String.class);
        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(r.getBody()).contains("\"status\":\"UP\"");
    }
}
