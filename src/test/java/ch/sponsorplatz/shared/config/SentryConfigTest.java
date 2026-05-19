package ch.sponsorplatz.shared.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ch.sponsorplatz.shared.exception.NotFoundException;
import io.sentry.SentryEvent;
import io.sentry.SentryOptions;
import io.sentry.protocol.User;

/**
 * Tests für Phase 10.2 — Sentry Error-Tracking Konfiguration.
 *
 * <p>Testet den BeforeSend-Callback (DSG-Filter):
 * <ul>
 *   <li>SENTRY-01: NotFoundException wird gefiltert (kein Noise)</li>
 *   <li>SENTRY-02: IllegalArgumentException wird gefiltert</li>
 *   <li>SENTRY-03: RuntimeException wird durchgelassen</li>
 *   <li>SENTRY-04: User-IP wird aus Events entfernt (DSG)</li>
 * </ul>
 */
class SentryConfigTest {

    private final SentryConfig config = new SentryConfig();
    private final SentryOptions.BeforeSendCallback callback = config.sentryBeforeSendCallback("test-env");

    @Test
    @DisplayName("SENTRY-01: NotFoundException wird gefiltert — kein Event an Sentry")
    void notFoundExceptionWirdGefiltert() {
        SentryEvent event = new SentryEvent(new NotFoundException("Org nicht gefunden"));
        SentryEvent result = callback.execute(event, null);
        assertThat(result).isNull();
    }

    @Test
    @DisplayName("SENTRY-02: IllegalArgumentException wird gefiltert")
    void illegalArgumentWirdGefiltert() {
        SentryEvent event = new SentryEvent(new IllegalArgumentException("Ungültiger Slug"));
        SentryEvent result = callback.execute(event, null);
        assertThat(result).isNull();
    }

    @Test
    @DisplayName("SENTRY-03: RuntimeException wird an Sentry gesendet")
    void runtimeExceptionWirdGesendet() {
        SentryEvent event = new SentryEvent(new RuntimeException("Unerwarteter Fehler"));
        SentryEvent result = callback.execute(event, null);
        assertThat(result).isNotNull();
    }

    @Test
    @DisplayName("SENTRY-04: User-IP wird aus Events entfernt (DSG)")
    void userIpWirdEntfernt() {
        SentryEvent event = new SentryEvent(new RuntimeException("Test"));
        User user = new User();
        user.setIpAddress("192.168.1.1");
        event.setUser(user);

        SentryEvent result = callback.execute(event, null);

        assertThat(result).isNotNull();
        assertThat(result.getUser().getIpAddress()).isNull();
    }

    @Test
    @DisplayName("SENTRY-05: Event ohne Exception wird durchgelassen")
    void eventOhneExceptionWirdDurchgelassen() {
        SentryEvent event = new SentryEvent();
        SentryEvent result = callback.execute(event, null);
        assertThat(result).isNotNull();
    }
}

