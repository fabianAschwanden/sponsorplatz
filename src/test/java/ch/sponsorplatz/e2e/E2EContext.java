package ch.sponsorplatz.e2e;

import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.Page;
import io.cucumber.spring.ScenarioScope;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Zustand pro Cucumber-Scenario — bewusst {@code @ScenarioScope}, damit
 * Cucumber-Spring fuer jedes Scenario eine frische Instanz baut.
 *
 * <p>Enthaelt den Playwright-{@link Page} (von {@link E2EHooks} gesetzt) sowie
 * Test-spezifische IDs/Emails, die Steps untereinander austauschen muessen
 * (z.B. die zufallsgenerierte Verein-User-E-Mail).
 */
@Component
@ScenarioScope
public class E2EContext {

    private String baseUrl;
    private BrowserContext browserContext;
    private Page page;

    /**
     * Frei verwendbarer Bag fuer Test-Daten — Steps speichern hier z.B.
     * {@code uniqueEmail}, {@code vereinName}, {@code anfrageId}, damit
     * spaetere Schritte denselben Wert referenzieren.
     */
    private final Map<String, Object> daten = new HashMap<>();

    /** Generiert eine eindeutige E-Mail fuer den Test-Lauf. */
    public String eindeutigeEmail(String praefix) {
        return praefix + "-" + UUID.randomUUID().toString().substring(0, 8).toLowerCase()
                + "@e2e.test";
    }

    public String getBaseUrl() { return baseUrl; }
    public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }

    public BrowserContext getBrowserContext() { return browserContext; }
    public void setBrowserContext(BrowserContext browserContext) {
        this.browserContext = browserContext;
    }

    public Page getPage() { return page; }
    public void setPage(Page page) { this.page = page; }

    public Map<String, Object> getDaten() { return daten; }

    @SuppressWarnings("unchecked")
    public <T> T daten(String key) {
        return (T) daten.get(key);
    }
}
