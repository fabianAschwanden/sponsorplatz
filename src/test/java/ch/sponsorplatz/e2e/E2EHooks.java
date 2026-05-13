package ch.sponsorplatz.e2e;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.Page;
import io.cucumber.java.After;
import io.cucumber.java.Before;
import io.cucumber.java.Scenario;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.server.LocalServerPort;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Browser-Lifecycle pro Scenario.
 *
 * <ul>
 *   <li>{@code @Before} oeffnet einen frischen BrowserContext (= eigene
 *       Cookies, Storage) + Page und schreibt {@code baseUrl} in den Kontext,
 *       damit Steps {@code page.navigate(ctx.getBaseUrl() + "/login")} machen
 *       koennen.</li>
 *   <li>{@code @After} schliesst Page + Context und schiesst bei fehlgeschlagenen
 *       Szenarien einen Screenshot ins {@code target/}-Verzeichnis fuer
 *       Post-Mortem-Debugging.</li>
 * </ul>
 */
public class E2EHooks {

    @LocalServerPort
    private int serverPort;

    @Autowired
    private Browser browser;

    @Autowired
    private E2EContext ctx;

    @Autowired
    private E2EFixtures fixtures;

    @Before(order = 0)
    public void datenbankBereinigenUndSeeden() {
        // Sauberes Out-of-Box-State pro Scenario, damit Email-Unique-Constraint
        // bei wiederholten Laeufen nicht kollidiert. Sponsor + Sponsor-Owner
        // werden dann frisch geseedet.
        fixtures.bereinige();
        fixtures.seedeCssSponsor();
    }

    @Before(order = 10)
    public void browserContextStarten() {
        BrowserContext context = browser.newContext(new Browser.NewContextOptions()
                .setViewportSize(1280, 800)
                .setLocale("de-CH"));
        Page page = context.newPage();
        ctx.setBaseUrl("http://localhost:" + serverPort);
        ctx.setBrowserContext(context);
        ctx.setPage(page);
    }

    @After
    public void aufraeumen(Scenario scenario) {
        try {
            if (scenario.isFailed() && ctx.getPage() != null) {
                Path screenshot = Paths.get("target",
                        "e2e-failure-" + sicherDateiname(scenario.getName()) + ".png");
                ctx.getPage().screenshot(new Page.ScreenshotOptions()
                        .setPath(screenshot)
                        .setFullPage(true));
                scenario.attach(ctx.getPage().content().getBytes(),
                        "text/html", "page-content-on-failure");
            }
        } finally {
            if (ctx.getPage() != null) ctx.getPage().close();
            if (ctx.getBrowserContext() != null) ctx.getBrowserContext().close();
        }
    }

    private String sicherDateiname(String name) {
        if (name == null) return "unknown";
        return name.replaceAll("[^A-Za-z0-9._-]", "_");
    }
}
