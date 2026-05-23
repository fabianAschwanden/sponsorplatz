package ch.sponsorplatz.e2e;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.options.ScreenshotType;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Accessibility-Smoke-Suite — lädt jede öffentliche Hauptseite, injiziert
 * {@code axe-core} und prüft auf WCAG-2.1-AA-Verletzungen mit Severity
 * {@code serious} oder {@code critical}.
 *
 * <p>Zweck:
 * <ul>
 *   <li>Schweizer DSG-/Behindertengleichstellungs-Vorgaben: öffentliche
 *       Web-Angebote sollten WCAG-2.1-AA erfüllen.</li>
 *   <li>Pilot-Vorbereitung: Vor dem ersten Echtbenutzer-Onboarding einen
 *       Baseline-Check.</li>
 *   <li>Regression-Schutz: bei neuen Templates fällt der CI-Lauf rot aus,
 *       wenn die a11y-Grundwerte unterschritten werden.</li>
 * </ul>
 *
 * <p>Geprüfte Seiten (public, ohne Auth):
 * <ul>
 *   <li>{@code /} — Home/Marketing</li>
 *   <li>{@code /login} — Authentifizierungs-Einstieg</li>
 *   <li>{@code /kontakt} — Anfrage-Funnel</li>
 *   <li>{@code /impressum}, {@code /datenschutz}, {@code /agb} — Legal-Pages</li>
 * </ul>
 *
 * <p>Auth-pflichtige Seiten (Phase 13.1, Backlog-V40) — Login via
 * {@code dev@sponsorplatz.ch}/{@code dev} aus dem {@code DevSeedRunner}:
 * <ul>
 *   <li>{@code /dashboard} — Eingelogged-Hub</li>
 *   <li>{@code /aufgaben} — Task-Inbox</li>
 *   <li>{@code /meine-anfragen} — Anfrage-Übersicht</li>
 *   <li>{@code /einstellungen} — Profil + Sicherheit + 2FA-Eingang</li>
 * </ul>
 *
 * <p>{@code /onboarding} wurde aus dem Auth-Suite-Coverage bewusst raus-
 * gehalten: der Dev-Seed-User ist {@code PLATFORM_ADMIN} und wird vom
 * {@code OnboardingController} grundsätzlich auf {@code /dashboard}
 * umgeleitet (Plattform-Admins durchlaufen kein Onboarding). Ein
 * dedizierter Non-Admin-Test-User ist Folge-Arbeit (TODO via
 * E2EFixtures).
 *
 * <p>Liegt im {@code e2e}-Paket, läuft via {@code mvn verify -P e2e -Dit.test=A11ySmokeIT}.
 *
 * <p>Test-IDs: A11Y-01..06 (public) + A11Y-07..10 (auth) in {@code specs/TESTSTRATEGIE.md}.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT,
        properties = "server.port=18086")
@ActiveProfiles("dev")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.DisplayName.class)
class A11ySmokeIT {

    /**
     * axe-core wird als inline-Script in die Seite injiziert (CSP blockiert
     * Cross-Origin-Scripts vom CDN; {@code 'unsafe-inline'} ist im script-src
     * aber erlaubt — daher Source als String, nicht via URL).
     */
    private static final String AXE_RESOURCE = "/a11y/axe.min.js";

    /** Maximal-Severity, die wir noch tolerieren. Alles ab serious bricht. */
    private static final List<String> BLOCKING_IMPACTS = List.of("serious", "critical");

    /**
     * Bekannte axe-Regel-IDs, die wir aktuell tolerieren — als Baseline für
     * den Pilot-Launch. Wer hier etwas hinzufügt, muss in
     * {@code docs/a11y-bekannt.md} eine Begründung + Plan zur Behebung
     * dokumentieren. Aktuell leer — Coral-Text-Farben wurden auf
     * accessible {@code --dash-coral-text} umgestellt (≥4.5:1).
     */
    private static final List<String> ZUGELASSENE_IDS = List.of();

    private Playwright playwright;
    private Browser browser;
    private BrowserContext context;
    private BrowserContext authContext;
    private String axeSource;

    @Value("${server.port:18086}")
    private int port;

    @BeforeAll
    void setUpBrowser() throws IOException {
        playwright = Playwright.create();
        browser = playwright.chromium().launch();
        context = browser.newContext();
        authContext = browser.newContext();
        try (var in = getClass().getResourceAsStream(AXE_RESOURCE)) {
            if (in == null) {
                throw new IllegalStateException(
                        "axe.min.js fehlt im Classpath unter " + AXE_RESOURCE
                                + " — bitte erneut herunterladen (siehe Test-Doku)");
            }
            axeSource = new String(in.readAllBytes());
        }
        loginAuthContext();
    }

    /**
     * Loggt den Dev-Seed-User in {@link #authContext} ein. Session-Cookie
     * bleibt für alle Auth-Tests bestehen.
     */
    private void loginAuthContext() {
        try (Page page = authContext.newPage()) {
            page.navigate("http://localhost:" + port + "/login");
            page.locator("#username").fill("dev@sponsorplatz.ch");
            page.locator("#password").fill("dev");
            page.locator("button[type=submit]").first().click();
            page.waitForFunction("() => !location.pathname.endsWith('/login')");
        }
    }

    @AfterAll
    void tearDownBrowser() {
        if (authContext != null) authContext.close();
        if (context != null) context.close();
        if (browser != null) browser.close();
        if (playwright != null) playwright.close();
    }

    @Test
    @DisplayName("A11Y-01: Home / hat keine kritischen WCAG-Verstösse")
    void home() {
        pruefeSeite("/");
    }

    @Test
    @DisplayName("A11Y-02: /login hat keine kritischen WCAG-Verstösse")
    void login() {
        pruefeSeite("/login");
    }

    @Test
    @DisplayName("A11Y-03: /kontakt hat keine kritischen WCAG-Verstösse")
    void kontakt() {
        pruefeSeite("/kontakt");
    }

    @Test
    @DisplayName("A11Y-04: /impressum hat keine kritischen WCAG-Verstösse")
    void impressum() {
        pruefeSeite("/impressum");
    }

    @Test
    @DisplayName("A11Y-05: /datenschutz hat keine kritischen WCAG-Verstösse")
    void datenschutz() {
        pruefeSeite("/datenschutz");
    }

    @Test
    @DisplayName("A11Y-06: /agb hat keine kritischen WCAG-Verstösse")
    void agb() {
        pruefeSeite("/agb");
    }

    // ── Phase 13.1 — Auth-pflichtige Seiten (Backlog-V40) ─────────────

    @Test
    @DisplayName("A11Y-07: /dashboard (auth) hat keine kritischen WCAG-Verstösse")
    void dashboard() {
        pruefeAuthSeite("/dashboard");
    }

    @Test
    @DisplayName("A11Y-08: /aufgaben (auth) hat keine kritischen WCAG-Verstösse")
    void aufgaben() {
        pruefeAuthSeite("/aufgaben");
    }

    @Test
    @DisplayName("A11Y-09: /meine-anfragen (auth) hat keine kritischen WCAG-Verstösse")
    void meineAnfragen() {
        pruefeAuthSeite("/meine-anfragen");
    }

    @Test
    @DisplayName("A11Y-10: /einstellungen (auth) hat keine kritischen WCAG-Verstösse")
    void einstellungen() {
        pruefeAuthSeite("/einstellungen");
    }

    private void pruefeSeite(String pfad) {
        pruefeMitContext(pfad, context);
    }

    private void pruefeAuthSeite(String pfad) {
        pruefeMitContext(pfad, authContext);
    }

    @SuppressWarnings("unchecked")
    private void pruefeMitContext(String pfad, BrowserContext ctx) {
        try (Page page = ctx.newPage()) {
            page.navigate("http://localhost:" + port + pfad);
            // Inline injection — CSP blockiert Cross-Origin-Scripts, daher die Quelle direkt.
            page.addScriptTag(new Page.AddScriptTagOptions().setContent(axeSource));

            Object raw = page.evaluate(
                    "async () => { const r = await axe.run({"
                            + " runOnly: { type: 'tag', values: ['wcag2a', 'wcag2aa'] }"
                            + " }); return r.violations; }");

            List<Map<String, Object>> violations = (List<Map<String, Object>>) raw;
            List<Map<String, Object>> kritisch = violations.stream()
                    .filter(v -> BLOCKING_IMPACTS.contains((String) v.get("impact")))
                    .filter(v -> !ZUGELASSENE_IDS.contains((String) v.get("id")))
                    .toList();

            if (!kritisch.isEmpty()) {
                Path screenshot = Path.of("target/a11y-" + pfad.replace("/", "_") + ".png");
                page.screenshot(new Page.ScreenshotOptions()
                        .setPath(screenshot)
                        .setType(ScreenshotType.PNG)
                        .setFullPage(true));
            }

            assertThat(kritisch)
                    .as("Seite %s hat WCAG-2.1-AA-Verstösse (serious/critical) — "
                            + "Screenshot in target/a11y-%s.png", pfad, pfad.replace("/", "_"))
                    .extracting(v -> {
                        Object nodes = v.get("nodes");
                        String target = "?";
                        if (nodes instanceof List<?> ns && !ns.isEmpty()
                                && ns.get(0) instanceof Map<?, ?> firstNode) {
                            Object tg = firstNode.get("target");
                            if (tg != null) target = tg.toString();
                        }
                        return v.get("id") + " @ " + target + ": " + v.get("description");
                    })
                    .isEmpty();
        }
    }
}
