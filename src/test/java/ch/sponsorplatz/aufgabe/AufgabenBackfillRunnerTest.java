package ch.sponsorplatz.aufgabe;

import ch.sponsorplatz.anfrage.AnfrageStatus;
import ch.sponsorplatz.anfrage.RechnungRepository;
import ch.sponsorplatz.anfrage.RechnungsStatus;
import ch.sponsorplatz.anfrage.SponsoringAnfrage;
import ch.sponsorplatz.anfrage.SponsoringAnfrageRepository;
import ch.sponsorplatz.anfrage.Vertrag;
import ch.sponsorplatz.anfrage.VertragRepository;
import ch.sponsorplatz.anfrage.VertragsStatus;
import ch.sponsorplatz.anfrage.Rechnung;
import ch.sponsorplatz.organisation.OrgStatus;
import ch.sponsorplatz.organisation.OrgTyp;
import ch.sponsorplatz.organisation.Organisation;
import ch.sponsorplatz.organisation.OrganisationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Tests für {@link AufgabenBackfillRunner}.
 * Test-IDs: AUFG-BACKFILL-01..02.
 */
@ExtendWith(MockitoExtension.class)
class AufgabenBackfillRunnerTest {

    @Mock private OrganisationRepository organisationRepository;
    @Mock private SponsoringAnfrageRepository anfrageRepository;
    @Mock private VertragRepository vertragRepository;
    @Mock private RechnungRepository rechnungRepository;
    @Mock private AufgabenEngine aufgabenEngine;

    private AufgabenBackfillRunner runner;

    @BeforeEach
    void setUp() {
        runner = new AufgabenBackfillRunner(organisationRepository, anfrageRepository,
                vertragRepository, rechnungRepository, aufgabenEngine);
    }

    @Test
    @DisplayName("AUFG-BACKFILL-01: Pro offenem Trigger-Status wird die Engine aufgerufen")
    void backfillTriggertEngineProTrigger() {
        Organisation pendingOrg = neueOrg(OrgStatus.PENDING);
        SponsoringAnfrage neueAnfrage = new SponsoringAnfrage();
        neueAnfrage.setId(UUID.randomUUID());
        neueAnfrage.setStatus(AnfrageStatus.NEU);
        Vertrag entwurf = new Vertrag();
        entwurf.setId(UUID.randomUUID());
        entwurf.setStatus(VertragsStatus.ENTWURF);
        Rechnung offen = new Rechnung();
        offen.setId(UUID.randomUUID());
        offen.setStatus(RechnungsStatus.OFFEN);

        when(organisationRepository.findByStatusOrderByCreatedAtAsc(OrgStatus.PENDING))
                .thenReturn(List.of(pendingOrg));
        when(anfrageRepository.findByStatus(AnfrageStatus.NEU))
                .thenReturn(List.of(neueAnfrage));
        when(vertragRepository.findByStatus(VertragsStatus.ENTWURF))
                .thenReturn(List.of(entwurf));
        when(rechnungRepository.findByStatus(RechnungsStatus.OFFEN))
                .thenReturn(List.of(offen));

        runner.backfillen();

        verify(aufgabenEngine).onOrgStatusWechsel(pendingOrg);
        verify(aufgabenEngine).onAnfrageStatusWechsel(neueAnfrage);
        verify(aufgabenEngine).onVertragStatusWechsel(entwurf);
        verify(aufgabenEngine).onRechnungStatusWechsel(offen);
    }

    @Test
    @DisplayName("AUFG-BACKFILL-02: Keine offenen Trigger-Datensätze → Engine wird nicht aufgerufen")
    void leererBackfill() {
        when(organisationRepository.findByStatusOrderByCreatedAtAsc(any())).thenReturn(List.of());
        when(anfrageRepository.findByStatus(any())).thenReturn(List.of());
        when(vertragRepository.findByStatus(any())).thenReturn(List.of());
        when(rechnungRepository.findByStatus(any())).thenReturn(List.of());

        runner.backfillen();

        verifyNoInteractions(aufgabenEngine);
    }

    private static Organisation neueOrg(OrgStatus status) {
        Organisation o = new Organisation();
        o.setId(UUID.randomUUID());
        o.setName("Test-Org");
        o.setTyp(OrgTyp.VEREIN);
        o.setStatus(status);
        return o;
    }
}
