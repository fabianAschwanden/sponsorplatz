package ch.sponsorplatz.dashboard;

import ch.sponsorplatz.anfrage.RechnungRepository;
import ch.sponsorplatz.anfrage.RechnungsStatus;
import ch.sponsorplatz.anfrage.VertragRepository;
import ch.sponsorplatz.benutzer.AppUser;
import ch.sponsorplatz.benutzer.AppUserRepository;
import ch.sponsorplatz.organisation.MitgliedschaftRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * DASHCHART-01..05 — Berechnung der Sponsoring-Entwicklung + Finanzierungs-Status
 * fürs Dashboard. Bucketierung im Service, SVG-Geometrie im DTO.
 */
@ExtendWith(MockitoExtension.class)
class DashboardChartServiceTest {

    @Mock private AppUserRepository appUserRepository;
    @Mock private MitgliedschaftRepository mitgliedschaftRepository;
    @Mock private VertragRepository vertragRepository;
    @Mock private RechnungRepository rechnungRepository;

    @InjectMocks private DashboardChartService service;

    private AppUser user;
    private UUID orgId;

    @BeforeEach
    void setUp() {
        user = new AppUser();
        user.setId(UUID.randomUUID());
        user.setEmail("v@example.ch");
        orgId = UUID.randomUUID();
    }

    /** DASHCHART-01: Unbekannte E-Mail → leere Charts, keine Repo-Calls auf Verträge/Rechnungen. */
    @Test
    void unbekannterUserGibtLeer() {
        when(appUserRepository.findByEmail("x@x.ch")).thenReturn(Optional.empty());

        DashboardChartDaten daten = service.ladeCharts("x@x.ch", "monat");

        assertThat(daten.entwicklung().hatDaten()).isFalse();
        assertThat(daten.finanzierung().hatDaten()).isFalse();
        verifyNoInteractions(vertragRepository, rechnungRepository);
    }

    /** DASHCHART-02: User ohne Org → leere Charts. */
    @Test
    void userOhneOrgGibtLeer() {
        when(appUserRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
        when(mitgliedschaftRepository.findOrgIdsByUserId(user.getId())).thenReturn(List.of());

        DashboardChartDaten daten = service.ladeCharts(user.getEmail(), "monat");

        assertThat(daten.entwicklung().hatDaten()).isFalse();
        assertThat(daten.finanzierung().hatDaten()).isFalse();
        verifyNoInteractions(vertragRepository, rechnungRepository);
    }

    /** DASHCHART-03: Finanzierungs-Donut rechnet Prozent gesammelt/offen korrekt. */
    @Test
    void finanzierungProzentKorrekt() {
        gemeinsamerSetup();
        when(rechnungRepository.summeBetragChfByOrgUndStatus(anyCollection(), eq(RechnungsStatus.BEZAHLT)))
                .thenReturn(new BigDecimal("7500"));
        when(rechnungRepository.summeBetragChfByOrgUndStatus(anyCollection(), eq(RechnungsStatus.OFFEN)))
                .thenReturn(new BigDecimal("2500"));

        DashboardChartDaten.Finanzierung fin = service.ladeCharts(user.getEmail(), "monat").finanzierung();

        assertThat(fin.hatDaten()).isTrue();
        assertThat(fin.prozentGesammelt()).isEqualTo(75);
        assertThat(fin.prozentOffen()).isEqualTo(25);
        assertThat(fin.gesammeltChf()).contains("7'500");
        assertThat(fin.offenChf()).contains("2'500");
    }

    /** DASHCHART-04: Keine Rechnungen → Donut leer (hatDaten=false), Prozent 0. */
    @Test
    void finanzierungOhneRechnungenLeer() {
        gemeinsamerSetup();
        when(rechnungRepository.summeBetragChfByOrgUndStatus(anyCollection(), any()))
                .thenReturn(BigDecimal.ZERO);

        DashboardChartDaten.Finanzierung fin = service.ladeCharts(user.getEmail(), "monat").finanzierung();

        assertThat(fin.hatDaten()).isFalse();
        assertThat(fin.prozentGesammelt()).isZero();
    }

    /** DASHCHART-05: Unterzeichnete Verträge erzeugen eine Polyline mit Punkten. */
    @Test
    void entwicklungErzeugtPolyline() {
        gemeinsamerSetup();
        Instant jetzt = Instant.now();
        when(vertragRepository.findeUnterzeichnetSeit(anyCollection(), any())).thenReturn(List.of(
                new Object[]{ jetzt.minus(20, ChronoUnit.DAYS), new BigDecimal("3000") },
                new Object[]{ jetzt.minus(2, ChronoUnit.DAYS), new BigDecimal("5000") }
        ));

        DashboardChartDaten.Entwicklung ent = service.ladeCharts(user.getEmail(), "monat").entwicklung();

        assertThat(ent.hatDaten()).isTrue();
        assertThat(ent.punkte()).isNotBlank().contains(",");
        // X-Achsen-Labels für das Monats-Fenster vorhanden.
        assertThat(ent.labels()).isNotEmpty();
    }

    /** Verdrahtet User + Org; Rechnungs-/Vertrags-Stubs setzen die Tests selbst. */
    private void gemeinsamerSetup() {
        when(appUserRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
        when(mitgliedschaftRepository.findOrgIdsByUserId(user.getId())).thenReturn(List.of(orgId));
        lenient().when(vertragRepository.findeUnterzeichnetSeit(anyCollection(), any())).thenReturn(List.of());
        lenient().when(rechnungRepository.summeBetragChfByOrgUndStatus(anyCollection(), any()))
                .thenReturn(BigDecimal.ZERO);
    }
}
