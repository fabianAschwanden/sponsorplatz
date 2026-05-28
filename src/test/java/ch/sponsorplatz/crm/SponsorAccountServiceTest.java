package ch.sponsorplatz.crm;

import ch.sponsorplatz.organisation.AccessControl;
import ch.sponsorplatz.organisation.Organisation;
import ch.sponsorplatz.organisation.OrganisationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Test-IDs CRM-SVC-01..05 — Zugriffs-Schranke + Portfolio-Logik des
 * SponsorAccountService. Die ECHTE Isolation gegen die DB beweist
 * {@code SponsorAccountIsolationIT}; hier wird die Service-Logik isoliert
 * mit gemocktem AccessControl geprüft.
 */
@ExtendWith(MockitoExtension.class)
class SponsorAccountServiceTest {

    @Mock private SponsorAccountRepository repository;
    @Mock private OrganisationRepository organisationRepository;
    @Mock private AccessControl accessControl;
    @Mock private Authentication auth;

    private SponsorAccountService service;

    private final UUID sponsorOrgId = UUID.randomUUID();
    private final UUID vereinOrgId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        service = new SponsorAccountService(repository, organisationRepository, accessControl);
    }

    /** CRM-SVC-01: findePortfolio ohne Sponsor-Zugriff → AccessDeniedException, kein Query. */
    @Test
    @DisplayName("CRM-SVC-01: findePortfolio ohne Zugriff wirft AccessDenied")
    void portfolioOhneZugriffWirft() {
        when(accessControl.kannSponsorDatenSehen(sponsorOrgId, auth)).thenReturn(false);

        assertThatThrownBy(() -> service.findePortfolio(sponsorOrgId, auth))
                .isInstanceOf(AccessDeniedException.class);
        verify(repository, never()).findByBesitzerSponsorOrgIdOrderByErstelltAmDesc(any());
    }

    /** CRM-SVC-02: findePortfolio mit Zugriff → nur Accounts des Sponsors. */
    @Test
    @DisplayName("CRM-SVC-02: findePortfolio liefert Portfolio des Sponsors")
    void portfolioMitZugriff() {
        when(accessControl.kannSponsorDatenSehen(sponsorOrgId, auth)).thenReturn(true);
        when(repository.findByBesitzerSponsorOrgIdOrderByErstelltAmDesc(sponsorOrgId))
                .thenReturn(List.of(account("FC A", "fc-a"), account("FC B", "fc-b")));

        List<SponsorAccountView> portfolio = service.findePortfolio(sponsorOrgId, auth);

        assertThat(portfolio).hasSize(2);
        assertThat(portfolio).extracting(SponsorAccountView::vereinName)
                .containsExactly("FC A", "FC B");
    }

    /** CRM-SVC-03: erstelle ohne Zugriff → AccessDeniedException, kein save. */
    @Test
    @DisplayName("CRM-SVC-03: erstelle ohne Zugriff wirft AccessDenied")
    void erstelleOhneZugriffWirft() {
        when(accessControl.kannSponsorDatenSehen(sponsorOrgId, auth)).thenReturn(false);

        assertThatThrownBy(() -> service.erstelle(sponsorOrgId, vereinOrgId, auth))
                .isInstanceOf(AccessDeniedException.class);
        verify(repository, never()).save(any());
    }

    /** CRM-SVC-04: erstelle mit Zugriff → speichert mit besitzerSponsorOrgId + Status LEAD. */
    @Test
    @DisplayName("CRM-SVC-04: erstelle speichert mit Mandanten-Key + Status LEAD")
    void erstelleSpeichert() {
        Organisation verein = new Organisation();
        verein.setId(vereinOrgId);
        verein.setName("FC Neu");
        verein.setSlug("fc-neu");
        when(accessControl.kannSponsorDatenSehen(sponsorOrgId, auth)).thenReturn(true);
        when(repository.existsByBesitzerSponsorOrgIdAndVereinId(sponsorOrgId, vereinOrgId)).thenReturn(false);
        when(organisationRepository.findById(vereinOrgId)).thenReturn(Optional.of(verein));
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        SponsorAccountView view = service.erstelle(sponsorOrgId, vereinOrgId, auth);

        ArgumentCaptor<SponsorAccount> cap = ArgumentCaptor.forClass(SponsorAccount.class);
        verify(repository).save(cap.capture());
        assertThat(cap.getValue().getBesitzerSponsorOrgId()).isEqualTo(sponsorOrgId);
        assertThat(cap.getValue().getVerein()).isEqualTo(verein);
        assertThat(cap.getValue().getStatus()).isEqualTo(AccountStatus.LEAD);
        assertThat(view.vereinName()).isEqualTo("FC Neu");
    }

    /** CRM-SVC-05: erstelle bei bestehendem Account → IllegalArgumentException (Dublette). */
    @Test
    @DisplayName("CRM-SVC-05: erstelle bei Dublette wirft IllegalArgument")
    void erstelleDubletteWirft() {
        when(accessControl.kannSponsorDatenSehen(sponsorOrgId, auth)).thenReturn(true);
        when(repository.existsByBesitzerSponsorOrgIdAndVereinId(sponsorOrgId, vereinOrgId)).thenReturn(true);

        assertThatThrownBy(() -> service.erstelle(sponsorOrgId, vereinOrgId, auth))
                .isInstanceOf(IllegalArgumentException.class);
        verify(repository, never()).save(any());
    }

    /** CRM-SVC-06: aktualisiere ohne Zugriff (fremder Account) → AccessDenied, kein save. */
    @Test
    @DisplayName("CRM-SVC-06: aktualisiere ohne Zugriff wirft AccessDenied")
    void aktualisiereOhneZugriffWirft() {
        UUID accountId = UUID.randomUUID();
        SponsorAccount bestehend = account("FC X", "fc-x");
        bestehend.setId(accountId);
        when(repository.findById(accountId)).thenReturn(Optional.of(bestehend));
        when(accessControl.kannSponsorDatenSehen(sponsorOrgId, auth)).thenReturn(false);

        assertThatThrownBy(() -> service.aktualisiere(accountId, AccountStatus.AKTIV, AccountTier.CORE,
                null, null, "x", auth))
                .isInstanceOf(AccessDeniedException.class);
        verify(repository, never()).save(any());
    }

    /** CRM-SVC-07: aktualisiere mit Zugriff → Status/Tier/Pipeline/Forecast/Notiz gesetzt. */
    @Test
    @DisplayName("CRM-SVC-07: aktualisiere setzt Status/Tier/Pipeline/Forecast/Notiz")
    void aktualisiereSetztFelder() {
        UUID accountId = UUID.randomUUID();
        SponsorAccount bestehend = account("FC Y", "fc-y");
        bestehend.setId(accountId);
        when(repository.findById(accountId)).thenReturn(Optional.of(bestehend));
        when(accessControl.kannSponsorDatenSehen(sponsorOrgId, auth)).thenReturn(true);
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        SponsorAccountView view = service.aktualisiere(accountId, AccountStatus.IN_RENEWAL,
                AccountTier.STRATEGIC, PipelineStage.ANGEBOT, new java.math.BigDecimal("10000.00"),
                "Notiz", auth);

        assertThat(view.status()).isEqualTo(AccountStatus.IN_RENEWAL);
        assertThat(view.tier()).isEqualTo(AccountTier.STRATEGIC);
        assertThat(view.pipelineStage()).isEqualTo(PipelineStage.ANGEBOT);
        assertThat(view.gewichteterForecastChf()).isEqualByComparingTo("6000.00");
        assertThat(view.notiz()).isEqualTo("Notiz");
    }

    private SponsorAccount account(String vereinName, String slug) {
        Organisation verein = new Organisation();
        verein.setId(UUID.randomUUID());
        verein.setName(vereinName);
        verein.setSlug(slug);
        SponsorAccount a = new SponsorAccount();
        a.setId(UUID.randomUUID());
        a.setBesitzerSponsorOrgId(sponsorOrgId);
        a.setVerein(verein);
        a.setStatus(AccountStatus.AKTIV);
        return a;
    }
}
