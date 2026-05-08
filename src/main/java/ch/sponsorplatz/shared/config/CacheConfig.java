package ch.sponsorplatz.shared.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

/**
 * Cache-Konfiguration.
 *
 * <p>Aktuell wird nur die Marken-Landing-Statistik gecacht — Marketing-Traffic
 * trifft sonst pro Page-Load eine Aggregat-Query gegen Postgres. TTL 5 min
 * ist ein bewusster Trade-off: Live-Zahlen müssen nicht sekundengenau sein,
 * aber lange Stale-Werte würden die Glaubwürdigkeit des Trust-Indikators
 * untergraben.
 *
 * <p>Caches sind explizit deklariert — versehentliches {@code @Cacheable("foo")}
 * mit unbekanntem Namen schlägt damit fail-fast (statt einen frischen
 * Default-Cache anzulegen, den niemand sieht).
 */
@Configuration
@EnableCaching
public class CacheConfig {

    public static final String STATISTIK_VEREINE_PRO_BRANCHE = "statistik-vereineProBranche";
    public static final String STATISTIK_ANZAHL_PROJEKTE     = "statistik-anzahlProjekte";
    /**
     * Cache für gerenderte OG-Preview-Bilder (~50 KB pro PNG, Single-VM-fit).
     * Verhindert, dass ein viraler Tweet/LinkedIn-Share die Server-CPU mit
     * BufferedImage-Allocations belegt — Browser/CDN deckt mit
     * {@code Cache-Control: max-age=3600} alle Re-Visits ab, der Server-Cache
     * absorbiert die initiale Render-Last beim Spike.
     */
    public static final String OG_IMAGES = "og-images";

    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager manager = new CaffeineCacheManager(
                STATISTIK_VEREINE_PRO_BRANCHE,
                STATISTIK_ANZAHL_PROJEKTE,
                OG_IMAGES);
        // 5-Min-TTL ist die enge Grenze (Statistik soll nicht stale wirken);
        // OG-Bilder profitieren auch davon — selbst 5 min Cache-Hit-Rate
        // schützt die CPU bei viralen Shares zuverlässig.
        manager.setCaffeine(Caffeine.newBuilder()
                .expireAfterWrite(5, TimeUnit.MINUTES)
                .maximumSize(256));
        manager.setAllowNullValues(false);
        return manager;
    }
}
