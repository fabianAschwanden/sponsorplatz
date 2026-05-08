package ch.sponsorplatz.ops;

import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Service;

import java.lang.management.ManagementFactory;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.OptionalLong;

/**
 * Aggregiert Laufzeit-Daten aus dem MeterRegistry + Repositories zu einem
 * {@link SystemSnapshotView} — Datenkanal für Ops-Dashboard und Alert-Job.
 */
@Service
public class SystemSnapshotService {

    private final MeterRegistry registry;
    private final DatabaseStatsService databaseStats;
    private final BucketStatsService bucketStats;
    private final RecentErrorsService recentErrors;

    public SystemSnapshotService(MeterRegistry registry,
                                 DatabaseStatsService databaseStats,
                                 BucketStatsService bucketStats,
                                 RecentErrorsService recentErrors) {
        this.registry = registry;
        this.databaseStats = databaseStats;
        this.bucketStats = bucketStats;
        this.recentErrors = recentErrors;
    }

    public SystemSnapshotView snapshot(int recentErrorsLimit) {
        return new SystemSnapshotView(
                heap(),
                cpu(),
                uptimeFormatiert(),
                gestartetAm(),
                dbPool(),
                dbSize(),
                jvm(),
                bucketStats.alleStats(),
                recentErrors.letzteErrors(recentErrorsLimit),
                recentErrors.anzahl(),
                LocalDateTime.now()
        );
    }

    public SystemSnapshotView.Heap heap() {
        long used = sumGaugeBytes("jvm.memory.used", "heap");
        long max = sumGaugeBytes("jvm.memory.max", "heap");
        double pct = max > 0 ? (used * 100.0 / max) : 0;
        return new SystemSnapshotView.Heap(used / 1024 / 1024, max / 1024 / 1024, pct);
    }

    public SystemSnapshotView.Cpu cpu() {
        return new SystemSnapshotView.Cpu(
                clamp(value("process.cpu.usage")) * 100,
                clamp(value("system.cpu.usage")) * 100,
                Math.max(0, value("system.load.average.1m"))
        );
    }

    private String uptimeFormatiert() {
        Duration d = Duration.ofSeconds((long) value("process.uptime"));
        long days = d.toDays();
        long hours = d.toHours() % 24;
        long minutes = d.toMinutes() % 60;
        return days > 0
                ? String.format("%dd %02d:%02d", days, hours, minutes)
                : String.format("%02d:%02d", hours, minutes);
    }

    private LocalDateTime gestartetAm() {
        return LocalDateTime.ofInstant(
                Instant.ofEpochSecond((long) value("process.start.time")),
                ZoneId.systemDefault());
    }

    public SystemSnapshotView.DbPool dbPool() {
        return new SystemSnapshotView.DbPool(
                (long) value("hikaricp.connections.active"),
                (long) value("hikaricp.connections.idle"),
                (long) value("hikaricp.connections.max"),
                (long) value("hikaricp.connections.pending")
        );
    }

    private SystemSnapshotView.DbSize dbSize() {
        OptionalLong bytes = databaseStats.groesseInBytes();
        return new SystemSnapshotView.DbSize(bytes.isPresent(), bytes.orElse(0));
    }

    private SystemSnapshotView.Jvm jvm() {
        var runtime = ManagementFactory.getRuntimeMXBean();
        return new SystemSnapshotView.Jvm(
                System.getProperty("java.vendor"),
                System.getProperty("java.version"),
                runtime.getVmName(),
                runtime.getVmVersion()
        );
    }

    private long sumGaugeBytes(String name, String areaTag) {
        return Math.round(
                registry.find(name).tag("area", areaTag).gauges().stream()
                        .mapToDouble(g -> g.value())
                        .sum()
        );
    }

    private double value(String name) {
        var gauge = registry.find(name).gauge();
        return gauge != null ? gauge.value() : 0;
    }

    private static double clamp(double v) {
        return Double.isNaN(v) || v < 0 ? 0 : v;
    }
}
