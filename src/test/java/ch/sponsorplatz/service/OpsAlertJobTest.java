package ch.sponsorplatz.service;

import ch.sponsorplatz.dto.SystemSnapshotView;
import ch.sponsorplatz.dto.SystemSnapshotView.Cpu;
import ch.sponsorplatz.dto.SystemSnapshotView.DbPool;
import ch.sponsorplatz.dto.SystemSnapshotView.DbSize;
import ch.sponsorplatz.dto.SystemSnapshotView.Heap;
import ch.sponsorplatz.dto.SystemSnapshotView.Jvm;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests für {@link OpsAlertJob} — Schwellen-Check + Throttling.
 *
 * Test-IDs: OPS-04..07 in {@code specs/TESTSTRATEGIE.md}.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class OpsAlertJobTest {

    @Mock private SystemSnapshotService snapshotService;
    @Mock private MailService mailService;

    private OpsAlertJob job;

    @BeforeEach
    void setUp() {
        // heapGrenze=80, cpuGrenze=80, errorGrenze=3, minPause=60min
        job = new OpsAlertJob(snapshotService, mailService, 80.0, 80.0, 3, 60);
        when(mailService.effektiverTestEmpfaenger()).thenReturn("admin@example.ch");
    }

    @Test
    @DisplayName("OPS-04: Heap unter Grenze → keine Mail")
    void heapUnterGrenzeKeineMail() {
        when(snapshotService.snapshot(5)).thenReturn(snapshot(50.0, 10.0, 0));

        job.pruefeUndAlarmiere();

        verify(mailService, never()).sendePlain(any(), any(), any());
    }

    @Test
    @DisplayName("OPS-05: Heap über Grenze → Mail an Test-Empfänger mit Heap-Details")
    void heapUeberGrenzeMail() {
        when(snapshotService.snapshot(5)).thenReturn(snapshot(95.0, 10.0, 0));

        job.pruefeUndAlarmiere();

        verify(mailService).sendePlain(eq("admin@example.ch"),
                contains("Heap-Auslastung kritisch"),
                contains("95"));
    }

    @Test
    @DisplayName("OPS-06: Wiederholter Alert innerhalb min-pause → unterdrückt")
    void wiederholterAlertGedrosselt() {
        when(snapshotService.snapshot(5)).thenReturn(snapshot(95.0, 10.0, 0));

        job.pruefeUndAlarmiere();
        job.pruefeUndAlarmiere();

        verify(mailService, times(1)).sendePlain(any(), any(), any());
    }

    @Test
    @DisplayName("OPS-07: Neue Errors über Schwelle → Alert mit Error-Vorschau")
    void neueErrorsAlert() {
        // 1. Tick: 0 Errors — Baseline gesetzt
        when(snapshotService.snapshot(5)).thenReturn(snapshot(50.0, 10.0, 0));
        job.pruefeUndAlarmiere();

        // 2. Tick: 5 neue Errors → über Schwelle 3
        when(snapshotService.snapshot(5)).thenReturn(snapshot(50.0, 10.0, 5));
        job.pruefeUndAlarmiere();

        verify(mailService).sendePlain(eq("admin@example.ch"),
                contains("Errors seit letztem Check"),
                contains("5 neue Error-Events"));
    }

    private SystemSnapshotView snapshot(double heapPct, double cpuPct, int errorTotal) {
        return new SystemSnapshotView(
                new Heap(100, 200, heapPct),
                new Cpu(cpuPct, cpuPct, 0.5),
                "00:01",
                LocalDateTime.now().minusMinutes(1),
                new DbPool(1, 2, 8, 0),
                new DbSize(true, 1024 * 1024),
                new Jvm("Test", "21", "VM", "1.0"),
                Map.of(),
                List.of(),
                errorTotal,
                LocalDateTime.now()
        );
    }
}
