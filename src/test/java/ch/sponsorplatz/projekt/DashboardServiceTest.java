package ch.sponsorplatz.projekt;
import ch.sponsorplatz.service.SponsoringAnfrageService;

import ch.sponsorplatz.benutzer.AppUser;
import ch.sponsorplatz.benutzer.AppUserRepository;
import ch.sponsorplatz.organisation.MitgliedschaftRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DashboardServiceTest {

    @Mock
    private AppUserRepository appUserRepository;

    @Mock
    private MitgliedschaftRepository mitgliedschaftRepository;

    @Mock
    private ProjektService projektService;

    @Mock
    private SponsoringAnfrageService anfrageService;

    @InjectMocks
    private DashboardService dashboardService;

    private AppUser testUser;
    private UUID orgId1;
    private UUID orgId2;

    @BeforeEach
    void setUp() {
        testUser = new AppUser();
        testUser.setId(UUID.randomUUID());
        testUser.setEmail("test@example.ch");

        orgId1 = UUID.randomUUID();
        orgId2 = UUID.randomUUID();
    }

    /** DASH-09: Unbekannte E-Mail → alle Zähler 0, keine weiteren Service-Calls. */
    @Test
    void ladeDashboardDatenFuerUnbekannteEmailGibtLeer() {
        when(appUserRepository.findByEmail("unbekannt@x.ch")).thenReturn(Optional.empty());

        DashboardDaten daten = dashboardService.ladeDashboardDaten("unbekannt@x.ch");

        assertThat(daten.anzahlOrganisationen()).isZero();
        assertThat(daten.anzahlProjekte()).isZero();
        assertThat(daten.anzahlAnfragen()).isZero();
        assertThat(daten.anzahlOffeneAnfragen()).isZero();
        verifyNoInteractions(mitgliedschaftRepository, projektService, anfrageService);
    }

    /** DASH-05: User ohne Mitgliedschaften → alle Zähler 0, keine count-Aufrufe. */
    @Test
    void ladeDashboardDatenOhneMitgliedschaftenGibtLeer() {
        when(appUserRepository.findByEmail(testUser.getEmail())).thenReturn(Optional.of(testUser));
        when(mitgliedschaftRepository.findOrgIdsByUserId(testUser.getId())).thenReturn(List.of());

        DashboardDaten daten = dashboardService.ladeDashboardDaten(testUser.getEmail());

        assertThat(daten.anzahlOrganisationen()).isZero();
        assertThat(daten.anzahlProjekte()).isZero();
        verifyNoInteractions(projektService, anfrageService);
    }

    /** DASH-06: Zählt Orgs korrekt (= Anzahl Mitgliedschaften). */
    @Test
    void ladeDashboardDatenZaehltOrgsKorrekt() {
        when(appUserRepository.findByEmail(testUser.getEmail())).thenReturn(Optional.of(testUser));
        when(mitgliedschaftRepository.findOrgIdsByUserId(testUser.getId()))
                .thenReturn(List.of(orgId1, orgId2));
        when(projektService.zaehleOeffentlicheNachOrgIds(List.of(orgId1, orgId2))).thenReturn(0L);
        when(anfrageService.zaehleEingehende(List.of(orgId1, orgId2))).thenReturn(0L);
        when(anfrageService.zaehleNeue(List.of(orgId1, orgId2))).thenReturn(0L);

        DashboardDaten daten = dashboardService.ladeDashboardDaten(testUser.getEmail());

        assertThat(daten.anzahlOrganisationen()).isEqualTo(2);
    }

    /** DASH-07: Öffentliche Projekte werden via Service-Aggregat geliefert. */
    @Test
    void ladeDashboardDatenZaehltOeffentlicheProjekte() {
        when(appUserRepository.findByEmail(testUser.getEmail())).thenReturn(Optional.of(testUser));
        when(mitgliedschaftRepository.findOrgIdsByUserId(testUser.getId())).thenReturn(List.of(orgId1));
        when(projektService.zaehleOeffentlicheNachOrgIds(List.of(orgId1))).thenReturn(7L);
        when(anfrageService.zaehleEingehende(List.of(orgId1))).thenReturn(0L);
        when(anfrageService.zaehleNeue(List.of(orgId1))).thenReturn(0L);

        DashboardDaten daten = dashboardService.ladeDashboardDaten(testUser.getEmail());

        assertThat(daten.anzahlProjekte()).isEqualTo(7);
    }

    /** DASH-08: Eingehende und offene Anfragen kommen aus den Aggregat-Service-Methoden. */
    @Test
    void ladeDashboardDatenZaehltAnfragenKorrekt() {
        when(appUserRepository.findByEmail(testUser.getEmail())).thenReturn(Optional.of(testUser));
        when(mitgliedschaftRepository.findOrgIdsByUserId(testUser.getId())).thenReturn(List.of(orgId1, orgId2));
        when(projektService.zaehleOeffentlicheNachOrgIds(List.of(orgId1, orgId2))).thenReturn(0L);
        when(anfrageService.zaehleEingehende(List.of(orgId1, orgId2))).thenReturn(12L);
        when(anfrageService.zaehleNeue(List.of(orgId1, orgId2))).thenReturn(4L);

        DashboardDaten daten = dashboardService.ladeDashboardDaten(testUser.getEmail());

        assertThat(daten.anzahlAnfragen()).isEqualTo(12);
        assertThat(daten.anzahlOffeneAnfragen()).isEqualTo(4);
    }

    /** DASH-10 (H3-Fix): Aggregat-Methoden werden genau EINMAL aufgerufen — kein N+1-Loop. */
    @Test
    void ladeDashboardDatenRuftAggregateGenauEinmalAuf() {
        when(appUserRepository.findByEmail(testUser.getEmail())).thenReturn(Optional.of(testUser));
        when(mitgliedschaftRepository.findOrgIdsByUserId(testUser.getId()))
                .thenReturn(List.of(orgId1, orgId2));
        when(projektService.zaehleOeffentlicheNachOrgIds(List.of(orgId1, orgId2))).thenReturn(5L);
        when(anfrageService.zaehleEingehende(List.of(orgId1, orgId2))).thenReturn(3L);
        when(anfrageService.zaehleNeue(List.of(orgId1, orgId2))).thenReturn(1L);

        dashboardService.ladeDashboardDaten(testUser.getEmail());

        // Egal wie viele Orgs → exakt 1 Aufruf pro Aggregat
        verify(projektService, times(1)).zaehleOeffentlicheNachOrgIds(List.of(orgId1, orgId2));
        verify(anfrageService, times(1)).zaehleEingehende(List.of(orgId1, orgId2));
        verify(anfrageService, times(1)).zaehleNeue(List.of(orgId1, orgId2));
    }

    /** DASH-11 (M5-Fix): aktuellerMonat und aktuelleKw kommen direkt aus dem DTO — Controller muss nichts mehr berechnen. */
    @Test
    void ladeDashboardDatenSetztMonatUndKwImDto() {
        when(appUserRepository.findByEmail(testUser.getEmail())).thenReturn(Optional.empty());

        DashboardDaten daten = dashboardService.ladeDashboardDaten(testUser.getEmail());

        assertThat(daten.aktuellerMonat()).isNotBlank().contains(String.valueOf(java.time.LocalDate.now().getYear()));
        assertThat(daten.aktuelleKw()).startsWith("KW ");
    }
}
