package ch.sponsorplatz.anfrage;

import ch.sponsorplatz.benutzer.AppUser;
import ch.sponsorplatz.benutzer.AppUserRepository;
import ch.sponsorplatz.organisation.Branche;
import ch.sponsorplatz.organisation.Mitgliedschaft;
import ch.sponsorplatz.organisation.MitgliedschaftRepository;
import ch.sponsorplatz.organisation.OrgTyp;
import ch.sponsorplatz.organisation.Organisation;
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
 * Tests für {@link SponsorStatistikService}.
 * Test-IDs: STAT-01..05.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SponsorStatistikServiceTest {

    @Mock private AppUserRepository appUserRepository;
    @Mock private MitgliedschaftRepository mitgliedschaftRepository;
    @Mock private VertragRepository vertragRepository;
    @Mock private SponsoringAnfrageRepository anfrageRepository;
    @Mock private RechnungRepository rechnungRepository;

    private SponsorStatistikService service;

    private AppUser user;
    private Organisation sponsorOrg;

    @BeforeEach
    void setUp() {
        service = new SponsorStatistikService(appUserRepository, mitgliedschaftRepository,
                vertragRepository, anfrageRepository, rechnungRepository);

        user = new AppUser();
        user.setId(UUID.randomUUID());
        user.setEmail("sponsor@css.test");

        sponsorOrg = new Organisation();
        sponsorOrg.setId(UUID.randomUUID());
        sponsorOrg.setName("CSS Versicherung");
        sponsorOrg.setTyp(OrgTyp.UNTERNEHMEN);

        when(appUserRepository.findByEmail("sponsor@css.test")).thenReturn(Optional.of(user));
        when(vertragRepository.summePreisChf(any(), any())).thenReturn(BigDecimal.ZERO);
        when(vertragRepository.zaehleProBranche(any(), any())).thenReturn(List.of());
    }

    @Test
    @DisplayName("STAT-01: User ohne UNTERNEHMEN-Org → leeres DTO")
    void leerOhneSponsorOrg() {
        Organisation vereinOrg = new Organisation();
        vereinOrg.setId(UUID.randomUUID());
        vereinOrg.setTyp(OrgTyp.VEREIN);
        when(mitgliedschaftRepository.findByUserIdAndRolleInMitOrg(eq(user.getId()), any()))
                .thenReturn(List.of(mitgliedschaft(user, vereinOrg)));

        SponsorStatistik stat = service.fuerUser("sponsor@css.test");

        assertThat(stat.hatSponsorOrgs()).isFalse();
        assertThat(stat).usingRecursiveComparison().isEqualTo(SponsorStatistik.leer());
    }

    @Test
    @DisplayName("STAT-02: User mit Sponsor-Org → Org-Namen + Counter-Aufrufe")
    void mitSponsorOrgLiefertKennzahlen() {
        when(mitgliedschaftRepository.findByUserIdAndRolleInMitOrg(eq(user.getId()), any()))
                .thenReturn(List.of(mitgliedschaft(user, sponsorOrg)));
        when(vertragRepository.countBySponsorOrgIdInAndStatus(any(), eq(VertragsStatus.UNTERZEICHNET)))
                .thenReturn(5L);
        when(vertragRepository.summePreisChf(any(), eq(VertragsStatus.UNTERZEICHNET)))
                .thenReturn(new BigDecimal("25000.00"));

        SponsorStatistik stat = service.fuerUser("sponsor@css.test");

        assertThat(stat.hatSponsorOrgs()).isTrue();
        assertThat(stat.sponsorOrgNamen()).containsExactly("CSS Versicherung");
        assertThat(stat.anzahlVertraegeUnterzeichnet()).isEqualTo(5);
        assertThat(stat.volumenChfUnterzeichnet()).isEqualByComparingTo("25000.00");
    }

    @Test
    @DisplayName("STAT-03: User mit 2 Sponsor-Orgs → distinct + alphabetisch listet")
    void mehrereSponsorOrgs() {
        Organisation zweiteOrg = new Organisation();
        zweiteOrg.setId(UUID.randomUUID());
        zweiteOrg.setName("Acme AG");
        zweiteOrg.setTyp(OrgTyp.UNTERNEHMEN);

        when(mitgliedschaftRepository.findByUserIdAndRolleInMitOrg(eq(user.getId()), any()))
                .thenReturn(List.of(
                        mitgliedschaft(user, sponsorOrg),
                        mitgliedschaft(user, zweiteOrg)));

        SponsorStatistik stat = service.fuerUser("sponsor@css.test");

        assertThat(stat.sponsorOrgNamen()).containsExactlyInAnyOrder("CSS Versicherung", "Acme AG");
    }

    @Test
    @DisplayName("STAT-04: summePreisChf liefert null (kein Vertrag) → DTO zeigt BigDecimal.ZERO")
    void volumenNullWirdAufNullCorrected() {
        when(mitgliedschaftRepository.findByUserIdAndRolleInMitOrg(eq(user.getId()), any()))
                .thenReturn(List.of(mitgliedschaft(user, sponsorOrg)));
        when(vertragRepository.summePreisChf(any(), any())).thenReturn(null);

        SponsorStatistik stat = service.fuerUser("sponsor@css.test");

        assertThat(stat.volumenChfUnterzeichnet()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("STAT-05: Branchen-Verteilung mappt Object[]-Tupel zu Branche→Long-Map")
    void branchenVerteilung() {
        when(mitgliedschaftRepository.findByUserIdAndRolleInMitOrg(eq(user.getId()), any()))
                .thenReturn(List.of(mitgliedschaft(user, sponsorOrg)));
        when(vertragRepository.zaehleProBranche(any(), eq(VertragsStatus.UNTERZEICHNET)))
                .thenReturn(List.of(
                        new Object[]{Branche.SPORT, 5L},
                        new Object[]{Branche.PRAEVENTION, 2L}));

        SponsorStatistik stat = service.fuerUser("sponsor@css.test");

        assertThat(stat.vertraegeProBranche())
                .containsEntry(Branche.SPORT, 5L)
                .containsEntry(Branche.PRAEVENTION, 2L);
    }

    @Test
    @DisplayName("STAT-06: User unbekannt → NotFoundException")
    void unbekannterUser() {
        when(appUserRepository.findByEmail("egal@test.ch")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.fuerUser("egal@test.ch"))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    @DisplayName("STAT-07: Conversion-Rate — 8 angenommen, 2 abgelehnt → 80%")
    void conversionRate() {
        SponsorStatistik stat = new SponsorStatistik(
                0, 0, 0, BigDecimal.ZERO,
                3, 8, 2,
                0, 0, 0,
                java.util.Map.of(), List.of("CSS"));

        assertThat(stat.conversionRateProzent()).isEqualTo(80);
    }

    @Test
    @DisplayName("STAT-07b: Conversion-Rate ohne beantwortete Anfragen → 0 (kein Div-by-Zero)")
    void conversionRateOhneAntworten() {
        SponsorStatistik stat = SponsorStatistik.leer();
        assertThat(stat.conversionRateProzent()).isEqualTo(0);
    }

    private static Mitgliedschaft mitgliedschaft(AppUser u, Organisation o) {
        Mitgliedschaft m = new Mitgliedschaft();
        m.setUser(u);
        m.setOrg(o);
        return m;
    }
}
