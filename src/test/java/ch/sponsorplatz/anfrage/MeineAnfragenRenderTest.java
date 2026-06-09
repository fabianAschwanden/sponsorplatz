package ch.sponsorplatz.anfrage;

import ch.sponsorplatz.benutzer.AppUserService;
import ch.sponsorplatz.organisation.MitgliedschaftService;
import ch.sponsorplatz.organisation.OrgTyp;
import ch.sponsorplatz.organisation.Organisation;
import ch.sponsorplatz.projekt.SponsoringPaket;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Voll-Kontext-Render-Test der /anfragen-Seite (echtes Thymeleaf-Rendering).
 *
 * <p>Regression: nach der neu/erledigt-Umstrukturierung konnte die Seite für
 * bestimmte Benutzer (Admin / Org-Bearbeiter ohne Vereins-Edit-Rolle) nicht mehr
 * geöffnet werden — der {@code @WebMvcTest} prüft nur den View-Namen, nicht das
 * Rendering. Dieser Test rendert das Template wirklich.
 *
 * Test-IDs: MANF-R01..R03.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
class MeineAnfragenRenderTest {

    @Autowired private MockMvc mockMvc;

    @MockitoBean private SponsoringAnfrageService anfrageService;
    @MockitoBean private AppUserService appUserService;
    @MockitoBean private MitgliedschaftService mitgliedschaftService;

    private static final UUID USER_ID = UUID.randomUUID();

    private void stubUser(boolean istVereinsMitglied) {
        when(appUserService.findeIdNachEmail(any())).thenReturn(USER_ID);
        List<UUID> vereinsOrgIds = istVereinsMitglied ? List.of(UUID.randomUUID()) : List.of();
        when(mitgliedschaftService.findeAnfragenSeitenDaten(USER_ID))
                .thenReturn(new MitgliedschaftService.AnfragenSeitenDaten(
                        List.of(UUID.randomUUID()), vereinsOrgIds,
                        istVereinsMitglied ? List.of("FC Test") : List.of()));
    }

    /** MANF-R01: Admin/Nicht-Vereinsmitglied mit gemischten eingehenden Anfragen → 200. */
    @Test
    @WithUserDetails("dev@sponsorplatz.ch")
    @DisplayName("MANF-R01: /anfragen rendert für Nicht-Vereinsmitglied mit neuen + erledigten Anfragen")
    void rendertFuerNichtVereinsmitglied() throws Exception {
        stubUser(false);
        when(anfrageService.findeAlleEingehendenViews(any())).thenReturn(List.of(
                paketAnfrage(AnfrageStatus.NEU),
                paketAnfrage(AnfrageStatus.ANGENOMMEN),
                kontaktAnfrage(AnfrageStatus.ABGELEHNT)));
        when(anfrageService.zaehleNeue(any(java.util.Collection.class))).thenReturn(1L);

        mockMvc.perform(get("/anfragen")).andExpect(status().isOk());
    }

    /** MANF-R02: Vereinsmitglied mit erledigten ausgehenden Anfragen (Fragment-Pfad) → 200. */
    @Test
    @WithUserDetails("dev@sponsorplatz.ch")
    @DisplayName("MANF-R02: /anfragen rendert erledigte ausgehende Anfragen (Fragment)")
    void rendertErledigteAusgehende() throws Exception {
        stubUser(true);
        when(anfrageService.findeAlleEingehendenViews(any())).thenReturn(List.of());
        when(anfrageService.zaehleNeue(any(java.util.Collection.class))).thenReturn(0L);
        when(anfrageService.findeAusgehendeVonUserViews(USER_ID)).thenReturn(List.of(
                kontaktAnfrage(AnfrageStatus.ANGENOMMEN), kontaktAnfrage(AnfrageStatus.NEU)));
        when(anfrageService.findeAusgehendeMeinerOrgsOhneUserViews(any(), any())).thenReturn(List.of(
                kontaktAnfrage(AnfrageStatus.ABGELEHNT)));

        mockMvc.perform(get("/anfragen")).andExpect(status().isOk());
    }

    /** MANF-R03: leere Seite (kein Datensatz) → 200. */
    @Test
    @WithUserDetails("dev@sponsorplatz.ch")
    @DisplayName("MANF-R03: /anfragen rendert ohne Daten")
    void rendertLeer() throws Exception {
        stubUser(false);
        when(anfrageService.findeAlleEingehendenViews(any())).thenReturn(List.of());
        when(anfrageService.zaehleNeue(any(java.util.Collection.class))).thenReturn(0L);

        mockMvc.perform(get("/anfragen")).andExpect(status().isOk());
    }

    private static AnfrageView paketAnfrage(AnfrageStatus status) {
        Organisation verein = org("FC Test", "fc-test", OrgTyp.VEREIN);
        Organisation sponsor = org("Sponsor AG", "sponsor-ag", OrgTyp.UNTERNEHMEN);
        SponsoringPaket paket = new SponsoringPaket();
        paket.setId(UUID.randomUUID());
        paket.setName("Gold");
        paket.setPreisChf(new java.math.BigDecimal("5000"));

        SponsoringAnfrage a = new SponsoringAnfrage();
        a.setId(UUID.randomUUID());
        a.setStatus(status);
        a.setNachricht("Nachricht");
        a.setKontaktName("Max Muster");
        a.setKontaktEmail("max@firma.ch");
        a.setPaket(paket);
        a.setAnfragenderOrg(sponsor);
        a.setEmpfaengerOrg(verein);
        if (status != AnfrageStatus.NEU) a.setAntwort("Eine Antwort.");
        return AnfrageView.von(a);
    }

    private static AnfrageView kontaktAnfrage(AnfrageStatus status) {
        Organisation verein = org("FC Test", "fc-test", OrgTyp.VEREIN);
        Organisation sponsor = org("Sponsor AG", "sponsor-ag", OrgTyp.UNTERNEHMEN);

        SponsoringAnfrage a = new SponsoringAnfrage();
        a.setId(UUID.randomUUID());
        a.setStatus(status);
        a.setNachricht("Kontakt-Nachricht");
        a.setBetreff("Sommerfest");
        a.setKontaktName("Anna Beispiel");
        a.setWunschBetragChf(new java.math.BigDecimal("3000"));
        a.setAnfragenderOrg(verein);   // Kontakt-Anfrage: Verein fragt Sponsor an
        a.setEmpfaengerOrg(sponsor);
        if (status != AnfrageStatus.NEU) a.setAntwort("Eine Antwort.");
        return AnfrageView.von(a);
    }

    private static Organisation org(String name, String slug, OrgTyp typ) {
        Organisation o = new Organisation();
        o.setId(UUID.randomUUID());
        o.setName(name);
        o.setSlug(slug);
        o.setTyp(typ);
        return o;
    }
}
