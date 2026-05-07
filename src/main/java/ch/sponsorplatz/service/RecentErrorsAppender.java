package ch.sponsorplatz.service;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.IThrowableProxy;
import ch.qos.logback.classic.spi.ThrowableProxyUtil;
import ch.qos.logback.core.AppenderBase;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Logback-Appender, der die letzten {@value #MAX_ENTRIES} ERROR-Events
 * in-memory hält für das Ops-Dashboard ({@code /admin/system}).
 *
 * <p>Buffer ist statisch zugänglich, damit {@link RecentErrorsService} ihn
 * unabhängig vom Logback-/Spring-Lifecycle lesen kann (Logback wird vor
 * dem Spring-Context initialisiert).
 *
 * <p>Speicherbudget: ~50 KB bei {@value #MAX_ENTRIES} Einträgen mit Stacktrace.
 */
public class RecentErrorsAppender extends AppenderBase<ILoggingEvent> {

    private static final int MAX_ENTRIES = 50;

    private static final ConcurrentLinkedDeque<RecentError> BUFFER = new ConcurrentLinkedDeque<>();
    private static final AtomicInteger SIZE = new AtomicInteger(0);

    @Override
    protected void append(ILoggingEvent event) {
        if (event.getLevel().toInt() < Level.ERROR_INT) return;
        BUFFER.addFirst(new RecentError(
                LocalDateTime.ofInstant(
                        Instant.ofEpochMilli(event.getTimeStamp()),
                        ZoneId.systemDefault()),
                event.getLevel().toString(),
                event.getLoggerName(),
                event.getThreadName(),
                event.getFormattedMessage(),
                stacktraceOrNull(event.getThrowableProxy())
        ));
        if (SIZE.incrementAndGet() > MAX_ENTRIES) {
            BUFFER.pollLast();
            SIZE.decrementAndGet();
        }
    }

    private static String stacktraceOrNull(IThrowableProxy proxy) {
        return proxy == null ? null : ThrowableProxyUtil.asString(proxy);
    }

    /** Snapshot — neueste zuerst, immutable. */
    public static List<RecentError> snapshot() {
        return Collections.unmodifiableList(new ArrayList<>(BUFFER));
    }

    /** Anzahl Errors seit App-Start (mit Overflow-Begrenzung). */
    public static int aktuelleAnzahl() {
        return SIZE.get();
    }

    static void clear() {
        BUFFER.clear();
        SIZE.set(0);
    }

    public record RecentError(LocalDateTime timestamp, String level, String logger,
                              String thread, String message, String stacktrace) {}
}
