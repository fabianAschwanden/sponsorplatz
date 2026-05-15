package ch.sponsorplatz.anfrage;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import ch.sponsorplatz.aufgabe.AufgabenEngine;
import ch.sponsorplatz.organisation.OrgStatus;
import ch.sponsorplatz.organisation.OrgTyp;
import ch.sponsorplatz.organisation.Organisation;
import ch.sponsorplatz.organisation.OrganisationRepository;

/**
 * Tests für {@link AufgabenBackfillRunner}.
 * Test-IDs: AUFG-BACKFILL-01..02.
 */
@ExtendWith(MockitoExtension.class)
class AufgabenBackfillRunnerTest {

    @Mock
    private OrganisationRepository organisationRepository;
    @Mock
    private SponsoringAnfrageRepository anfrageRepository;
    @Mock
    private VertragRepository vertragRepository;
    @Mock
    private RechnungRepository rechnungRepository;
    @Mock
    private AufgabenEngine aufgabenEngine;

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

        verify(aufgabenEngine)
                .onOrgStatusWechsel(new ch.sponsorplatz.organisation.OrgStatusGewechseltEvent(pendingOrg));
        verify(aufgabenEngine).onStatusWechsel(eq(ch.sponsorplatz.aufgabe.TriggerEntityTyp.ANFRAGE),
                eq(neueAnfrage.getId()), eq(neueAnfrage.getStatus().name()), any());
        verify(aufgabenEngine).onStatusWechsel(eq(ch.sponsorplatz.aufgabe.TriggerEntityTyp.VERTRAG),
                eq(entwurf.getId()), eq(entwurf.getStatus().name()), any());
        verify(aufgabenEngine).onStatusWechsel(eq(ch.sponsorplatz.aufgabe.TriggerEntityTyp.RECHNUNG), eq(offen.getId()),
                eq(offen.getStatus().name()), any());
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
