package ch.sponsorplatz.anfrage;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/**
 * Tests für {@link RechnungsnummerGenerator}.
 *
 * Test-IDs: RECH-07..09 — siehe specs/SPONSORING_ZAHLUNGSFLUSS.md §5.
 */
@ExtendWith(MockitoExtension.class)
class RechnungsnummerGeneratorTest {

    @Mock private RechnungRepository repository;

    private final UUID orgId = UUID.randomUUID();
    private final Clock clock2026 = Clock.fixed(
            LocalDate.of(2026, 5, 13).atStartOfDay(ZoneId.systemDefault()).toInstant(),
            ZoneId.systemDefault());

    private RechnungsnummerGenerator generator;

    @BeforeEach
    void setUp() {
        generator = new RechnungsnummerGenerator(repository, clock2026);
    }

    @Test
    @DisplayName("RECH-07: Format R-YYYY-NNNNN, 5-stellige Nummer, pro Org-Jahr fortlaufend")
    void format() {
        when(repository.findeMaxLfdNr(orgId, "R-2026-%")).thenReturn(null);

        assertThat(generator.naechste(orgId)).isEqualTo("R-2026-00001");
    }

    @Test
    @DisplayName("RECH-07b: bei vorhandenen Rechnungen zählt der Generator um 1 hoch")
    void hochzaehlen() {
        when(repository.findeMaxLfdNr(orgId, "R-2026-%")).thenReturn(41);

        assertThat(generator.naechste(orgId)).isEqualTo("R-2026-00042");
    }

    @Test
    @DisplayName("RECH-08: Jahres-Rollover — neues Jahr startet wieder bei 1")
    void jahresRolloverStartetBeiEins() {
        Clock clock2027 = Clock.fixed(
                LocalDate.of(2027, 1, 1).atStartOfDay(ZoneId.systemDefault()).toInstant(),
                ZoneId.systemDefault());
        RechnungsnummerGenerator gen2027 = new RechnungsnummerGenerator(repository, clock2027);
        // Wichtig: 2027-Query liefert null, auch wenn 2026-Rechnungen existieren —
        // weil unsere Query nach Praefix "R-2027-%" filtert.
        when(repository.findeMaxLfdNr(orgId, "R-2027-%")).thenReturn(null);

        assertThat(gen2027.naechste(orgId)).isEqualTo("R-2027-00001");
    }

    @Test
    @DisplayName("RECH-09: Lückenlosigkeit — stornierte Rechnungen behalten Nummer, nächste = max+1")
    void lueckenlosTrotzStorno() {
        // findeMaxLfdNr zählt alle Rechnungen (auch STORNIERT) mit — das Repo-Query
        // filtert NICHT nach Status. Die nächste Nummer überspringt also keine Lücke.
        when(repository.findeMaxLfdNr(orgId, "R-2026-%")).thenReturn(42);

        assertThat(generator.naechste(orgId)).isEqualTo("R-2026-00043");
    }
}
