package ch.sponsorplatz.ops;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.LoggingEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests für {@link RecentErrorsAppender} + {@link RecentErrorsService}.
 *
 * Test-IDs: OPS-01..03 in {@code specs/TESTSTRATEGIE.md}.
 */
class RecentErrorsServiceTest {

    private RecentErrorsAppender appender;
    private final RecentErrorsService service = new RecentErrorsService();

    @BeforeEach
    void setUp() {
        RecentErrorsAppender.clear();
        appender = new RecentErrorsAppender();
        appender.start();
    }

    @Test
    @DisplayName("OPS-01: ERROR-Events landen im Buffer (INFO/WARN nicht)")
    void nurErrorEvents() {
        appender.doAppend(event(Level.INFO, "info wird ignoriert"));
        appender.doAppend(event(Level.WARN, "warn auch"));
        appender.doAppend(event(Level.ERROR, "echter fehler"));

        assertThat(service.letzteErrors(10)).hasSize(1);
        assertThat(service.letzteErrors(10).get(0).message()).isEqualTo("echter fehler");
        assertThat(service.anzahl()).isEqualTo(1);
    }

    @Test
    @DisplayName("OPS-02: Buffer-Cap respektiert (max 50, neueste behalten)")
    void bufferCap() {
        for (int i = 1; i <= 60; i++) {
            appender.doAppend(event(Level.ERROR, "err " + i));
        }
        var liste = service.letzteErrors(100);
        assertThat(liste).hasSize(50);
        assertThat(liste.get(0).message()).isEqualTo("err 60");        // neuestes
        assertThat(liste.get(49).message()).isEqualTo("err 11");        // ältestes (60-49=11)
    }

    @Test
    @DisplayName("OPS-03: limit-Parameter begrenzt die Rückgabe")
    void limitWirktSichAus() {
        for (int i = 1; i <= 10; i++) {
            appender.doAppend(event(Level.ERROR, "err " + i));
        }
        assertThat(service.letzteErrors(3)).hasSize(3);
        assertThat(service.letzteErrors(100)).hasSize(10);
    }

    private static LoggingEvent event(Level level, String msg) {
        LoggingEvent e = new LoggingEvent();
        e.setLevel(level);
        e.setLoggerName("ch.sponsorplatz.test");
        e.setThreadName("test");
        e.setMessage(msg);
        e.setTimeStamp(System.currentTimeMillis());
        return e;
    }
}
