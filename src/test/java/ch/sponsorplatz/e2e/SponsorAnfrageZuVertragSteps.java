package ch.sponsorplatz.e2e;

import ch.sponsorplatz.anfrage.SponsoringAnfrageRepository;
import ch.sponsorplatz.anfrage.VertragRepository;
import ch.sponsorplatz.organisation.Organisation;
import ch.sponsorplatz.organisation.OrganisationRepository;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;
import io.cucumber.java.de.Angenommen;
import io.cucumber.java.de.Dann;
import io.cucumber.java.de.Und;
import io.cucumber.java.de.Wenn;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Steps fuer das Pilot-Szenario:
 * Verein-Registrierung → Verein-Anlage → Projekt + Paket → CSS stellt Anfrage
 * → Verein nimmt an → Vertrag wird erstellt.
 *
 * <p>Wir nutzen Playwright's {@link Page#getByLabel}/{@link Page#getByRole}-
 * Locators wo moeglich, weil sie an die sichtbaren Beschriftungen binden
 * (i18n-stabil + accessibility-getrieben), und nur dort
 * CSS-Selektoren wo Labels mehrdeutig sind.
 */
public class SponsorAnfrageZuVertragSteps {

    private static final String VEREIN_PASSWORT = "Verein12345!";

    @Autowired private E2EContext ctx;
    @Autowired private E2EFixtures fixtures;
    @Autowired private OrganisationRepository orgRepository;
    @Autowired private SponsoringAnfrageRepository anfrageRepository;
    @Autowired private VertragRepository vertragRepository;

    // -------- Background --------

    @Angenommen("die Plattform läuft mit dem Test-Sponsor {string}")
    public void plattformLaeuftMitTestSponsor(String sponsorName) {
        Organisation sponsor = orgRepository.findBySlug("css-versicherung")
                .orElseThrow(() -> new AssertionError(
                        "Sponsor-Fixture wurde nicht von @Before geseedet"));
        assertThat(sponsor.getName()).isEqualTo(sponsorName);
        ctx.getDaten().put("sponsorOrgId", sponsor.getId());
    }

    // -------- Verein-Registrierung + Onboarding --------

    @Wenn("ich mich als neuer Verein-Owner {string} registriere")
    public void registriere(String anzeigename) {
        Page page = ctx.getPage();
        String email = ctx.eindeutigeEmail("verein");
        ctx.getDaten().put("vereinEmail", email);
        ctx.getDaten().put("vereinAnzeigename", anzeigename);

        page.navigate(ctx.getBaseUrl() + "/registrieren");
        page.getByLabel("E-Mail").fill(email);
        page.getByLabel("Anzeigename").fill(anzeigename);
        page.getByLabel("Passwort").fill(VEREIN_PASSWORT);
        page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Registrieren"))
                .click();
        page.waitForURL("**/login**");
    }

    @Und("meine E-Mail-Adresse bestätigt wird")
    public void emailBestaetigt() {
        fixtures.markiereEmailVerifiziert(ctx.daten("vereinEmail"));
    }

    @Und("ich mich einlogge")
    public void verein_einlogge() {
        login(ctx.daten("vereinEmail"), VEREIN_PASSWORT);
    }

    @Und("ich im Onboarding den Verein {string} in der Branche {string} anlege")
    public void onboarding_vereinAnlegen(String vereinName, String branche) {
        Page page = ctx.getPage();
        // Nach Login direkt auf /dashboard; ohne Org leitet das auf /onboarding.
        // Wir navigieren explizit, falls der Redirect schon konsumiert wurde.
        if (!page.url().contains("/onboarding")) {
            page.navigate(ctx.getBaseUrl() + "/onboarding");
        }
        page.locator("#vereinName").fill(vereinName);
        page.locator("#branche").selectOption(branche);
        page.getByRole(AriaRole.BUTTON,
                        new Page.GetByRoleOptions().setName("Verein erstellen"))
                .first()
                .click();
        page.waitForURL("**/dashboard**");

        // Slug ableiten — der SlugGenerator macht Lowercase + Bindestriche
        ctx.getDaten().put("vereinName", vereinName);
        ctx.getDaten().put("vereinSlug", vereinName.toLowerCase().replace(' ', '-'));
    }

    // -------- Projekt + Paket --------

    @Und("ich ein Projekt {string} mit Sponsoring-Paket {string} zu {int} CHF erstelle")
    public void projektUndPaket(String projektName, String paketName, int preisChf) {
        Page page = ctx.getPage();
        String vereinSlug = ctx.daten("vereinSlug");

        page.navigate(ctx.getBaseUrl() + "/organisationen/" + vereinSlug + "/projekte/neu");
        page.locator("#name").fill(projektName);
        page.locator("#beschreibung").fill("E2E-Projekt, Beschreibung egal");
        page.getByRole(AriaRole.BUTTON,
                        new Page.GetByRoleOptions().setName("Speichern"))
                .first()
                .click();
        page.waitForURL("**/projekte/" + projektSlug(projektName));

        // Projekt veroeffentlichen — sonst erscheint es nicht im Marktplatz
        page.getByRole(AriaRole.BUTTON,
                        new Page.GetByRoleOptions().setName("Veröffentlichen"))
                .first()
                .click();
        page.waitForURL("**/projekte/" + projektSlug(projektName) + "**");

        // Paket im Projekt-Detail anlegen
        page.locator("#paketName").fill(paketName);
        page.locator("#paketPreis").fill(String.valueOf(preisChf));
        page.getByRole(AriaRole.BUTTON,
                        new Page.GetByRoleOptions().setName("Paket speichern"))
                .first()
                .click();
        page.waitForURL("**/projekte/" + projektSlug(projektName) + "**");

        ctx.getDaten().put("projektSlug", projektSlug(projektName));
        ctx.getDaten().put("paketName", paketName);
    }

    // -------- Sponsor stellt Anfrage --------

    @Und("sich der Sponsor {string} einloggt")
    public void sponsorEinloggen(String sponsorName) {
        assertThat(sponsorName).isEqualTo(E2EFixtures.SPONSOR_NAME);
        logout();
        login(E2EFixtures.SPONSOR_EMAIL, E2EFixtures.SPONSOR_PASSWORT);
    }

    @Und("der Sponsor eine Sponsor-Anfrage zum Paket {string} stellt")
    public void sponsorStelltAnfrage(String paketName) {
        Page page = ctx.getPage();
        String projektSlug = ctx.daten("projektSlug");

        page.navigate(ctx.getBaseUrl() + "/marktplatz/" + projektSlug);
        page.getByRole(AriaRole.LINK,
                        new Page.GetByRoleOptions().setName("Anfrage stellen"))
                .first()
                .click();
        page.waitForURL("**/anfragen/neu**");

        // anfragenderOrgId-Dropdown enthaelt nur die Sponsor-Org
        page.locator("#anfragenderOrgId")
                .selectOption(((UUID) ctx.daten("sponsorOrgId")).toString());
        page.locator("#nachricht")
                .fill("Wir bei CSS würden gerne das Sommerfest sponsern.");
        page.getByRole(AriaRole.BUTTON,
                        new Page.GetByRoleOptions().setName("Anfrage senden"))
                .first()
                .click();
        page.waitForURL("**/anfragen**");
    }

    // -------- Annehmen + Vertrag --------

    @Und("ich mich wieder als Verein-Owner einlogge")
    public void verein_reLogin() {
        logout();
        login(ctx.daten("vereinEmail"), VEREIN_PASSWORT);
    }

    @Und("ich die Anfrage von {string} annehme")
    public void anfrageAnnehmen(String sponsorName) {
        Page page = ctx.getPage();
        page.navigate(ctx.getBaseUrl() + "/anfragen");
        // Im Eingehend-Block der ersten passenden Anfrage auf "Annehmen" klicken
        page.locator("form[action*='/annehmen']")
                .first()
                .getByRole(AriaRole.BUTTON,
                        new com.microsoft.playwright.Locator.GetByRoleOptions().setName("Annehmen"))
                .click();
        page.waitForURL("**/anfragen**");

        UUID anfrageId = findeNeuesteAnfrageIdFuerSponsor(sponsorName);
        ctx.getDaten().put("anfrageId", anfrageId);
    }

    @Und("ich für die angenommene Anfrage einen Vertrag erstelle")
    public void vertragErstellen() {
        Page page = ctx.getPage();
        // Auf /anfragen erscheint der "Vertrag erstellen"-Button bei
        // ANGENOMMEN-Paket-Anfragen. Ein POST über Form-Submit reicht.
        page.locator("form[action*='/vertrag/erstellen']")
                .first()
                .getByRole(AriaRole.BUTTON,
                        new com.microsoft.playwright.Locator.GetByRoleOptions().setName("Vertrag erstellen"))
                .click();
        page.waitForURL("**/vertraege/**");
    }

    // -------- Assertion --------

    @Dann("existiert in der Datenbank ein Vertrag zwischen {string} und {string}")
    public void vertragExistiert(String anfragenderName, String empfaengerName) {
        long anzahl = zaehleVertraegeZwischen(anfragenderName, empfaengerName);
        assertThat(anzahl)
                .as("Vertrag zwischen %s und %s sollte existieren", anfragenderName, empfaengerName)
                .isGreaterThanOrEqualTo(1);
    }

    // -------- Helpers --------

    private void login(String email, String passwort) {
        Page page = ctx.getPage();
        page.navigate(ctx.getBaseUrl() + "/login");
        page.locator("#username").fill(email);
        page.locator("#password").fill(passwort);
        page.getByRole(AriaRole.BUTTON,
                        new Page.GetByRoleOptions().setName("Anmelden"))
                .first()
                .click();
        // Login redirected je nach Onboarding-Status; URL-Wait auf einen der
        // gueltigen Folgezustaende
        page.waitForFunction("() => !location.pathname.endsWith('/login')");
    }

    private void logout() {
        Page page = ctx.getPage();
        // POST /logout via JS direkt — die Sidebar-Form ist u.U. nicht sichtbar
        page.context().clearCookies();
    }

    private String projektSlug(String name) {
        return name.toLowerCase().replace(' ', '-');
    }

    @Transactional
    public long zaehleVertraegeZwischen(String anfragenderName, String empfaengerName) {
        return vertragRepository.findAll().stream()
                .filter(v -> {
                    var a = v.getAnfrage();
                    if (a == null) return false;
                    return a.getAnfragenderOrg().getName().equals(anfragenderName)
                            && a.getEmpfaengerOrg().getName().equals(empfaengerName);
                })
                .count();
    }

    @Transactional
    public UUID findeNeuesteAnfrageIdFuerSponsor(String sponsorName) {
        return anfrageRepository.findAll().stream()
                .filter(a -> a.getAnfragenderOrg().getName().equals(sponsorName))
                .map(a -> a.getId())
                .findFirst()
                .orElseThrow(() -> new AssertionError(
                        "Anfrage von " + sponsorName + " nicht gefunden"));
    }
}
