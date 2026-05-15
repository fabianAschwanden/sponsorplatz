package ch.sponsorplatz.aufgabe;

import ch.sponsorplatz.anfrage.AnfrageStatus;
import ch.sponsorplatz.anfrage.SponsoringAnfrage;
import ch.sponsorplatz.anfrage.Vertrag;
import ch.sponsorplatz.anfrage.VertragsStatus;
import ch.sponsorplatz.organisation.OrgStatus;
import ch.sponsorplatz.organisation.OrgStatusGewechseltEvent;
import ch.sponsorplatz.organisation.OrgTyp;
import ch.sponsorplatz.organisation.Organisation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests für {@link AufgabenEngine} — Erzeugung, Auto-Erledigung, Idempotenz,
 * Assignee-Resolution. Test-IDs: AUFG-ENG-01..07.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AufgabenEngineTest {

    @Mock private AufgabenDefinitionRepository definitionRepository;
    @Mock private AufgabeRepository aufgabeRepository;

    private AufgabenEngine engine;

    @BeforeEach
    void setUp() {
        engine = new AufgabenEngine(definitionRepository, aufgabeRepository);
        when(aufgabeRepository.save(any(Aufgabe.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    @DisplayName("AUFG-ENG-01: Neue PENDING-Org + aktive PLATFORM_ADMIN-Definition → eine Aufgabe für Admins")
    void erzeugePlatformAdminAufgabe() {
        Organisation org = neueOrg(OrgTyp.VEREIN, OrgStatus.PENDING);
        AufgabenDefinition def = neueDefinition(TriggerEntityTyp.ORG, "PENDING", "VERIFIED",
                AssigneeRegel.PLATFORM_ADMIN);
        when(aufgabeRepository.findByEntityTypAndEntityIdAndStatus(eq(TriggerEntityTyp.ORG),
                eq(org.getId()), eq(AufgabenStatus.OFFEN)))
                .thenReturn(List.of());
        when(definitionRepository.findByAktivTrueAndTriggerEntityTypAndTriggerStatus(
                eq(TriggerEntityTyp.ORG), eq("PENDING")))
                .thenReturn(List.of(def));

        engine.onOrgStatusWechsel(new OrgStatusGewechseltEvent(org));

        ArgumentCaptor<Aufgabe> cap = ArgumentCaptor.forClass(Aufgabe.class);
        verify(aufgabeRepository).save(cap.capture());
        assertThat(cap.getValue().isNurPlatformAdmin()).isTrue();
        assertThat(cap.getValue().getAssigneeOrg()).isNull();
        assertThat(cap.getValue().getEntityTyp()).isEqualTo(TriggerEntityTyp.ORG);
        assertThat(cap.getValue().getEntityId()).isEqualTo(org.getId());
    }

    @Test
    @DisplayName("AUFG-ENG-02: Org wechselt PENDING → VERIFIED → offene Aufgabe wird ERLEDIGT")
    void erledigeBeiZielStatus() {
        Organisation org = neueOrg(OrgTyp.VEREIN, OrgStatus.VERIFIED);
        AufgabenDefinition def = neueDefinition(TriggerEntityTyp.ORG, "PENDING", "VERIFIED",
                AssigneeRegel.PLATFORM_ADMIN);
        Aufgabe offene = neueAufgabe(def, org.getId(), TriggerEntityTyp.ORG);
        when(aufgabeRepository.findByEntityTypAndEntityIdAndStatus(eq(TriggerEntityTyp.ORG),
                eq(org.getId()), eq(AufgabenStatus.OFFEN)))
                .thenReturn(List.of(offene));
        when(definitionRepository.findByAktivTrueAndTriggerEntityTypAndTriggerStatus(
                eq(TriggerEntityTyp.ORG), eq("VERIFIED")))
                .thenReturn(List.of());

        engine.onOrgStatusWechsel(new OrgStatusGewechseltEvent(org));

        assertThat(offene.getStatus()).isEqualTo(AufgabenStatus.ERLEDIGT);
        assertThat(offene.getErledigtAm()).isNotNull();
    }

    @Test
    @DisplayName("AUFG-ENG-03: Anfrage wechselt NEU → ABGELEHNT → Aufgabe (Ziel=ANGENOMMEN) wird ENTFALLEN")
    void entfalleBeiAlternativStatus() {
        SponsoringAnfrage anfrage = neueAnfrage(AnfrageStatus.ABGELEHNT);
        AufgabenDefinition def = neueDefinition(TriggerEntityTyp.ANFRAGE, "NEU", "ANGENOMMEN",
                AssigneeRegel.ANFRAGE_EMPFAENGER_ORG);
        Aufgabe offene = neueAufgabe(def, anfrage.getId(), TriggerEntityTyp.ANFRAGE);
        when(aufgabeRepository.findByEntityTypAndEntityIdAndStatus(eq(TriggerEntityTyp.ANFRAGE),
                eq(anfrage.getId()), eq(AufgabenStatus.OFFEN)))
                .thenReturn(List.of(offene));
        when(definitionRepository.findByAktivTrueAndTriggerEntityTypAndTriggerStatus(
                eq(TriggerEntityTyp.ANFRAGE), eq("ABGELEHNT")))
                .thenReturn(List.of());

        engine.onStatusWechsel(TriggerEntityTyp.ANFRAGE, anfrage.getId(), anfrage.getStatus().name(), AssigneeKontext.ausAnfrageOrgs(anfrage.getEmpfaengerOrg(), anfrage.getAnfragenderOrg()));

        assertThat(offene.getStatus()).isEqualTo(AufgabenStatus.ENTFALLEN);
        assertThat(offene.getErledigtAm()).isNotNull();
    }

    @Test
    @DisplayName("AUFG-ENG-04: Vertrag ENTWURF → erzeugt zwei Aufgaben (Verein + Sponsor) mit korrekten Orgs")
    void vertragErzeugtZweiAufgaben() {
        Organisation verein = neueOrg(OrgTyp.VEREIN, OrgStatus.VERIFIED);
        Organisation sponsor = neueOrg(OrgTyp.UNTERNEHMEN, OrgStatus.VERIFIED);
        Vertrag vertrag = neuerVertrag(VertragsStatus.ENTWURF, verein, sponsor);
        AufgabenDefinition defVerein = neueDefinition(TriggerEntityTyp.VERTRAG, "ENTWURF",
                "UNTERZEICHNET", AssigneeRegel.VERTRAG_VEREIN_ORG);
        AufgabenDefinition defSponsor = neueDefinition(TriggerEntityTyp.VERTRAG, "ENTWURF",
                "UNTERZEICHNET", AssigneeRegel.VERTRAG_SPONSOR_ORG);
        when(aufgabeRepository.findByEntityTypAndEntityIdAndStatus(eq(TriggerEntityTyp.VERTRAG),
                eq(vertrag.getId()), eq(AufgabenStatus.OFFEN)))
                .thenReturn(List.of());
        when(definitionRepository.findByAktivTrueAndTriggerEntityTypAndTriggerStatus(
                eq(TriggerEntityTyp.VERTRAG), eq("ENTWURF")))
                .thenReturn(List.of(defVerein, defSponsor));

        engine.onStatusWechsel(TriggerEntityTyp.VERTRAG, vertrag.getId(), vertrag.getStatus().name(), AssigneeKontext.ausVertragOrgs(vertrag.getOrg(), vertrag.getSponsorOrg()));

        ArgumentCaptor<Aufgabe> cap = ArgumentCaptor.forClass(Aufgabe.class);
        verify(aufgabeRepository, org.mockito.Mockito.times(2)).save(cap.capture());
        List<Aufgabe> erzeugte = cap.getAllValues();
        assertThat(erzeugte).extracting(Aufgabe::getAssigneeOrg)
                .containsExactlyInAnyOrder(verein, sponsor);
    }

    @Test
    @DisplayName("AUFG-ENG-05: Doppelter Trigger-Aufruf → nur eine Aufgabe (Idempotenz via exists-Check)")
    void idempotenz() {
        Organisation org = neueOrg(OrgTyp.VEREIN, OrgStatus.PENDING);
        AufgabenDefinition def = neueDefinition(TriggerEntityTyp.ORG, "PENDING", "VERIFIED",
                AssigneeRegel.PLATFORM_ADMIN);
        when(aufgabeRepository.findByEntityTypAndEntityIdAndStatus(any(), any(), any()))
                .thenReturn(List.of());
        when(definitionRepository.findByAktivTrueAndTriggerEntityTypAndTriggerStatus(any(), any()))
                .thenReturn(List.of(def));
        when(aufgabeRepository.existsByDefinitionIdAndEntityIdAndStatus(
                eq(def.getId()), eq(org.getId()), eq(AufgabenStatus.OFFEN)))
                .thenReturn(true);

        engine.onOrgStatusWechsel(new OrgStatusGewechseltEvent(org));

        verify(aufgabeRepository, never()).save(any(Aufgabe.class));
    }

    @Test
    @DisplayName("AUFG-ENG-06: Keine aktive Definition für den Status → kein Save, keine Exception")
    void keineDefinitionKeinErgebnis() {
        Organisation org = neueOrg(OrgTyp.VEREIN, OrgStatus.PENDING);
        when(aufgabeRepository.findByEntityTypAndEntityIdAndStatus(any(), any(), any()))
                .thenReturn(List.of());
        when(definitionRepository.findByAktivTrueAndTriggerEntityTypAndTriggerStatus(any(), any()))
                .thenReturn(List.of());

        engine.onOrgStatusWechsel(new OrgStatusGewechseltEvent(org));

        verify(aufgabeRepository, never()).save(any(Aufgabe.class));
    }

    @Test
    @DisplayName("AUFG-ENG-07: Assignee-Regel ORG_MITGLIEDER ohne Org-Kontext → Aufgabe wird übersprungen (kein Crash)")
    void assigneeOhneOrgWirdUebersprungen() {
        SponsoringAnfrage anfrage = neueAnfrage(AnfrageStatus.NEU);
        anfrage.setEmpfaengerOrg(null);
        AufgabenDefinition def = neueDefinition(TriggerEntityTyp.ANFRAGE, "NEU", "ANGENOMMEN",
                AssigneeRegel.ANFRAGE_EMPFAENGER_ORG);
        when(aufgabeRepository.findByEntityTypAndEntityIdAndStatus(any(), any(), any()))
                .thenReturn(List.of());
        when(definitionRepository.findByAktivTrueAndTriggerEntityTypAndTriggerStatus(any(), any()))
                .thenReturn(List.of(def));

        engine.onStatusWechsel(TriggerEntityTyp.ANFRAGE, anfrage.getId(), anfrage.getStatus().name(), AssigneeKontext.ausAnfrageOrgs(anfrage.getEmpfaengerOrg(), anfrage.getAnfragenderOrg()));

        verify(aufgabeRepository, never()).save(any(Aufgabe.class));
    }

    // ── Helpers ────────────────────────────────────────────────────────────

    private static Organisation neueOrg(OrgTyp typ, OrgStatus status) {
        Organisation o = new Organisation();
        o.setId(UUID.randomUUID());
        o.setName("Org-" + typ);
        o.setTyp(typ);
        o.setStatus(status);
        return o;
    }

    private static AufgabenDefinition neueDefinition(TriggerEntityTyp typ, String trigger,
                                                       String ziel, AssigneeRegel regel) {
        AufgabenDefinition d = new AufgabenDefinition();
        d.setId(UUID.randomUUID());
        d.setTitel("Test-Aufgabe " + typ);
        d.setTriggerEntityTyp(typ);
        d.setTriggerStatus(trigger);
        d.setZielStatus(ziel);
        d.setAssigneeRegel(regel);
        d.setAktiv(true);
        return d;
    }

    private static Aufgabe neueAufgabe(AufgabenDefinition def, UUID entityId, TriggerEntityTyp typ) {
        Aufgabe a = new Aufgabe();
        a.setId(UUID.randomUUID());
        a.setDefinition(def);
        a.setEntityTyp(typ);
        a.setEntityId(entityId);
        a.setTitel(def.getTitel());
        a.setStatus(AufgabenStatus.OFFEN);
        return a;
    }

    private static SponsoringAnfrage neueAnfrage(AnfrageStatus status) {
        SponsoringAnfrage a = new SponsoringAnfrage();
        a.setId(UUID.randomUUID());
        a.setStatus(status);
        Organisation empfaenger = new Organisation();
        empfaenger.setId(UUID.randomUUID());
        empfaenger.setName("Empfänger");
        a.setEmpfaengerOrg(empfaenger);
        Organisation anfragender = new Organisation();
        anfragender.setId(UUID.randomUUID());
        anfragender.setName("Anfragender");
        a.setAnfragenderOrg(anfragender);
        return a;
    }

    private static Vertrag neuerVertrag(VertragsStatus status, Organisation verein, Organisation sponsor) {
        Vertrag v = new Vertrag();
        v.setId(UUID.randomUUID());
        v.setStatus(status);
        v.setOrg(verein);
        v.setSponsorOrg(sponsor);
        return v;
    }
}
