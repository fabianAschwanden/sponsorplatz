package ch.sponsorplatz.anfrage;

import ch.sponsorplatz.audit.AuditService;
import ch.sponsorplatz.aufgabe.AufgabenEngine;
import ch.sponsorplatz.shared.exception.NotFoundException;
import ch.sponsorplatz.organisation.OrgTyp;
import ch.sponsorplatz.organisation.Organisation;
import ch.sponsorplatz.projekt.SponsoringPaket;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests für {@link VertragService}.
 *
 * Test-IDs: VTR-01..06 in {@code specs/TESTSTRATEGIE.md}.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class VertragServiceTest {

    @Mock private VertragRepository repository;
    @Mock private SponsoringAnfrageRepository anfrageRepository;
    @Mock private AuditService auditService;
    @Mock private RechnungService rechnungService;
    @Mock private AufgabenEngine aufgabenEngine;

    private VertragService service;

    private final UUID anfrageId = UUID.randomUUID();
    private SponsoringAnfrage anfrage;

    @BeforeEach
    void setUp() {
        service = new VertragService(repository, anfrageRepository, auditService,
                rechnungService, aufgabenEngine);

        Organisation verein = neueOrg("FC Beispiel", OrgTyp.VEREIN);
        Organisation sponsor = neueOrg("Acme AG", OrgTyp.UNTERNEHMEN);
        SponsoringPaket paket = neuesPaket("Gold", "1 Banner", new BigDecimal("5000.00"));

        anfrage = new SponsoringAnfrage();
        anfrage.setId(anfrageId);
        anfrage.setStatus(AnfrageStatus.ANGENOMMEN);
        anfrage.setEmpfaengerOrg(verein);
        anfrage.setAnfragenderOrg(sponsor);
        anfrage.setKontaktName("Max Muster");
        anfrage.setKontaktEmail("max@acme.ch");
        anfrage.setPaket(paket);

        when(anfrageRepository.findById(anfrageId)).thenReturn(Optional.of(anfrage));
        when(repository.save(any(Vertrag.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    @DisplayName("VTR-01: erstelle aus angenommener Anfrage kopiert Snapshot-Felder")
    void erstelleKopiertSnapshot() {
        when(repository.findByAnfrageId(anfrageId)).thenReturn(Optional.empty());

        Vertrag v = service.erstelle(anfrageId, "fabian@example.ch");

        assertThat(v.getStatus()).isEqualTo(VertragsStatus.ENTWURF);
        assertThat(v.getOrgName()).isEqualTo("FC Beispiel");
        assertThat(v.getSponsorName()).isEqualTo("Max Muster");
        assertThat(v.getSponsorEmail()).isEqualTo("max@acme.ch");
        assertThat(v.getPaketName()).isEqualTo("Gold");
        assertThat(v.getPreisChf()).isEqualByComparingTo(new BigDecimal("5000.00"));
        assertThat(v.getErstelltVon()).isEqualTo("fabian@example.ch");
    }

    @Test
    @DisplayName("VTR-02: erstelle bei Status NEU wirft IllegalStateException")
    void erstelleNurAusAngenommen() {
        anfrage.setStatus(AnfrageStatus.NEU);

        assertThatThrownBy(() -> service.erstelle(anfrageId, "u"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("angenommen");
    }

    @Test
    @DisplayName("VTR-03: erstelle bei vorhandenem Vertrag wirft IllegalStateException")
    void erstelleNurEinmal() {
        when(repository.findByAnfrageId(anfrageId)).thenReturn(Optional.of(new Vertrag()));

        assertThatThrownBy(() -> service.erstelle(anfrageId, "u"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("bereits");
    }

    @Test
    @DisplayName("VTR-04: erstelle bei unbekannter Anfrage wirft NotFoundException")
    void erstelleAnfrageMussExistieren() {
        UUID fremd = UUID.randomUUID();
        when(anfrageRepository.findById(fremd)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.erstelle(fremd, "u"))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    @DisplayName("VTR-05: markiereUnterzeichnet setzt Status + Zeitstempel + User (mit Preis > 0)")
    void markiereUnterzeichnet() {
        Vertrag v = new Vertrag();
        v.setId(UUID.randomUUID());
        v.setStatus(VertragsStatus.ENTWURF);
        v.setPreisChf(new BigDecimal("5000.00")); // Geld-Sponsoring → Pflicht-Check passt
        when(repository.findById(v.getId())).thenReturn(Optional.of(v));

        Vertrag result = service.markiereUnterzeichnet(v.getId(), "fabian@example.ch");

        assertThat(result.getStatus()).isEqualTo(VertragsStatus.UNTERZEICHNET);
        assertThat(result.getUnterzeichnetAm()).isNotNull();
        assertThat(result.getUnterzeichnetVon()).isEqualTo("fabian@example.ch");
    }

    @Test
    @DisplayName("VTR-06: markiereUnterzeichnet auf bereits unterzeichnetem Vertrag wirft")
    void markiereUnterzeichnetNurAusEntwurf() {
        Vertrag v = new Vertrag();
        v.setId(UUID.randomUUID());
        v.setStatus(VertragsStatus.UNTERZEICHNET);
        when(repository.findById(v.getId())).thenReturn(Optional.of(v));

        assertThatThrownBy(() -> service.markiereUnterzeichnet(v.getId(), "u"))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("VTR-09: erstelle aus Kontakt-Anfrage mappt Verein-Org als v.org via OrgTyp")
    void erstelleAusKontaktAnfrageMappingNachOrgTyp() {
        // Bei Kontakt-Anfrage ist die Rollen-Richtung umgedreht:
        // anfragenderOrg = Verein, empfaengerOrg = Sponsor. Das Mapping muss
        // trotzdem v.org = Verein, v.sponsorOrg = Sponsor liefern.
        Organisation verein = neueOrg("FC Sportverein", OrgTyp.VEREIN);
        Organisation sponsor = neueOrg("CSS Versicherung", OrgTyp.UNTERNEHMEN);

        anfrage.setAnfragenderOrg(verein);       // Verein als Anfragender
        anfrage.setEmpfaengerOrg(sponsor);       // Sponsor als Empfänger
        anfrage.setPaket(null);                  // Kontakt-Anfrage hat kein Paket
        anfrage.setBetreff("Sommerfest-Sponsoring");

        when(repository.findByAnfrageId(anfrageId)).thenReturn(Optional.empty());

        Vertrag v = service.erstelle(anfrageId, "max@verein.ch");

        assertThat(v.getOrg()).as("v.org muss der Verein sein").isEqualTo(verein);
        assertThat(v.getOrgName()).isEqualTo("FC Sportverein");
        assertThat(v.getSponsorOrg()).as("v.sponsorOrg muss der Sponsor sein").isEqualTo(sponsor);
    }

    @Test
    @DisplayName("VTR-10: erstelle aus Kontakt-Anfrage ohne Wunsch-Betrag — preisChf=0")
    void erstelleAusKontaktAnfrageSnapshot() {
        anfrage.setAnfragenderOrg(neueOrg("FC Sportverein", OrgTyp.VEREIN));
        anfrage.setEmpfaengerOrg(neueOrg("CSS Versicherung", OrgTyp.UNTERNEHMEN));
        anfrage.setPaket(null);
        anfrage.setBetreff("Sommerfest-Sponsoring 2026");
        anfrage.setNachricht("Wir suchen einen Sponsor für unser Sommerfest.");
        anfrage.setWunschBetragChf(null); // kein Richtbetrag

        when(repository.findByAnfrageId(anfrageId)).thenReturn(Optional.empty());

        Vertrag v = service.erstelle(anfrageId, "max@verein.ch");

        assertThat(v.getPaketName()).isEqualTo("Sommerfest-Sponsoring 2026");
        assertThat(v.getPaketBeschreibung())
                .isEqualTo("Wir suchen einen Sponsor für unser Sommerfest.");
        assertThat(v.getPreisChf())
                .as("Ohne Wunsch-Betrag: 0 — Verein-Owner ergänzt im Edit-Form")
                .isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(v.getStatus()).isEqualTo(VertragsStatus.ENTWURF);
    }

    @Test
    @DisplayName("VTR-10b: erstelle aus Kontakt-Anfrage mit Wunsch-Betrag — preisChf = wunschBetrag")
    void erstelleAusKontaktAnfrageMitWunschBetrag() {
        anfrage.setAnfragenderOrg(neueOrg("FC Sportverein", OrgTyp.VEREIN));
        anfrage.setEmpfaengerOrg(neueOrg("CSS Versicherung", OrgTyp.UNTERNEHMEN));
        anfrage.setPaket(null);
        anfrage.setBetreff("Sommerfest-Sponsoring 2026");
        anfrage.setWunschBetragChf(new BigDecimal("5000.00"));

        when(repository.findByAnfrageId(anfrageId)).thenReturn(Optional.empty());

        Vertrag v = service.erstelle(anfrageId, "max@verein.ch");

        assertThat(v.getPreisChf())
                .as("Initial-Preis kommt aus anfrage.wunschBetragChf")
                .isEqualByComparingTo(new BigDecimal("5000.00"));
    }

    @Test
    @DisplayName("VTR-05b: markiereUnterzeichnet ohne Preis und ohne Leistung wirft")
    void unterzeichnenOhnePreisUndLeistungWirft() {
        Vertrag v = new Vertrag();
        v.setId(UUID.randomUUID());
        v.setStatus(VertragsStatus.ENTWURF);
        v.setPreisChf(BigDecimal.ZERO); // Naturalien-Kandidat
        v.setLeistungVerein(null);
        v.setLeistungSponsor("   "); // blank zählt als nicht gepflegt
        when(repository.findById(v.getId())).thenReturn(Optional.of(v));

        assertThatThrownBy(() -> service.markiereUnterzeichnet(v.getId(), "u"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Preis > 0 oder eine Leistungsbeschreibung");
    }

    @Test
    @DisplayName("VTR-05c: markiereUnterzeichnet bei Naturalien-Sponsoring (preisChf=0, Leistung gepflegt) erlaubt")
    void unterzeichnenMitNaturalienErlaubt() {
        Vertrag v = new Vertrag();
        v.setId(UUID.randomUUID());
        v.setStatus(VertragsStatus.ENTWURF);
        v.setPreisChf(BigDecimal.ZERO);
        v.setLeistungVerein("Trikot-Logo für Saison 2026");
        when(repository.findById(v.getId())).thenReturn(Optional.of(v));

        Vertrag result = service.markiereUnterzeichnet(v.getId(), "verein@test.ch");

        assertThat(result.getStatus()).isEqualTo(VertragsStatus.UNTERZEICHNET);
    }

    @Test
    @DisplayName("VTR-07: kuendige mit bezahlter Rechnung wirft IllegalStateException")
    void kuendigeMitBezahlterRechnungWirft() {
        Vertrag v = new Vertrag();
        v.setId(UUID.randomUUID());
        v.setStatus(VertragsStatus.UNTERZEICHNET);
        when(repository.findById(v.getId())).thenReturn(Optional.of(v));

        Rechnung bezahlt = new Rechnung();
        bezahlt.setId(UUID.randomUUID());
        bezahlt.setStatus(RechnungsStatus.BEZAHLT);
        when(rechnungService.findeNachVertrag(v.getId())).thenReturn(Optional.of(bezahlt));

        assertThatThrownBy(() -> service.kuendige(v.getId(), "Grund egal"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("bezahlter Rechnung");
    }

    @Test
    @DisplayName("VTR-08: kuendige mit offener Rechnung storniert diese mit Grund-Hinweis")
    void kuendigeMitOffenerRechnungStorniert() {
        Vertrag v = new Vertrag();
        v.setId(UUID.randomUUID());
        v.setStatus(VertragsStatus.UNTERZEICHNET);
        when(repository.findById(v.getId())).thenReturn(Optional.of(v));

        Rechnung offen = new Rechnung();
        offen.setId(UUID.randomUUID());
        offen.setStatus(RechnungsStatus.OFFEN);
        when(rechnungService.findeNachVertrag(v.getId())).thenReturn(Optional.of(offen));

        Vertrag result = service.kuendige(v.getId(), "Sponsor-Insolvenz");

        assertThat(result.getStatus()).isEqualTo(VertragsStatus.GEKUENDIGT);
        assertThat(result.getGekuendigtAm()).isNotNull();
        assertThat(result.getKuendigungsGrund()).isEqualTo("Sponsor-Insolvenz");
        verify(rechnungService).stornieren(eq(offen.getId()),
                org.mockito.ArgumentMatchers.contains("Sponsor-Insolvenz"));
    }

    @Test
    @DisplayName("VTR-08b: kuendige ohne Rechnung läuft sauber durch (einfacher Pfad)")
    void kuendigeOhneRechnung() {
        Vertrag v = new Vertrag();
        v.setId(UUID.randomUUID());
        v.setStatus(VertragsStatus.UNTERZEICHNET);
        when(repository.findById(v.getId())).thenReturn(Optional.of(v));
        when(rechnungService.findeNachVertrag(v.getId())).thenReturn(Optional.empty());

        Vertrag result = service.kuendige(v.getId(), null);

        assertThat(result.getStatus()).isEqualTo(VertragsStatus.GEKUENDIGT);
        verify(rechnungService, org.mockito.Mockito.never())
                .stornieren(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
    }

    @Test
    @DisplayName("VTR-08c: kuendige bei Status ENTWURF wirft (nur aus UNTERZEICHNET erlaubt)")
    void kuendigeAusEntwurfWirft() {
        Vertrag v = new Vertrag();
        v.setId(UUID.randomUUID());
        v.setStatus(VertragsStatus.ENTWURF);
        when(repository.findById(v.getId())).thenReturn(Optional.of(v));

        assertThatThrownBy(() -> service.kuendige(v.getId(), "egal"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("unterzeichnete");
    }

    private static Organisation neueOrg(String name, OrgTyp typ) {
        Organisation o = new Organisation();
        o.setId(UUID.randomUUID());
        o.setName(name);
        o.setSlug(name.toLowerCase().replace(" ", "-"));
        o.setTyp(typ);
        return o;
    }

    private static SponsoringPaket neuesPaket(String name, String beschreibung, BigDecimal preis) {
        SponsoringPaket p = new SponsoringPaket();
        p.setId(UUID.randomUUID());
        p.setName(name);
        p.setBeschreibung(beschreibung);
        p.setPreisChf(preis);
        return p;
    }
}
