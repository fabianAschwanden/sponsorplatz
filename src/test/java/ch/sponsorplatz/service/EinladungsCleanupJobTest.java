package ch.sponsorplatz.service;

import ch.sponsorplatz.repository.EinladungRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Test für den Scheduled-Cleanup-Job (EINL-14).
 * Verifiziert, dass {@code loescheAbgelaufene()} das Repository mit einem
 * Cutoff von „jetzt" aufruft — alle Einladungen davor werden gelöscht.
 */
@ExtendWith(MockitoExtension.class)
class EinladungsCleanupJobTest {

    @Mock
    private EinladungRepository einladungRepository;

    @InjectMocks
    private EinladungsCleanupJob job;

    /** EINL-14: loescheAbgelaufene ruft deleteByGueltigBisBefore mit Cutoff ≈ now() auf. */
    @Test
    void loescheAbgelaufeneRuftRepositoryMitNowCutoffAuf() {
        when(einladungRepository.deleteByGueltigBisBefore(any(Instant.class))).thenReturn(3);

        Instant vorher = Instant.now();
        job.loescheAbgelaufene();
        Instant nachher = Instant.now();

        ArgumentCaptor<Instant> captor = ArgumentCaptor.forClass(Instant.class);
        verify(einladungRepository).deleteByGueltigBisBefore(captor.capture());
        Instant cutoff = captor.getValue();
        assertThat(cutoff).isBetween(vorher, nachher);
    }
}
