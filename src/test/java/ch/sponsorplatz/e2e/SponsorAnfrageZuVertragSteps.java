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
 * Steps für den vom User definierten E2E-Flow (siehe
 * specs/KONTAKT_ANFRAGE_VERTRAG.md):
 * Verein-Registrierung → Verein anlegen → Kontakt-Anfrage an CSS →
 * CSS nimmt an → Verein erstellt Vertrag.
 *
 * <p>Bewusst <b>keine</b> Paket-Erstellung — der Flow ist Verein-zu-Sponsor-
 * Initiative, kein Marktplatz-Browse-Pfad. Der Vertrag entsteht aus einer
 * paket-losen Kontakt-Anfrage (Snapshot via {@code VertragService.erstelle}).
 *
 * <p>Locator-Strategie:
 * <ul>
 *   <li>{@code page.getByLabel(...)} für Form-Felder (i18n-stabil)</li>
 *   <li>{@code page.getByRole(BUTTON, name=...)} für Klick-Targets</li>
 *   <li>Inline CSS-Selektoren nur, wenn Labels mehrdeutig sind</li>
 * </ul>
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
        ctx.getDaten().put("sponsorName", sponsorName);
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

        ctx.getDaten().put("vereinName", vereinName);
        ctx.getDaten().put("vereinSlug", vereinName.toLowerCase().replace(' ', '-'));
    }

    // -------- Verein stellt Kontakt-Anfrage --------

    @Und("ich eine Kontakt-Anfrage an {string} mit Betreff {string} stelle")
    public void stelleKontaktAnfrage(String sponsorName, String betreff) {
        Page page = ctx.getPage();
        page.navigate(ctx.getBaseUrl() + "/anfragen/neu-kontakt");

        // Verein-Org-Dropdown — Verein-Owner hat nur eine Org
        page.locator("#anfragenderOrgId")
                .selectOption(new com.microsoft.playwright.options.SelectOption()
                        .setLabel(ctx.daten("vereinName")));

        // Sponsor-Dropdown — wir wählen via UUID, weil Label das
        // Branche-Suffix enthält (z.B. "CSS Versicherung (Prävention)")
        page.locator("#empfaengerOrgId")
                .selectOption(((UUID) ctx.daten("sponsorOrgId")).toString());

        page.locator("#betreff").fill(betreff);
        page.locator("#nachricht").fill(
                "Wir vom Verein FC E2E würden uns sehr freuen, " + sponsorName
                + " für unser Sommerfest 2026 als Sponsor zu gewinnen.");

        page.getByRole(AriaRole.BUTTON,
                        new Page.GetByRoleOptions().setName("Anfrage senden"))
                .first()
                .click();
        page.waitForURL("**/anfragen**");

        ctx.getDaten().put("betreff", betreff);
    }

    // -------- Sponsor nimmt an --------

    @Und("sich der Sponsor {string} einloggt")
    public void sponsorEinloggen(String sponsorName) {
        assertThat(sponsorName).isEqualTo(E2EFixtures.SPONSOR_NAME);
        logout();
        login(E2EFixtures.SPONSOR_EMAIL, E2EFixtures.SPONSOR_PASSWORT);
    }

    @Und("der Sponsor die Kontakt-Anfrage von {string} annimmt")
    public void sponsorNimmtKontaktAnfrageAn(String vereinName) {
        Page page = ctx.getPage();
        page.navigate(ctx.getBaseUrl() + "/anfragen");
        // Im Eingehend-Block der ersten passenden Anfrage auf "Annehmen" klicken.
        // Bei nur 1 Anfrage im Test-Scope reicht .first().
        page.locator("form[action*='/annehmen']")
                .first()
                .getByRole(AriaRole.BUTTON,
                        new com.microsoft.playwright.Locator.GetByRoleOptions().setName("Annehmen"))
                .click();
        page.waitForURL("**/anfragen**");

        UUID anfrageId = findeKontaktAnfrageId(vereinName);
        ctx.getDaten().put("anfrageId", anfrageId);
    }

    // -------- Verein erstellt Vertrag --------

    @Und("ich mich wieder als Verein-Owner einlogge")
    public void verein_reLogin() {
        logout();
        login(ctx.daten("vereinEmail"), VEREIN_PASSWORT);
    }

    @Und("ich für die angenommene Kontakt-Anfrage einen Vertrag erstelle")
    public void vertragErstellen() {
        Page page = ctx.getPage();
        page.navigate(ctx.getBaseUrl() + "/anfragen");
        // Vertrag-Button erscheint bei ANGENOMMENen Kontakt-Anfragen in der
        // "Meine ausgehende"-Section — siehe meine-anfragen.html.
        page.locator("form[action*='/vertrag/erstellen']")
                .first()
                .getByRole(AriaRole.BUTTON,
                        new com.microsoft.playwright.Locator.GetByRoleOptions().setName("Vertrag erstellen"))
                .click();
        page.waitForURL("**/vertraege/**");
    }

    // -------- Assertions --------

    @Dann("existiert in der Datenbank ein Vertrag zwischen {string} und {string}")
    public void vertragExistiert(String vereinName, String sponsorName) {
        long anzahl = zaehleVertraegeZwischen(vereinName, sponsorName);
        assertThat(anzahl)
                .as("Vertrag zwischen %s und %s sollte existieren", vereinName, sponsorName)
                .isGreaterThanOrEqualTo(1);
    }

    @Und("der Vertrag referenziert die Kontakt-Anfrage als Quelle")
    public void vertragReferenziertKontaktAnfrage() {
        UUID anfrageId = ctx.daten("anfrageId");
        long mitAnfrage = vertragRepository.findAll().stream()
                .filter(v -> v.getAnfrage() != null
                        && v.getAnfrage().getId().equals(anfrageId)
                        && v.getAnfrage().getPaket() == null) // Kontakt-Anfrage = paket-frei
                .count();
        assertThat(mitAnfrage)
                .as("Vertrag muss an die paket-lose Kontakt-Anfrage gebunden sein")
                .isEqualTo(1);
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
        page.waitForFunction("() => !location.pathname.endsWith('/login')");
    }

    private void logout() {
        ctx.getPage().context().clearCookies();
    }

    @Transactional
    public long zaehleVertraegeZwischen(String vereinName, String sponsorName) {
        return vertragRepository.findAll().stream()
                .filter(v -> v.getOrg() != null
                        && v.getOrg().getName().equals(vereinName)
                        && v.getSponsorOrg() != null
                        && v.getSponsorOrg().getName().equals(sponsorName))
                .count();
    }

    @Transactional
    public UUID findeKontaktAnfrageId(String vereinName) {
        return anfrageRepository.findAll().stream()
                .filter(a -> a.getAnfragenderOrg().getName().equals(vereinName)
                        && a.getPaket() == null)
                .map(a -> a.getId())
                .findFirst()
                .orElseThrow(() -> new AssertionError(
                        "Kontakt-Anfrage von " + vereinName + " nicht gefunden"));
    }
}
