package ch.sponsorplatz.anfrage;

import ch.sponsorplatz.organisation.Branche;
import ch.sponsorplatz.organisation.OrgTyp;
import ch.sponsorplatz.organisation.Organisation;
import ch.sponsorplatz.projekt.Projekt;
import ch.sponsorplatz.projekt.SponsoringPaket;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * VIEW-ENG-01..02 — Rollen-Auflösung (Verein vs. Marke) über den Org-Typ und
 * Null-Sicherheit bei Kontakt-Anfragen ohne Paket.
 */
class EngagementViewTest {

    /** VIEW-ENG-01: Paket-Anfrage (Marke → Verein) — Rollen + Projekt-Felder korrekt. */
    @Test
    @DisplayName("VIEW-ENG-01: Paket-Anfrage mappt Marke/Verein + Projekt")
    void paketAnfrage() {
        Organisation marke = org("CSS", "css", OrgTyp.UNTERNEHMEN, null);
        Organisation verein = org("FC Beispiel", "fc-beispiel", OrgTyp.VEREIN, Branche.SPORT);
        Projekt projekt = new Projekt();
        projekt.setName("Sommerfest");
        projekt.setSlug("sommerfest");
        projekt.setOrt("Zürich");
        projekt.setOrg(verein);
        SponsoringPaket paket = new SponsoringPaket();
        paket.setName("Gold");
        paket.setProjekt(projekt);

        SponsoringAnfrage a = new SponsoringAnfrage();
        a.setId(UUID.randomUUID());
        a.setAnfragenderOrg(marke);   // Marke fragt das Paket an
        a.setEmpfaengerOrg(verein);
        a.setPaket(paket);
        a.setStatus(AnfrageStatus.ANGENOMMEN);
        a.setBeantwortetAm(Instant.now());

        EngagementView v = EngagementView.von(a);

        assertThat(v.sponsorName()).isEqualTo("CSS");
        assertThat(v.vereinName()).isEqualTo("FC Beispiel");
        assertThat(v.vereinBranche()).isEqualTo(Branche.SPORT);
        assertThat(v.projektName()).isEqualTo("Sommerfest");
        assertThat(v.paketName()).isEqualTo("Gold");
        assertThat(v.region()).isEqualTo("Zürich");
    }

    /** VIEW-ENG-02: Kontakt-Anfrage (Verein → Marke, kein Paket) — Rollen per Typ, Projekt-Felder null. */
    @Test
    @DisplayName("VIEW-ENG-02: Kontakt-Anfrage Verein→Marke — Rollen per Org-Typ, kein NPE")
    void kontaktAnfrageVereinAnMarke() {
        Organisation verein = org("MindBalance", "mindbalance", OrgTyp.VEREIN, Branche.MENTAL_HEALTH);
        Organisation marke = org("CSS", "css", OrgTyp.UNTERNEHMEN, null);

        SponsoringAnfrage a = new SponsoringAnfrage();
        a.setId(UUID.randomUUID());
        a.setAnfragenderOrg(verein);   // Verein kontaktiert die Marke
        a.setEmpfaengerOrg(marke);
        a.setStatus(AnfrageStatus.ANGENOMMEN);
        // kein Paket

        EngagementView v = EngagementView.von(a);

        assertThat(v.sponsorName()).isEqualTo("CSS");        // per Org-Typ aufgelöst
        assertThat(v.vereinName()).isEqualTo("MindBalance");
        assertThat(v.vereinBranche()).isEqualTo(Branche.MENTAL_HEALTH);
        assertThat(v.projektName()).isNull();
        assertThat(v.projektSlug()).isNull();
        assertThat(v.paketName()).isNull();
        assertThat(v.region()).isNull();
    }

    /** VIEW-ENG-03: Verein-Logo-URL wird durchgereicht; ohne Logo bleibt sie null. */
    @Test
    @DisplayName("VIEW-ENG-03: Verein-Logo-URL durchgereicht / null ohne Logo")
    void vereinLogo() {
        Organisation marke = org("CSS", "css", OrgTyp.UNTERNEHMEN, null);
        Organisation verein = org("FC Beispiel", "fc-beispiel", OrgTyp.VEREIN, Branche.SPORT);
        SponsoringAnfrage a = new SponsoringAnfrage();
        a.setId(UUID.randomUUID());
        a.setAnfragenderOrg(marke);
        a.setEmpfaengerOrg(verein);
        a.setStatus(AnfrageStatus.ANGENOMMEN);

        assertThat(EngagementView.von(a, "/medien/logo-9").vereinLogoUrl()).isEqualTo("/medien/logo-9");
        assertThat(EngagementView.von(a).vereinLogoUrl()).isNull();
    }

    /** VIEW-ENG-04: Kanton wird aus der Verein-PLZ abgeleitet (null ohne/ungültige PLZ). */
    @Test
    @DisplayName("VIEW-ENG-04: Kanton aus Verein-PLZ abgeleitet")
    void kantonAusPlz() {
        Organisation marke = org("CSS", "css", OrgTyp.UNTERNEHMEN, null);
        Organisation verein = org("FC Beispiel", "fc-beispiel", OrgTyp.VEREIN, Branche.SPORT);
        verein.setPostleitzahl("8001");   // Zürich
        SponsoringAnfrage a = new SponsoringAnfrage();
        a.setId(UUID.randomUUID());
        a.setAnfragenderOrg(marke);
        a.setEmpfaengerOrg(verein);
        a.setStatus(AnfrageStatus.ANGENOMMEN);

        assertThat(EngagementView.von(a).kanton()).isEqualTo(ch.sponsorplatz.organisation.Kanton.ZH);

        verein.setPostleitzahl(null);
        assertThat(EngagementView.von(a).kanton()).isNull();
    }

    /** VIEW-ENG-05: ohne Verein-PLZ wird der Kanton aus dem Projekt-Ort abgeleitet. */
    @Test
    @DisplayName("VIEW-ENG-05: Kanton-Fallback aus Projekt-Ort, wenn keine Verein-PLZ")
    void kantonFallbackAusProjektOrt() {
        Organisation marke = org("CSS", "css", OrgTyp.UNTERNEHMEN, null);
        Organisation verein = org("FC Beispiel", "fc-beispiel", OrgTyp.VEREIN, Branche.SPORT);
        // Verein ohne PLZ + ohne Ort
        Projekt projekt = new Projekt();
        projekt.setName("Sommerfest");
        projekt.setSlug("sommerfest");
        projekt.setOrt("Bern");
        projekt.setOrg(verein);
        SponsoringPaket paket = new SponsoringPaket();
        paket.setName("Gold");
        paket.setProjekt(projekt);

        SponsoringAnfrage a = new SponsoringAnfrage();
        a.setId(UUID.randomUUID());
        a.setAnfragenderOrg(marke);
        a.setEmpfaengerOrg(verein);
        a.setPaket(paket);
        a.setStatus(AnfrageStatus.ANGENOMMEN);

        assertThat(EngagementView.von(a).kanton()).isEqualTo(ch.sponsorplatz.organisation.Kanton.BE);
    }

    private Organisation org(String name, String slug, OrgTyp typ, Branche branche) {
        Organisation o = new Organisation();
        o.setId(UUID.randomUUID());
        o.setName(name);
        o.setSlug(slug);
        o.setTyp(typ);
        o.setBranche(branche);
        return o;
    }
}
