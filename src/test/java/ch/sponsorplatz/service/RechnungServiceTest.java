package ch.sponsorplatz.service;

import ch.sponsorplatz.organisation.Organisation;
import ch.sponsorplatz.model.Rechnung;
import ch.sponsorplatz.model.RechnungsStatus;
import ch.sponsorplatz.model.Vertrag;
import ch.sponsorplatz.model.VertragsStatus;
import ch.sponsorplatz.repository.RechnungRepository;
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
 * Tests für {@link RechnungService}.
 *
 * Test-IDs: RECH-01..06.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class RechnungServiceTest {

    @Mock private RechnungRepository repository;
    @Mock private VertragService vertragService;

    private RechnungService service;

    private final UUID vertragId = UUID.randomUUID();
    private Vertrag vertrag;

    @BeforeEach
    void setUp() {
        service = new RechnungService(repository, vertragService);

        Organisation verein = neueOrg("FC Beispiel", "fc-beispiel", "CH4431999123000889012");

        vertrag = new Vertrag();
        vertrag.setId(vertragId);
        vertrag.setStatus(VertragsStatus.ENTWURF);
        vertrag.setOrg(verein);
        vertrag.setOrgName(verein.getName());
        vertrag.setSponsorName("Acme AG");
        vertrag.setSponsorEmail("kontakt@acme.ch");
        vertrag.setPaketName("Gold");
        vertrag.setPreisChf(new BigDecimal("5000.00"));

        when(vertragService.findeNachId(vertragId)).thenReturn(vertrag);
        when(repository.save(any(Rechnung.class))).thenAnswer(inv -> inv.getArgument(0));
        when(repository.countByOrgId(any())).thenReturn(0L);
    }

    @Test
    @DisplayName("RECH-01: erstelle aus Vertrag kopiert IBAN, Betrag, Sponsor — Status OFFEN")
    void erstelleSnapshot() {
        when(repository.findByVertragId(vertragId)).thenReturn(Optional.empty());

        Rechnung r = service.erstelle(vertragId, "fabian@example.ch");

        assertThat(r.getStatus()).isEqualTo(RechnungsStatus.OFFEN);
        assertThat(r.getBetragChf()).isEqualByComparingTo(new BigDecimal("5000.00"));
        assertThat(r.getIban()).isEqualTo("CH4431999123000889012");
        assertThat(r.getSponsorName()).isEqualTo("Acme AG");
        assertThat(r.getSponsorEmail()).isEqualTo("kontakt@acme.ch");
        assertThat(r.getZahlungszweck()).contains("Gold");
        assertThat(r.getRechnungsnummer()).startsWith("SP-").contains("0001");
        assertThat(r.getFaelligAm()).isAfter(java.time.LocalDate.now());
    }

    @Test
    @DisplayName("RECH-02: erstelle bei vorhandener Rechnung wirft IllegalStateException")
    void erstelleNurEinmal() {
        when(repository.findByVertragId(vertragId)).thenReturn(Optional.of(new Rechnung()));

        assertThatThrownBy(() -> service.erstelle(vertragId, "u"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("bereits");
    }

    @Test
    @DisplayName("RECH-03: erstelle ohne IBAN auf der Org wirft IllegalStateException")
    void erstelleNurMitIban() {
        Organisation ohneIban = neueOrg("FC", "fc", null);
        vertrag.setOrg(ohneIban);
        when(repository.findByVertragId(vertragId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.erstelle(vertragId, "u"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("IBAN");
    }

    @Test
    @DisplayName("RECH-04: erstelle generiert QR-Referenz nur bei QR-IBAN (Institut-ID 30000-31999)")
    void qrReferenzNurBeiQrIban() {
        // Test-IBAN oben (CH4431999123000889012) ist eine QR-IBAN — Referenz erwartet
        when(repository.findByVertragId(vertragId)).thenReturn(Optional.empty());

        Rechnung r = service.erstelle(vertragId, "u");

        assertThat(r.getQrReferenz()).isNotBlank();
        assertThat(r.getQrReferenz()).hasSize(27);
    }

    @Test
    @DisplayName("RECH-05: markiereBezahlt setzt Status, Zeitstempel, User")
    void markiereBezahlt() {
        Rechnung r = new Rechnung();
        r.setId(UUID.randomUUID());
        r.setStatus(RechnungsStatus.OFFEN);
        when(repository.findById(r.getId())).thenReturn(Optional.of(r));

        Rechnung result = service.markiereBezahlt(r.getId(), "fabian@example.ch");

        assertThat(result.getStatus()).isEqualTo(RechnungsStatus.BEZAHLT);
        assertThat(result.getBezahltAm()).isNotNull();
        assertThat(result.getBezahltVon()).isEqualTo("fabian@example.ch");
    }

    @Test
    @DisplayName("RECH-06: stornieren bezahlter Rechnung wirft IllegalStateException")
    void stornierenNichtAusBezahlt() {
        Rechnung r = new Rechnung();
        r.setId(UUID.randomUUID());
        r.setStatus(RechnungsStatus.BEZAHLT);
        when(repository.findById(r.getId())).thenReturn(Optional.of(r));

        assertThatThrownBy(() -> service.stornieren(r.getId()))
                .isInstanceOf(IllegalStateException.class);
    }

    private static Organisation neueOrg(String name, String slug, String iban) {
        Organisation o = new Organisation();
        o.setId(UUID.randomUUID());
        o.setName(name);
        o.setSlug(slug);
        o.setIban(iban);
        o.setStrasse("Bahnhofstrasse 1");
        o.setPostleitzahl("8001");
        o.setOrt("Zürich");
        return o;
    }
}
