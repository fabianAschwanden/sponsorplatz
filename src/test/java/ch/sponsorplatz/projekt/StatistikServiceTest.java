package ch.sponsorplatz.projekt;

import  ch.sponsorplatz.organisation.Branche;

import ch.sponsorplatz.organisation.OrganisationRepository;

import ch.sponsorplatz.projekt.ProjektRepository;
import ch.sponsorplatz.projekt.Sichtbarkeit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StatistikServiceTest {

    @Mock
    private OrganisationRepository orgRepository;

    @Mock
    private ProjektRepository projektRepository;

    @InjectMocks
    private StatistikService statistikService;

    /** MARK-02a: vereineProBranche mappt Aggregat-Zeilen korrekt auf Map<Branche, Long>. */
    @Test
    void vereineProBrancheZaehltKorrekt() {
        when(orgRepository.zaehleVereineNachBranche(any())).thenReturn(List.<Object[]>of(
                new Object[]{Branche.SPORT, 2L},
                new Object[]{Branche.REHA, 1L}
        ));

        Map<Branche, Long> ergebnis = statistikService.vereineProBranche();

        assertThat(ergebnis.get(Branche.SPORT)).isEqualTo(2);
        assertThat(ergebnis.get(Branche.REHA)).isEqualTo(1);
    }

    /** MARK-02b: Statistik liefert Gesamtzahl aktiver Projekte (COUNT-Query). */
    @Test
    void aktiveProjekteZaehltKorrekt() {
        when(projektRepository.countBySichtbarkeit(Sichtbarkeit.OEFFENTLICH)).thenReturn(0L);

        long ergebnis = statistikService.anzahlAktiveProjekte();

        assertThat(ergebnis).isZero();
    }

    /** MARK-02c: leere Aggregat-Antwort → leere Map (keine NPE). */
    @Test
    void leeresErgebnisWirdLeereMap() {
        when(orgRepository.zaehleVereineNachBranche(any())).thenReturn(List.of());

        Map<Branche, Long> ergebnis = statistikService.vereineProBranche();

        assertThat(ergebnis).isEmpty();
    }
}

