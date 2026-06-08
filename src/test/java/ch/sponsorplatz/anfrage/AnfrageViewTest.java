package ch.sponsorplatz.anfrage;

import ch.sponsorplatz.organisation.OrgTyp;
import ch.sponsorplatz.organisation.Organisation;
import ch.sponsorplatz.projekt.SponsoringPaket;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class AnfrageViewTest {

    /** VIEW-06: AnfrageView mappt Anfrage inkl. paketName (flach). */
    @Test
    void mappingEinerAnfrage() {
        UUID anfrageId = UUID.randomUUID();

        SponsoringPaket paket = new SponsoringPaket();
        paket.setId(UUID.randomUUID());
        paket.setName("Gold-Paket");

        Organisation anfragender = new Organisation();
        anfragender.setId(UUID.randomUUID());
        anfragender.setName("Sponsor AG");
        Organisation empfaenger = new Organisation();
        empfaenger.setId(UUID.randomUUID());
        empfaenger.setName("Sportverein Zürich");
        empfaenger.setSlug("sportverein-zuerich");

        SponsoringAnfrage a = new SponsoringAnfrage();
        a.setId(anfrageId);
        a.setStatus(AnfrageStatus.NEU);
        a.setNachricht("Wir würden gerne sponsern.");
        a.setKontaktName("Max Müller");
        a.setKontaktEmail("max@firma.ch");
        a.setPaket(paket);
        a.setAnfragenderOrg(anfragender);
        a.setEmpfaengerOrg(empfaenger);

        AnfrageView view = AnfrageView.von(a);

        assertThat(view.id()).isEqualTo(anfrageId);
        assertThat(view.status()).isEqualTo(AnfrageStatus.NEU);
        assertThat(view.nachricht()).isEqualTo("Wir würden gerne sponsern.");
        assertThat(view.kontaktName()).isEqualTo("Max Müller");
        assertThat(view.kontaktEmail()).isEqualTo("max@firma.ch");
        assertThat(view.paketName()).isEqualTo("Gold-Paket");
        assertThat(view.empfaengerOrgSlug()).isEqualTo("sportverein-zuerich");
        assertThat(view.empfaengerOrgName()).isEqualTo("Sportverein Zürich");
        assertThat(view.anfragenderOrgName()).isEqualTo("Sponsor AG");
    }

    /**
     * VIEW-13: vereinSlug() liefert den korrekten Slug abhaengig vom
     * Anfrage-Typ — der Vertrag wird immer beim Verein angelegt, der
     * jeweilige Slug variiert aber zwischen Paket- und Kontakt-Anfrage.
     */
    @Test
    void vereinSlugBeiPaketAnfrage() {
        // Paket-Anfrage: anfragender = Sponsor, empfaenger = Verein
        Organisation sponsor = neueOrg("Sponsor AG", "sponsor-ag", OrgTyp.UNTERNEHMEN);
        Organisation verein = neueOrg("FC Beispiel", "fc-beispiel", OrgTyp.VEREIN);
        SponsoringPaket paket = new SponsoringPaket();
        paket.setName("Gold");

        SponsoringAnfrage a = new SponsoringAnfrage();
        a.setId(UUID.randomUUID());
        a.setStatus(AnfrageStatus.ANGENOMMEN);
        a.setAnfragenderOrg(sponsor);
        a.setEmpfaengerOrg(verein);
        a.setPaket(paket);

        AnfrageView v = AnfrageView.von(a);

        assertThat(v.vereinSlug())
                .as("Bei Paket-Anfrage ist der Verein der Empfaenger")
                .isEqualTo("fc-beispiel");
    }

    @Test
    void vereinSlugBeiKontaktAnfrage() {
        // Kontakt-Anfrage: anfragender = Verein, empfaenger = Sponsor
        Organisation verein = neueOrg("FC Beispiel", "fc-beispiel", OrgTyp.VEREIN);
        Organisation sponsor = neueOrg("CSS Versicherung", "css", OrgTyp.UNTERNEHMEN);

        SponsoringAnfrage a = new SponsoringAnfrage();
        a.setId(UUID.randomUUID());
        a.setStatus(AnfrageStatus.NEU);
        a.setAnfragenderOrg(verein);
        a.setEmpfaengerOrg(sponsor);
        a.setBetreff("Sommerfest");
        // paket = null  → Kontakt-Anfrage

        AnfrageView v = AnfrageView.von(a);

        assertThat(v.vereinSlug())
                .as("Bei Kontakt-Anfrage ist der Verein der Anfragende")
                .isEqualTo("fc-beispiel");
    }

    /** VIEW-16: istNeu()/istErledigt() partitionieren nach Status (neu vs. erledigt-Liste). */
    @org.junit.jupiter.api.Test
    void istNeuUndIstErledigt() {
        assertThat(mitStatus(AnfrageStatus.NEU).istNeu()).isTrue();
        assertThat(mitStatus(AnfrageStatus.NEU).istErledigt()).isFalse();

        assertThat(mitStatus(AnfrageStatus.ANGENOMMEN).istErledigt()).isTrue();
        assertThat(mitStatus(AnfrageStatus.ANGENOMMEN).istNeu()).isFalse();

        assertThat(mitStatus(AnfrageStatus.ABGELEHNT).istErledigt()).isTrue();
        assertThat(mitStatus(AnfrageStatus.ABGELEHNT).istNeu()).isFalse();
    }

    private static AnfrageView mitStatus(AnfrageStatus status) {
        SponsoringAnfrage a = new SponsoringAnfrage();
        a.setId(UUID.randomUUID());
        a.setStatus(status);
        return AnfrageView.von(a);
    }

    /** VIEW-17: OFFEN_DANN_NEUSTE hebt NEU vor erledigte, erhält sonst die Eingangsordnung. */
    @org.junit.jupiter.api.Test
    void offenDannNeusteSortierung() {
        AnfrageView erledigt = mitStatus(AnfrageStatus.ANGENOMMEN);
        AnfrageView neu = mitStatus(AnfrageStatus.NEU);

        var sortiert = java.util.stream.Stream.of(erledigt, neu)
                .sorted(AnfrageView.OFFEN_DANN_NEUSTE).toList();

        assertThat(sortiert.get(0).istNeu()).isTrue();
        assertThat(sortiert.get(1).istErledigt()).isTrue();
    }

    private static Organisation neueOrg(String name, String slug, OrgTyp typ) {
        Organisation o = new Organisation();
        o.setId(UUID.randomUUID());
        o.setName(name);
        o.setSlug(slug);
        o.setTyp(typ);
        return o;
    }
}
