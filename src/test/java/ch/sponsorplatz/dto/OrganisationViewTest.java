package ch.sponsorplatz.dto;

import ch.sponsorplatz.model.Branche;
import ch.sponsorplatz.model.OrgStatus;
import ch.sponsorplatz.model.OrgTyp;
import ch.sponsorplatz.model.Organisation;
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
