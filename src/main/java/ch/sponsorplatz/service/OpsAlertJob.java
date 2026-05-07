package ch.sponsorplatz.service;

import ch.sponsorplatz.dto.SystemSnapshotView;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Geplanter Schwellen-Check + Mail-Alert für Plattform-Admin.
 *
 * <p>Cron: alle 15 Minuten. Prüft Heap-Auslastung, CPU, neue Errors seit
 * letztem Tick und alarmiert per {@link MailService#sendePlain} an den
 * konfigurierten Test-Empfänger (DB &gt; ENV).
 *
 * <p>Throttling: Pro Schwellen-Typ wird höchstens alle 60 Minuten gemailt
 * (verhindert Mail-Sturm bei dauerhaft erhöhtem Heap).
 *
 * <p>Konfigurierbare Schwellen:
 * <pre>
 *   sponsorplatz.alerts.heap-prozent=85
 *   sponsorplatz.alerts.cpu-prozent=85
 *   sponsorplatz.alerts.error-grenze=5
 *   sponsorplatz.alerts.min-pause-minuten=60
 * </pre>
 */
@Component
public class OpsAlertJob {

    private static final Logger log = LoggerFactory.getLogger(OpsAlertJob.class);

    private final SystemSnapshotService snapshotService;
    private final MailService mailService;

    private final double heapGrenze;
    private final double cpuGrenze;
    private final int errorGrenze;
    private final long minPauseMinuten;

    private final Map<String, Instant> letzterAlert = new LinkedHashMap<>();
    private int errorAnzahlBeimLetztenTick = 0;

    public OpsAlertJob(SystemSnapshotService snapshotService,
                       MailService mailService,
                       @Value("${sponsorplatz.alerts.heap-prozent:85}") double heapGrenze,
                       @Value("${sponsorplatz.alerts.cpu-prozent:85}") double cpuGrenze,
                       @Value("${sponsorplatz.alerts.error-grenze:5}") int errorGrenze,
                       @Value("${sponsorplatz.alerts.min-pause-minuten:60}") long minPauseMinuten) {
        this.snapshotService = snapshotService;
        this.mailService = mailService;
        this.heapGrenze = heapGrenze;
        this.cpuGrenze = cpuGrenze;
        this.errorGrenze = errorGrenze;
        this.minPauseMinuten = minPauseMinuten;
    }

    @Scheduled(cron = "${sponsorplatz.alerts.cron:0 */15 * * * *}")
    public void pruefeUndAlarmiere() {
        try {
            SystemSnapshotView snap = snapshotService.snapshot(5);

            if (snap.heap().prozent() > heapGrenze) {
                vielleichtAlarmieren("heap",
                        "Heap-Auslastung kritisch",
                        String.format("Heap %.1f%% (Grenze %.1f%%) — %d von %d MB belegt.",
                                snap.heap().prozent(), heapGrenze, snap.heap().usedMb(), snap.heap().maxMb()));
            }

            if (snap.cpu().processProzent() > cpuGrenze) {
                vielleichtAlarmieren("cpu",
                        "CPU-Auslastung kritisch",
                        String.format("Process-CPU %.1f%% (Grenze %.1f%%), Load 1m %.2f.",
                                snap.cpu().processProzent(), cpuGrenze, snap.cpu().load1m()));
            }

            int neueErrors = snap.errorAnzahlTotal() - errorAnzahlBeimLetztenTick;
            errorAnzahlBeimLetztenTick = snap.errorAnzahlTotal();
            if (neueErrors >= errorGrenze) {
                String details = snap.recentErrors().stream()
                        .limit(3)
                        .map(e -> "  • [" + e.level() + "] " + e.message())
                        .reduce("", (a, b) -> a + b + "\n");
                vielleichtAlarmieren("errors",
                        "Mehrere Errors seit letztem Check",
                        String.format("%d neue Error-Events in den letzten ~15 Minuten.%n%n%s",
                                neueErrors, details));
            }
        } catch (RuntimeException e) {
            log.warn("Ops-Alert-Job fehlgeschlagen: {}", e.getMessage());
        }
    }

    private void vielleichtAlarmieren(String typ, String betreff, String body) {
        Instant zuletzt = letzterAlert.get(typ);
        if (zuletzt != null && zuletzt.isAfter(Instant.now().minus(minPauseMinuten, ChronoUnit.MINUTES))) {
            log.debug("Alert '{}' geblockt — letzter Alarm {} (min-pause {} min)", typ, zuletzt, minPauseMinuten);
            return;
        }
        String empfaenger = mailService.effektiverTestEmpfaenger();
        if (empfaenger == null || empfaenger.isBlank()) {
            log.warn("Alert '{}' nicht gesendet — kein Empfänger konfiguriert (sponsorplatz.mail.test-empfaenger). Body: {}", typ, body);
            return;
        }
        try {
            mailService.sendePlain(empfaenger, "[Sponsorplatz Ops] " + betreff,
                    body + "\n\nDashboard: https://sponsorplatz.for-better.biz/admin/system");
            letzterAlert.put(typ, Instant.now());
            log.info("Ops-Alert '{}' an {} gesendet", typ, empfaenger);
        } catch (RuntimeException e) {
            log.warn("Ops-Alert '{}' konnte nicht gesendet werden: {}", typ, e.getMessage());
        }
    }
}
