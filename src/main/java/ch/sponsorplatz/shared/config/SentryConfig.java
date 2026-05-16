package ch.sponsorplatz.shared.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.sentry.SentryOptions;

/**
 * DSG-konforme Sentry-Konfiguration.
 *
 * <p>Aktivierung: ENV {@code SENTRY_DSN=https://...@sentry.io/...} oder
 * {@code SENTRY_DSN=https://...@glitchtip.example.com/...} (self-hosted, DSG-konform).
 *
 * <p>DSG-Massnahmen:
 * <ul>
 *   <li>{@code send-default-pii=false} — keine IP, keine Cookies, keine User-E-Mail</li>
 *   <li>{@code traces-sample-rate=0} — kein Performance-/Session-Tracking</li>
 *   <li>BeforeSend-Callback filtert 404-NotFoundException und 400-Errors (kein Noise)</li>
 *   <li>IP-Adresse explizit aus Events entfernt (Defense in Depth)</li>
 *   <li>Nur {@code ch.sponsorplatz}-Stackframes als in-app markiert</li>
 * </ul>
 *
 * <p>Der {@code sentry-spring-boot-starter-jakarta} registriert automatisch einen
 * {@code SentryExceptionResolver}, einen {@code SentrySpringFilter} und die
 * Logback-Appender-Integration. Diese Config ergänzt nur den DSG-konformen
 * Event-Filter.
 */
@Configuration
public class SentryConfig {

    private static final Logger log = LoggerFactory.getLogger(SentryConfig.class);

    /**
     * Filtert Events bevor sie an Sentry gesendet werden:
     * - 404 NotFoundException und 400 IllegalArgumentException verwerfen (Business-Errors, kein Noise)
     * - User-IP prophylaktisch entfernen (DSG)
     */
    @Bean
    @ConditionalOnProperty(name = "sentry.dsn", matchIfMissing = false)
    public SentryOptions.BeforeSendCallback sentryBeforeSendCallback() {
        log.info("Sentry Error-Tracking aktiv — DSG-konform (keine PII, kein Replay)");
        return (event, hint) -> {
            // Erwartete Business-Exceptions nicht an Sentry senden
            if (event.getThrowable() != null) {
                Throwable throwable = event.getThrowable();
                String className = throwable.getClass().getName();
                if (className.contains("NotFoundException")
                        || throwable instanceof IllegalArgumentException) {
                    return null; // Event verwerfen
                }
            }

            // User-IP explizit entfernen (Defense in Depth, auch wenn send-default-pii=false)
            if (event.getUser() != null) {
                event.getUser().setIpAddress(null);
            }

            return event;
        };
    }
}
