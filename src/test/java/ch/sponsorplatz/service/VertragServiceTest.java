package ch.sponsorplatz.service;

import ch.sponsorplatz.exception.NotFoundException;
import ch.sponsorplatz.model.AnfrageStatus;
import ch.sponsorplatz.model.Organisation;
import ch.sponsorplatz.model.SponsoringAnfrage;
import ch.sponsorplatz.model.SponsoringPaket;
import ch.sponsorplatz.model.Vertrag;
import ch.sponsorplatz.model.VertragsStatus;
import ch.sponsorplatz.repository.SponsoringAnfrageRepository;
import ch.sponsorplatz.repository.VertragRepository;
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

    private VertragService service;

    private final UUID anfrageId = UUID.randomUUID();
    private SponsoringAnfrage anfrage;

    @BeforeEach
    void setUp() {
        service = new VertragService(repository, anfrageRepository);

        Organisation verein = neueOrg("FC Beispiel");
        Organisation sponsor = neueOrg("Acme AG");
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
    @DisplayName("VTR-05: markiereUnterzeichnet setzt Status + Zeitstempel + User")
    void markiereUnterzeichnet() {
        Vertrag v = new Vertrag();
        v.setId(UUID.randomUUID());
        v.setStatus(VertragsStatus.ENTWURF);
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

    private static Organisation neueOrg(String name) {
        Organisation o = new Organisation();
        o.setId(UUID.randomUUID());
        o.setName(name);
        o.setSlug(name.toLowerCase().replace(" ", "-"));
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
