package ch.sponsorplatz.shared.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

/**
 * Stellt einen {@link Clock}-Bean bereit, sodass zeitabhängige Logik
 * (Rechnungsnummer-Jahr, Mahnungs-Fälligkeit, Audit-Zeitstempel-Tests)
 * deterministisch testbar ist — {@code Clock.fixed(...)} im Test, sonst
 * System-Default. Verhindert {@code Instant.now()}/{@code LocalDate.now()}-
 * Streueffekte zur Mitternacht oder über Jahreswechsel hinweg.
 */
@Configuration
public class ClockConfig {

    @Bean
    Clock clock() {
        return Clock.systemDefaultZone();
    }
}
