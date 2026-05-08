package ch.sponsorplatz.projekt;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SponsoringPaketServiceTest {

    private SponsoringPaketRepository repository;
    private SponsoringPaketService service;

    @BeforeEach
    void setUp() {
        repository = mock(SponsoringPaketRepository.class);
        service = new SponsoringPaketService(repository);
    }

    /** SP-01: Paket erstellen mit gültigem Namen und Preis. */
    @Test
    void erstelleMitGueltigemNamenUndPreis() {
        Projekt projekt = new Projekt();
        projekt.setId(UUID.randomUUID());
        when(repository.save(any(SponsoringPaket.class))).thenAnswer(inv -> inv.getArgument(0));

        SponsoringPaket paket = service.erstelle(projekt, "Gold", "Logo auf Trikot", new BigDecimal("5000.00"));

        assertThat(paket.getName()).isEqualTo("Gold");
        assertThat(paket.getBeschreibung()).isEqualTo("Logo auf Trikot");
        assertThat(paket.getPreisChf()).isEqualByComparingTo("5000.00");
        assertThat(paket.getProjekt()).isEqualTo(projekt);
        assertThat(paket.isAktiv()).isTrue();
    }

    /** SP-02: Erstellen mit leerem Namen wirft. */
    @Test
    void erstelleOhneNameWirft() {
        Projekt projekt = new Projekt();
        assertThatThrownBy(() -> service.erstelle(projekt, "", null, null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    /** SP-03: Paket deaktivieren. */
    @Test
    void deaktivieren() {
        SponsoringPaket paket = new SponsoringPaket();
        paket.setId(UUID.randomUUID());
        paket.setAktiv(true);
        when(repository.findById(paket.getId())).thenReturn(Optional.of(paket));
        when(repository.save(any(SponsoringPaket.class))).thenAnswer(inv -> inv.getArgument(0));

        SponsoringPaket result = service.deaktiviere(paket.getId());

        assertThat(result.isAktiv()).isFalse();
    }

    /** SP-04: Pakete nach Projekt finden. */
    @Test
    void findeNachProjekt() {
        UUID projektId = UUID.randomUUID();
        when(repository.findByProjektIdOrderBySortierungAsc(projektId)).thenReturn(List.of());

        List<SponsoringPaket> result = service.findeNachProjekt(projektId);

        assertThat(result).isEmpty();
        verify(repository).findByProjektIdOrderBySortierungAsc(projektId);
    }
}

