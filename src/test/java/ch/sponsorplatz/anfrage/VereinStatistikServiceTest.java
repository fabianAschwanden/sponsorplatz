package ch.sponsorplatz.anfrage;

import ch.sponsorplatz.benutzer.AppUser;
import ch.sponsorplatz.benutzer.AppUserRepository;
import ch.sponsorplatz.organisation.Mitgliedschaft;
import ch.sponsorplatz.organisation.MitgliedschaftRepository;
import ch.sponsorplatz.organisation.OrgTyp;
import ch.sponsorplatz.organisation.Organisation;
import ch.sponsorplatz.projekt.ProjektService;
import ch.sponsorplatz.projekt.SponsoringPaketRepository;
import ch.sponsorplatz.shared.exception.NotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/**
 * Tests für {@link VereinStatistikService}.
 * Test-IDs: STAT-VEREIN-01..07.
 *
 * <p>Spiegel zu {@link SponsorStatistikServiceTest} — gleiche Test-Topologie,
 * andere Aggregation (Verein-Sicht: eingehend/ausgehend, Pakete, Einnahmen
 * via Vertrag.org statt Vertrag.sponsorOrg).
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class VereinStatistikServiceTest {

    @Mock private AppUserRepository appUserRepository;
    @Mock private MitgliedschaftRepository mitgliedschaftRepository;
    @Mock private ProjektService projektService;
    @Mock private SponsoringPaketRepository paketRepository;
    @Mock private VertragRepository vertragRepository;
    @Mock private SponsoringAnfrageRepository anfrageRepository;
    @Mock private RechnungRepository rechnungRepository;

    private VereinStatistikService service;

    private AppUser user;
    private Organisation vereinOrg;

    @BeforeEach
    void setUp() {
        service = new VereinStatistikService(appUserRepository, mitgliedschaftRepository,
                projektService, paketRepository, vertragRepository, anfrageRepository,
                rechnungRepository);

        user = new AppUser();
        user.setId(UUID.randomUUID());
        user.setEmail("vorstand@verein.test");

        vereinOrg = new Organisation();
        vereinOrg.setId(UUID.randomUUID());
        vereinOrg.setName("FC Beispiel");
        vereinOrg.setTyp(OrgTyp.VEREIN);

        when(appUserRepository.findByEmail("vorstand@verein.test")).thenReturn(Optional.of(user));
        when(vertragRepository.summePreisChfByOrg(any(), any())).thenReturn(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("STAT-VEREIN-01: User ohne VEREIN-Org → leeres DTO")
    void leerOhneVereinOrg() {
        Organisation sponsorOrg = new Organisation();
        sponsorOrg.setId(UUID.randomUUID());
        sponsorOrg.setTyp(OrgTyp.UNTERNEHMEN);
        when(mitgliedschaftRepository.findByUserIdAndRolleInMitOrg(eq(user.getId()), any()))
                .thenReturn(List.of(mitgliedschaft(user, sponsorOrg)));

        VereinStatistik stat = service.fuerUser("vorstand@verein.test");

        assertThat(stat.hatVereinOrgs()).isFalse();
        assertThat(stat).usingRecursiveComparison().isEqualTo(VereinStatistik.leer());
    }

    @Test
    @DisplayName("STAT-VEREIN-02: User mit Verein-Org → Org-Namen + Counter-Aufrufe")
    void mitVereinOrgLiefertKennzahlen() {
        when(mitgliedschaftRepository.findByUserIdAndRolleInMitOrg(eq(user.getId()), any()))
                .thenReturn(List.of(mitgliedschaft(user, vereinOrg)));
        when(projektService.zaehleOeffentlicheNachOrgIds(any())).thenReturn(3L);
        when(paketRepository.zaehleAktiveByOrgIds(any())).thenReturn(5L);
        when(vertragRepository.countByOrgIdInAndStatus(any(), eq(VertragsStatus.UNTERZEICHNET)))
                .thenReturn(4L);
        when(vertragRepository.summePreisChfByOrg(any(), eq(VertragsStatus.UNTERZEICHNET)))
                .thenReturn(new BigDecimal("12000.00"));

        VereinStatistik stat = service.fuerUser("vorstand@verein.test");

        assertThat(stat.hatVereinOrgs()).isTrue();
        assertThat(stat.vereinOrgNamen()).containsExactly("FC Beispiel");
        assertThat(stat.anzahlProjekteVeroeffentlicht()).isEqualTo(3);
        assertThat(stat.anzahlPakete()).isEqualTo(5);
        assertThat(stat.anzahlVertraegeUnterzeichnet()).isEqualTo(4);
        assertThat(stat.einnahmenChfUnterzeichnet()).isEqualByComparingTo("12000.00");
    }

    @Test
    @DisplayName("STAT-VEREIN-03: User mit 2 Verein-Orgs → distinct listet beide")
    void mehrereVereinOrgs() {
        Organisation zweiterVerein = new Organisation();
        zweiterVerein.setId(UUID.randomUUID());
        zweiterVerein.setName("HC Beispiel");
        zweiterVerein.setTyp(OrgTyp.VEREIN);

        when(mitgliedschaftRepository.findByUserIdAndRolleInMitOrg(eq(user.getId()), any()))
                .thenReturn(List.of(
                        mitgliedschaft(user, vereinOrg),
                        mitgliedschaft(user, zweiterVerein)));

        VereinStatistik stat = service.fuerUser("vorstand@verein.test");

        assertThat(stat.vereinOrgNamen()).containsExactlyInAnyOrder("FC Beispiel", "HC Beispiel");
    }

    @Test
    @DisplayName("STAT-VEREIN-04: summePreisChfByOrg liefert null → DTO zeigt BigDecimal.ZERO")
    void einnahmenNullWirdAufZeroCorrected() {
        when(mitgliedschaftRepository.findByUserIdAndRolleInMitOrg(eq(user.getId()), any()))
                .thenReturn(List.of(mitgliedschaft(user, vereinOrg)));
        when(vertragRepository.summePreisChfByOrg(any(), any())).thenReturn(null);

        VereinStatistik stat = service.fuerUser("vorstand@verein.test");

        assertThat(stat.einnahmenChfUnterzeichnet()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("STAT-VEREIN-05: Eingehende + ausgehende Anfragen werden getrennt aggregiert")
    void anfragenEingehendUndAusgehend() {
        when(mitgliedschaftRepository.findByUserIdAndRolleInMitOrg(eq(user.getId()), any()))
                .thenReturn(List.of(mitgliedschaft(user, vereinOrg)));
        when(anfrageRepository.countByEmpfaengerOrgIdInAndStatus(any(), eq(AnfrageStatus.NEU)))
                .thenReturn(3L);
        when(anfrageRepository.countByEmpfaengerOrgIdInAndStatus(any(), eq(AnfrageStatus.ANGENOMMEN)))
                .thenReturn(8L);
        when(anfrageRepository.countByAnfragenderOrgIdInAndStatus(any(), eq(AnfrageStatus.ANGENOMMEN)))
                .thenReturn(2L);

        VereinStatistik stat = service.fuerUser("vorstand@verein.test");

        assertThat(stat.anzahlAnfragenEingehendNeu()).isEqualTo(3);
        assertThat(stat.anzahlAnfragenEingehendAngenommen()).isEqualTo(8);
        assertThat(stat.anzahlAnfragenAusgehendAngenommen()).isEqualTo(2);
    }

    @Test
    @DisplayName("STAT-VEREIN-06: User unbekannt → NotFoundException")
    void unbekannterUser() {
        when(appUserRepository.findByEmail("egal@test.ch")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.fuerUser("egal@test.ch"))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    @DisplayName("STAT-VEREIN-07: Conversion-Rate eingehend — 8 angenommen, 2 abgelehnt → 80%")
    void conversionRateEingehend() {
        VereinStatistik stat = new VereinStatistik(
                0, 0,
                3, 8, 2,
                0, 0, 0,
                0, 0, 0, BigDecimal.ZERO,
                0, 0, 0,
                List.of("FC Beispiel"));

        assertThat(stat.conversionRateEingehendProzent()).isEqualTo(80);
    }

    @Test
    @DisplayName("STAT-VEREIN-07b: Conversion-Rate ohne beantwortete Anfragen → 0 (kein Div-by-Zero)")
    void conversionRateOhneAntworten() {
        VereinStatistik stat = VereinStatistik.leer();
        assertThat(stat.conversionRateEingehendProzent()).isEqualTo(0);
        assertThat(stat.conversionRateAusgehendProzent()).isEqualTo(0);
    }

    private static Mitgliedschaft mitgliedschaft(AppUser u, Organisation o) {
        Mitgliedschaft m = new Mitgliedschaft();
        m.setUser(u);
        m.setOrg(o);
        return m;
    }
}
