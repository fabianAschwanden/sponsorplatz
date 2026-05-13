package ch.sponsorplatz.e2e;

import jakarta.annotation.PreDestroy;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Playwright;

/**
 * Stellt Singleton-{@link Playwright} und Singleton-{@link Browser}-Beans
 * fuer die E2E-Suite bereit. {@code @PreDestroy} sorgt fuer sauberes
 * Herunterfahren — Playwright haelt sonst Node-Subprozesse offen.
 *
 * <p>
 * Browser-Wahl via System-Property {@code e2e.browser} (Default
 * {@code chromium}); auch {@code firefox} und {@code webkit} sind unterstuetzt.
 * Headless wird via {@code e2e.headless} gesteuert (Default true; auf
 * {@code false} setzen fuer Debug-Sessions).
 */
@TestConfiguration
public class E2EPlaywrightConfig {

    private Playwright playwright;
    private Browser browser;

    @Bean
    public Playwright playwright() {
        playwright = Playwright.create();
        return playwright;
    }

    @Bean
    public Browser browser(Playwright playwright,
            @Value("${e2e.browser:chromium}") String browserName,
            @Value("${e2e.headless:true}") boolean headless) {
        BrowserType type = switch (browserName) {
            case "firefox" -> playwright.firefox();
            case "webkit" -> playwright.webkit();
            default -> playwright.chromium();
        };
        browser = type.launch(new BrowserType.LaunchOptions().setHeadless(headless));
        return browser;
    }

    /**
     * Page wird pro Cucumber-Scenario via {@link E2EHooks} angelegt/geschlossen.
     * Hier definieren wir nur Browser + Playwright; den BrowserContext schneidet
     * Hooks pro Scenario, damit Cookies/Storage zwischen Szenarien isoliert sind.
     */

    @PreDestroy
    public void shutdown() {
        if (browser != null) {
            browser.close();
        }
        if (playwright != null) {
            playwright.close();
        }
    }
}
