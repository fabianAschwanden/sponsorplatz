package ch.sponsorplatz.dto;

import ch.sponsorplatz.service.BucketStatsService.BucketStats;
import ch.sponsorplatz.service.RecentErrorsAppender.RecentError;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Aggregierter Snapshot des Laufzeit-Status für das Ops-Dashboard
 * (Heap, CPU, Uptime, DB-Pool, JVM, DB-Grösse, Bucket-Stats, ErrorEvents).
 *
 * <p>Wird sowohl per HTML-Render als auch per JSON-Endpoint
 * ({@code /admin/system/data}) ausgeliefert.
 */
public record SystemSnapshotView(
        Heap heap,
        Cpu cpu,
        String uptime,
        LocalDateTime gestartetAm,
        DbPool dbPool,
        DbSize dbSize,
        Jvm jvm,
        Map<String, BucketStats> buckets,
        List<RecentError> recentErrors,
        int errorAnzahlTotal,
        LocalDateTime zeitstempel
) {
    public record Heap(long usedMb, long maxMb, double prozent) {}
    public record Cpu(double processProzent, double systemProzent, double load1m) {}
    public record DbPool(long active, long idle, long max, long pending) {}
    public record DbSize(boolean verfuegbar, long bytes) {
        public double mb() { return bytes / 1024.0 / 1024.0; }
    }
    public record Jvm(String vendor, String version, String vmName, String vmVersion) {}
}
