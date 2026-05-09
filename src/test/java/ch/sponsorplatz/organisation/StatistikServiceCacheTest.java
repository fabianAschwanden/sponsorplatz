package ch.sponsorplatz.organisation;

import ch.sponsorplatz.projekt.ProjektRepository;
import ch.sponsorplatz.projekt.Sichtbarkeit;
import ch.sponsorplatz.shared.config.CacheConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.cache.CacheManager;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * MARK-07: Verifiziert dass {@link StatistikService} mit {@code @Cacheable}
 * korrekt verdrahtet ist — der zweite Aufruf innerhalb der TTL trifft das
 * Repository nicht erneut. Wir testen hier explizit die Verdrahtung
 * (Annotation + CacheManager + EnableCaching), nicht das TTL-Ablauf-Verhalten —
 * das wäre ein langer Sleep oder Clock-Manipulation.
 */
@SpringBootTest
@ActiveProfiles("dev")
class StatistikServiceCacheTest {

    @Autowired
    private StatistikService statistikService;

    @Autowired
    private CacheManager cacheManager;

    @MockitoBean
    private OrganisationRepository organisationRepository;

    @MockitoBean
    private ProjektRepository projektRepository;

    @BeforeEach
    void cachesLeeren() {
        cacheManager.getCacheNames().forEach(name -> {
            var cache = cacheManager.getCache(name);
            if (cache != null) cache.clear();
        });
    }

    @Test
    @DisplayName("MARK-07a: zweiter Aufruf vereineProBranche trifft Repo nicht (Cache-Hit)")
    void vereineProBrancheCachedZweitenAufruf() {
        when(organisationRepository.zaehleVereineNachBranche(any())).thenReturn(List.<Object[]>of(
                new Object[]{Branche.SPORT, 3L}
        ));

        Map<Branche, Long> erst = statistikService.vereineProBranche();
        Map<Branche, Long> zweit = statistikService.vereineProBranche();

        assertThat(erst).isEqualTo(zweit);
        verify(organisationRepository, times(1)).zaehleVereineNachBranche(any());
    }

    @Test
    @DisplayName("MARK-07b: zweiter Aufruf anzahlAktiveProjekte trifft Repo nicht")
    void anzahlAktiveProjekteCachedZweitenAufruf() {
        when(projektRepository.countBySichtbarkeit(Sichtbarkeit.OEFFENTLICH)).thenReturn(7L);

        long erst = statistikService.anzahlAktiveProjekte();
        long zweit = statistikService.anzahlAktiveProjekte();

        assertThat(erst).isEqualTo(7L);
        assertThat(zweit).isEqualTo(7L);
        verify(projektRepository, times(1)).countBySichtbarkeit(Sichtbarkeit.OEFFENTLICH);
    }

    @Test
    @DisplayName("MARK-07c: Cache-Regions sind explizit deklariert (fail-fast bei Tippfehler)")
    void cacheRegionsExistieren() {
        assertThat(cacheManager.getCacheNames())
                .contains(CacheConfig.STATISTIK_VEREINE_PRO_BRANCHE,
                          CacheConfig.STATISTIK_ANZAHL_PROJEKTE);
    }
}
