package ch.sponsorplatz.anfrage;

import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Generiert Rechnungsnummern im Format {@code R-YYYY-NNNNN} pro Org-Jahr
 * fortlaufend. Lückenlose Nummerierung (auch stornierte Rechnungen behalten
 * ihre Nummer) ist Buchhaltungs-Pflicht in der Schweiz (OR Art. 957 ff.,
 * siehe SPONSORING_ZAHLUNGSFLUSS.md §5).
 *
 * <p>Beispiele: {@code R-2026-00001}, {@code R-2026-00042}, {@code R-2027-00001}
 * (Jahres-Rollover startet wieder bei 1).
 *
 * <p>Eigene Komponente — nicht im {@link RechnungService} inline — damit der
 * Generator unabhängig vom Persistenz-Flow getestet werden kann (RECH-07..09)
 * und in einem späteren Refactor durch eine eigene Sequenz-Tabelle ersetzt
 * werden kann (siehe Spec §5.2 Thread-Safety-Hinweis).
 */
@Component
public class RechnungsnummerGenerator {

    private static final String PREFIX_FORMAT = "R-%d-";
    private static final String LFD_NR_FORMAT = "%05d";

    private final RechnungRepository repository;
    private final Clock clock;

    public RechnungsnummerGenerator(RechnungRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    /**
     * Nächste Rechnungsnummer für eine Org im aktuellen Jahr.
     *
     * <p>Race-Condition-Hinweis: aktuell pro Service-Tx vergeben. Bei Parallel-
     * Lastspitzen (zwei Owner erstellen gleichzeitig) kann eine UNIQUE-
     * Constraint-Violation auf {@code (org_id, rechnungsnummer)} auftreten.
     * Lösung dafür ist die in §5.2 erwähnte Sequenz-Tabelle — Backlog.
     */
    public String naechste(UUID orgId) {
        int jahr = LocalDate.now(clock).getYear();
        String praefix = PREFIX_FORMAT.formatted(jahr);
        Integer maxLfd = repository.findeMaxLfdNr(orgId, praefix + "%");
        int naechsteLfd = (maxLfd == null ? 0 : maxLfd) + 1;
        return praefix + LFD_NR_FORMAT.formatted(naechsteLfd);
    }
}
