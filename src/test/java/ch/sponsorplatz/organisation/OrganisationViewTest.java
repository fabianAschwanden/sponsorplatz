package ch.sponsorplatz.organisation;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class OrganisationViewTest {

    /** VIEW-01: von(entity) mappt alle relevanten Felder. */
    @Test
    void mappingEinerOrganisation() {
        UUID id = UUID.randomUUID();
        Instant registriert = Instant.now();
        Instant verifiziert = Instant.now().minusSeconds(3600);

        Organisation org = new Organisation();
        org.setId(id);
        org.setName("FC Test");
        org.setSlug("fc-test");
        org.setTyp(OrgTyp.VEREIN);
        org.setStatus(OrgStatus.VERIFIED);
        org.setRechtsform("Verein");
        org.setBranche(Branche.SPORT);
        org.setBeschreibung("Beschreibung");
        org.setWebsiteUrl("https://fc-test.ch");
        org.setRegistriertAm(registriert);
        org.setVerifiziertAm(verifiziert);

        OrganisationView view = OrganisationView.von(org);

        assertThat(view.id()).isEqualTo(id);
        assertThat(view.name()).isEqualTo("FC Test");
        assertThat(view.slug()).isEqualTo("fc-test");
        assertThat(view.typ()).isEqualTo(OrgTyp.VEREIN);
        assertThat(view.status()).isEqualTo(OrgStatus.VERIFIED);
        assertThat(view.rechtsform()).isEqualTo("Verein");
        assertThat(view.branche()).isEqualTo(Branche.SPORT);
        assertThat(view.beschreibung()).isEqualTo("Beschreibung");
        assertThat(view.websiteUrl()).isEqualTo("https://fc-test.ch");
        assertThat(view.registriertAm()).isEqualTo(registriert);
        assertThat(view.verifiziertAm()).isEqualTo(verifiziert);
        assertThat(view.uebergeordneteOrgId()).isNull();
        assertThat(view.istUnterorganisation()).isFalse();
    }

    /** VIEW-02: von(List) mappt jedes Element. */
    @Test
    void mappingEinerListe() {
        Organisation a = neueOrg("A", "a");
        Organisation b = neueOrg("B", "b");

        List<OrganisationView> views = OrganisationView.von(List.of(a, b));

        assertThat(views).hasSize(2);
        assertThat(views.get(0).slug()).isEqualTo("a");
        assertThat(views.get(1).slug()).isEqualTo("b");
    }

    /** VIEW-03: Hierarchie-Felder werden korrekt gemappt. */
    @Test
    void mappingMitElternOrg() {
        Organisation eltern = neueOrg("Konzern AG", "konzern-ag");
        Organisation kind = neueOrg("Abteilung", "abteilung");
        kind.setUebergeordneteOrg(eltern);

        OrganisationView view = OrganisationView.von(kind);

        assertThat(view.uebergeordneteOrgId()).isEqualTo(eltern.getId());
        assertThat(view.uebergeordneteOrgName()).isEqualTo("Konzern AG");
        assertThat(view.uebergeordneteOrgSlug()).isEqualTo("konzern-ag");
        assertThat(view.istUnterorganisation()).isTrue();
    }

    /** VIEW-03b: von(entity) liefert logoUrl=null (Logo wird im Controller nachgereicht). */
    @Test
    void mappingOhneLogo() {
        OrganisationView view = OrganisationView.von(neueOrg("FC Test", "fc-test"));
        assertThat(view.logoUrl()).isNull();
    }

    /** VIEW-03c: mitLogoUrl() kopiert die View mit ergänztem logoUrl, alle anderen Felder bleiben. */
    @Test
    void mitLogoUrlKopiert() {
        OrganisationView original = OrganisationView.von(neueOrg("FC Test", "fc-test"));

        OrganisationView mitLogo = original.mitLogoUrl("/medien/abc-123");

        assertThat(mitLogo.logoUrl()).isEqualTo("/medien/abc-123");
        assertThat(mitLogo.slug()).isEqualTo("fc-test");
        assertThat(mitLogo.name()).isEqualTo("FC Test");
        assertThat(mitLogo.id()).isEqualTo(original.id());
        // Original bleibt unverändert (immutable record)
        assertThat(original.logoUrl()).isNull();
    }

    private Organisation neueOrg(String name, String slug) {
        Organisation o = new Organisation();
        o.setId(UUID.randomUUID());
        o.setName(name);
        o.setSlug(slug);
        o.setTyp(OrgTyp.VEREIN);
        o.setStatus(OrgStatus.PENDING);
        o.setRegistriertAm(Instant.now());
        return o;
    }
}
