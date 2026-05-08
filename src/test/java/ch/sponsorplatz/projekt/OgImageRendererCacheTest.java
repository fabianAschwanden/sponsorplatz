package ch.sponsorplatz.projekt;

import ch.sponsorplatz.organisation.Branche;
import ch.sponsorplatz.organisation.Organisation;
import ch.sponsorplatz.organisation.OrganisationRepository;
import ch.sponsorplatz.shared.config.CacheConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.cache.CacheManager;
import org.springframework.test.context.ActiveProfiles;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * OG-04 / OG-05: Verifiziert dass {@link OgImageRenderer} mit {@code @Cacheable}
 * korrekt verdrahtet ist — der zweite Aufruf für denselben Slug rendert nicht
 * neu (kein Repo-Call). Die Renderer-Methoden produzieren ~50 KB pro PNG;
 * ohne Cache wäre ein viraler LinkedIn/Twitter-Share teuer.
 */
@SpringBootTest
@ActiveProfiles("dev")
class OgImageRendererCacheTest {

    @Autowired
    private OgImageRenderer renderer;

    @Autowired
    private CacheManager cacheManager;

    @MockBean
    private OrganisationRepository organisationRepository;

    @MockBean
    private ProjektRepository projektRepository;

    @BeforeEach
    void cachesLeeren() {
        cacheManager.getCacheNames().forEach(name -> {
            var cache = cacheManager.getCache(name);
            if (cache != null) cache.clear();
        });
    }

    @Test
    @DisplayName("OG-04: zweiter Aufruf rendereVerein trifft Repo nicht (Cache-Hit pro Slug)")
    void vereinCacheHit() {
        Organisation org = new Organisation();
        org.setId(UUID.randomUUID());
        org.setName("FC Test");
        org.setSlug("fc-test");
        org.setBranche(Branche.SPORT);
        when(organisationRepository.findBySlug("fc-test")).thenReturn(Optional.of(org));

        byte[] erst = renderer.rendereVerein("fc-test");
        byte[] zweit = renderer.rendereVerein("fc-test");

        assertThat(erst).isEqualTo(zweit);
        verify(organisationRepository, times(1)).findBySlug("fc-test");
    }

    @Test
    @DisplayName("OG-05: Verein-Cache-Key kollidiert nicht mit Projekt-Cache-Key (Slug-Prefix)")
    void vereinUndProjektMitGleichemSlugKollidierenNicht() {
        Organisation org = new Organisation();
        org.setName("Doppelter Slug — Verein");
        org.setSlug("doppelter-slug");
        org.setBranche(Branche.REHA);
        when(organisationRepository.findBySlug("doppelter-slug")).thenReturn(Optional.of(org));

        Projekt projekt = new Projekt();
        projekt.setName("Doppelter Slug — Projekt");
        projekt.setSlug("doppelter-slug");
        projekt.setKategorie("Sport");
        when(projektRepository.findBySlug("doppelter-slug")).thenReturn(Optional.of(projekt));

        renderer.rendereVerein("doppelter-slug");
        renderer.rendereProjekt("doppelter-slug");

        // Beide haben denselben Slug, aber unterschiedliche Cache-Keys (verein:* vs projekt:*)
        // → BEIDE Repos werden je 1x getroffen, KEIN unbeabsichtigter Cache-Hit
        verify(organisationRepository, times(1)).findBySlug("doppelter-slug");
        verify(projektRepository, times(1)).findBySlug("doppelter-slug");
    }

    @Test
    @DisplayName("OG-06: Cache-Region 'og-images' ist explizit registriert")
    void ogCacheRegionExistiert() {
        assertThat(cacheManager.getCacheNames()).contains(CacheConfig.OG_IMAGES);
    }
}
