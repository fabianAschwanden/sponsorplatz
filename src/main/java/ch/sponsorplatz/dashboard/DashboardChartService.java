package ch.sponsorplatz.dashboard;

import ch.sponsorplatz.anfrage.RechnungRepository;
import ch.sponsorplatz.anfrage.RechnungsStatus;
import ch.sponsorplatz.anfrage.VertragRepository;
import ch.sponsorplatz.benutzer.AppUserRepository;
import ch.sponsorplatz.organisation.MitgliedschaftRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.time.temporal.IsoFields;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * Berechnet die beiden Dashboard-Charts aus echten Daten:
 * <ul>
 *   <li><b>Sponsoring-Entwicklung</b> — neu unterzeichnete Vertragsvolumen pro
 *       Zeit-Bucket über das gewählte Fenster (Woche/Monat/Jahr).</li>
 *   <li><b>Finanzierungs-Status</b> — bezahlte vs. offene Rechnungssummen der
 *       Vereins-Orgs des Users.</li>
 * </ul>
 *
 * <p>Scoping wie das übrige Dashboard: über die Org-Mitgliedschaften des Users.
 * Bucketierung läuft im Service (H2/PG-portabel, keine DB-Datumsfunktionen),
 * die SVG-Geometrie im {@link DashboardChartDaten}.
 */
@Service
@Transactional(readOnly = true)
public class DashboardChartService {

    private static final ZoneId ZONE = ZoneId.of("Europe/Zurich");
    private static final Locale DE = Locale.GERMAN;

    private final AppUserRepository appUserRepository;
    private final MitgliedschaftRepository mitgliedschaftRepository;
    private final VertragRepository vertragRepository;
    private final RechnungRepository rechnungRepository;

    public DashboardChartService(AppUserRepository appUserRepository,
                                 MitgliedschaftRepository mitgliedschaftRepository,
                                 VertragRepository vertragRepository,
                                 RechnungRepository rechnungRepository) {
        this.appUserRepository = appUserRepository;
        this.mitgliedschaftRepository = mitgliedschaftRepository;
        this.vertragRepository = vertragRepository;
        this.rechnungRepository = rechnungRepository;
    }

    /**
     * Lädt beide Charts für den User. {@code zeitraum} ∈ {woche, monat, jahr};
     * unbekannte Werte fallen auf „monat" zurück.
     */
    public DashboardChartDaten ladeCharts(String email, String zeitraum) {
        List<UUID> orgIds = appUserRepository.findByEmail(email)
                .map(u -> mitgliedschaftRepository.findOrgIdsByUserId(u.getId()))
                .orElseGet(List::of);
        if (orgIds.isEmpty()) {
            return new DashboardChartDaten(
                    DashboardChartDaten.entwicklungAus(List.of(), List.of()),
                    DashboardChartDaten.finanzierungAus(BigDecimal.ZERO, BigDecimal.ZERO));
        }
        return new DashboardChartDaten(
                ladeEntwicklung(orgIds, Zeitfenster.von(zeitraum)),
                ladeFinanzierung(orgIds));
    }

    private DashboardChartDaten.Entwicklung ladeEntwicklung(List<UUID> orgIds, Zeitfenster fenster) {
        LocalDate heute = LocalDate.now(ZONE);
        List<Bucket> buckets = fenster.buckets(heute);
        Instant ab = buckets.get(0).von.atStartOfDay(ZONE).toInstant();

        List<Object[]> rohdaten = vertragRepository.findeUnterzeichnetSeit(orgIds, ab);
        for (Object[] zeile : rohdaten) {
            Instant unterzeichnetAm = (Instant) zeile[0];
            BigDecimal preis = (BigDecimal) zeile[1];
            LocalDate tag = unterzeichnetAm.atZone(ZONE).toLocalDate();
            for (Bucket b : buckets) {
                if (!tag.isBefore(b.von) && !tag.isAfter(b.bis)) {
                    b.summe = b.summe.add(preis);
                    break;
                }
            }
        }
        List<BigDecimal> summen = buckets.stream().map(b -> b.summe).toList();
        List<String> labels = buckets.stream().map(b -> b.label).toList();
        return DashboardChartDaten.entwicklungAus(summen, labels);
    }

    private DashboardChartDaten.Finanzierung ladeFinanzierung(List<UUID> orgIds) {
        BigDecimal gesammelt = rechnungRepository
                .summeBetragChfByOrgUndStatus(orgIds, RechnungsStatus.BEZAHLT);
        BigDecimal offen = rechnungRepository
                .summeBetragChfByOrgUndStatus(orgIds, RechnungsStatus.OFFEN);
        return DashboardChartDaten.finanzierungAus(gesammelt, offen);
    }

    /** Ein Zeit-Bucket auf der X-Achse mit Datumsgrenzen + Anzeige-Label. */
    private static final class Bucket {
        final LocalDate von;
        final LocalDate bis;
        final String label;
        BigDecimal summe = BigDecimal.ZERO;

        Bucket(LocalDate von, LocalDate bis, String label) {
            this.von = von;
            this.bis = bis;
            this.label = label;
        }
    }

    /** Definiert Anzahl + Granularität der Buckets je Zeitraum. */
    private enum Zeitfenster {
        /** Letzte 7 Tage, täglich. */
        WOCHE {
            @Override List<Bucket> buckets(LocalDate heute) {
                DateTimeFormatter tag = DateTimeFormatter.ofPattern("EE", DE);
                List<Bucket> b = new ArrayList<>();
                for (int i = 6; i >= 0; i--) {
                    LocalDate d = heute.minusDays(i);
                    b.add(new Bucket(d, d, d.format(tag)));
                }
                return b;
            }
        },
        /** Letzte ~4 Wochen, je Kalenderwoche. */
        MONAT {
            @Override List<Bucket> buckets(LocalDate heute) {
                List<Bucket> b = new ArrayList<>();
                LocalDate wochenStart = heute.minusWeeks(3).with(java.time.DayOfWeek.MONDAY);
                for (int i = 0; i < 4; i++) {
                    LocalDate von = wochenStart.plusWeeks(i);
                    LocalDate bis = von.plusDays(6);
                    int kw = von.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR);
                    b.add(new Bucket(von, bis, "KW " + kw));
                }
                return b;
            }
        },
        /** Letzte 12 Monate, je Kalendermonat. */
        JAHR {
            @Override List<Bucket> buckets(LocalDate heute) {
                List<Bucket> b = new ArrayList<>();
                LocalDate monatsStart = heute.withDayOfMonth(1).minusMonths(11);
                for (int i = 0; i < 12; i++) {
                    LocalDate von = monatsStart.plusMonths(i);
                    LocalDate bis = von.withDayOfMonth(von.lengthOfMonth());
                    String label = von.getMonth().getDisplayName(TextStyle.SHORT, DE);
                    b.add(new Bucket(von, bis, label));
                }
                return b;
            }
        };

        abstract List<Bucket> buckets(LocalDate heute);

        static Zeitfenster von(String zeitraum) {
            if (zeitraum == null) {
                return MONAT;
            }
            return switch (zeitraum.toLowerCase(Locale.ROOT)) {
                case "woche" -> WOCHE;
                case "jahr" -> JAHR;
                default -> MONAT;
            };
        }
    }
}
