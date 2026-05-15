package ch.sponsorplatz.anfrage;

import ch.sponsorplatz.benutzer.AppUserService;
import ch.sponsorplatz.organisation.AccessControl;
import ch.sponsorplatz.organisation.MitgliedschaftService;
import ch.sponsorplatz.organisation.OrganisationService;
import ch.sponsorplatz.organisation.OrganisationView;
import ch.sponsorplatz.projekt.SponsoringPaketService;
import ch.sponsorplatz.shared.config.SecurityConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Tests für {@link MeineAnfragenController}.
 * Test-IDs: MANF-01..07 in {@code specs/TESTSTRATEGIE.md}.
 */
@WebMvcTest(MeineAnfragenController.class)
@Import(SecurityConfig.class)
@ActiveProfiles("dev")
class MeineAnfragenControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean private SponsoringAnfrageService anfrageService;
    @MockitoBean private AppUserService appUserService;
    @MockitoBean private MitgliedschaftService mitgliedschaftService;
    @MockitoBean private AccessControl accessControl;
    @MockitoBean private SponsoringPaketService paketService;
    @MockitoBean private OrganisationService organisationService;

    // Beans die SecurityConfig braucht
    @MockitoBean private ch.sponsorplatz.benutzer.SponsorplatzUserDetailsService userDetailsService;

    @Test
    @DisplayName("MANF-01: /anfragen ohne Auth → Redirect auf Login")
    void ohneAuthRedirect() throws Exception {
        mockMvc.perform(get("/anfragen"))
                .andExpect(status().is3xxRedirection());
    }

    @Test
    @WithMockUser("test@sp.ch")
    @DisplayName("MANF-02: /anfragen mit Auth → 200 + Template meine-anfragen")
    void mitAuthZeigtListe() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID orgId = UUID.randomUUID();

        when(appUserService.findeIdNachEmail("test@sp.ch")).thenReturn(userId);
        when(mitgliedschaftService.findeAnfragenSeitenDaten(userId))
                .thenReturn(new MitgliedschaftService.AnfragenSeitenDaten(
                        List.of(orgId), List.of(), List.of()));
        when(anfrageService.findeAlleEingehendenViews(any())).thenReturn(List.of());
        when(anfrageService.zaehleNeue(anyCollection())).thenReturn(0L);

        mockMvc.perform(get("/anfragen"))
                .andExpect(status().isOk())
                .andExpect(view().name("meine-anfragen"))
                .andExpect(model().attributeExists("anfragen", "anzahlOffene"));
    }

    @Test
    @WithMockUser("test@sp.ch")
    @DisplayName("MANF-03: /anfragen zeigt offene Zählung korrekt an")
    void zeigtOffeneAnfragen() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID orgId = UUID.randomUUID();

        when(appUserService.findeIdNachEmail("test@sp.ch")).thenReturn(userId);
        when(mitgliedschaftService.findeAnfragenSeitenDaten(userId))
                .thenReturn(new MitgliedschaftService.AnfragenSeitenDaten(
                        List.of(orgId), List.of(), List.of()));
        when(anfrageService.findeAlleEingehendenViews(any())).thenReturn(List.of());
        when(anfrageService.zaehleNeue(anyCollection())).thenReturn(5L);

        mockMvc.perform(get("/anfragen"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("anzahlOffene", 5L));
    }

    @Test
    @WithMockUser("sponsor@sp.ch")
    @DisplayName("MANF-04: GET /anfragen/neu zeigt Form mit Paket-Kontext und meinen Orgs")
    void erstellungsFormularZeigtPaketUndOrgs() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID paketId = UUID.randomUUID();
        UUID empfaengerId = UUID.randomUUID();
        UUID sponsorOrgId = UUID.randomUUID();

        OrganisationView empfaengerView = neueOrgView(empfaengerId, "FC Verein", "fc-verein");
        OrganisationView sponsorView = neueOrgView(sponsorOrgId, "ACME AG", "acme");

        when(appUserService.findeIdNachEmail("sponsor@sp.ch")).thenReturn(userId);
        when(appUserService.findeKontaktSnapshotNachEmail("sponsor@sp.ch"))
                .thenReturn(new AppUserService.KontaktSnapshot(userId, "Sponsorin", "sponsor@sp.ch"));
        when(paketService.findePaketAnfrageInfo(paketId))
                .thenReturn(new SponsoringPaketService.PaketAnfrageInfo(
                        paketId, "Gold", new BigDecimal("500"),
                        "Sommerfest 2026", "sommerfest-2026", empfaengerView));
        when(mitgliedschaftService.findeMeineOrgsAusser(userId, empfaengerId))
                .thenReturn(List.of(sponsorView));

        mockMvc.perform(get("/anfragen/neu").param("paketId", paketId.toString()))
                .andExpect(status().isOk())
                .andExpect(view().name("anfrage-neu"))
                .andExpect(model().attributeExists("anfrageForm", "paketName", "empfaengerOrg", "meineOrgs"))
                .andExpect(model().attribute("paketName", "Gold"));
    }

    @Test
    @WithMockUser("sponsor@sp.ch")
    @DisplayName("MANF-05: POST /anfragen/erstellen — Happy Path → erzeugt Anfrage und redirected")
    void erstellungSpeichertUndRedirected() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID paketId = UUID.randomUUID();
        UUID empfaengerId = UUID.randomUUID();
        UUID sponsorOrgId = UUID.randomUUID();

        OrganisationView empfaengerView = neueOrgView(empfaengerId, "FC Verein", "fc-verein");

        when(appUserService.findeIdNachEmail("sponsor@sp.ch")).thenReturn(userId);
        when(paketService.findePaketAnfrageInfo(paketId))
                .thenReturn(new SponsoringPaketService.PaketAnfrageInfo(
                        paketId, "Gold", new BigDecimal("500"),
                        "Sommerfest 2026", "sommerfest-2026", empfaengerView));
        when(accessControl.kannOrgEditieren(eq(sponsorOrgId), any())).thenReturn(true);

        mockMvc.perform(post("/anfragen/erstellen")
                        .param("paketId", paketId.toString())
                        .param("anfragenderOrgId", sponsorOrgId.toString())
                        .param("nachricht", "Wir haben grosses Interesse am Sommerfest 2026.")
                        .param("kontaktName", "Sponsorin")
                        .param("kontaktEmail", "sponsor@sp.ch")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/anfragen"));

        verify(anfrageService).erstelleNachIds(eq(paketId), eq(sponsorOrgId),
                eq("Wir haben grosses Interesse am Sommerfest 2026."),
                eq("Sponsorin"), eq("sponsor@sp.ch"), eq(userId));
    }

    @Test
    @WithMockUser("sponsor@sp.ch")
    @DisplayName("MANF-06: POST /anfragen/erstellen — Self-Anfrage (anfragenderOrg == empfaengerOrg) wird abgelehnt")
    void selfAnfrageWirdAbgelehnt() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID paketId = UUID.randomUUID();
        UUID empfaengerId = UUID.randomUUID();

        OrganisationView empfaengerView = neueOrgView(empfaengerId, "FC Verein", "fc-verein");

        when(appUserService.findeIdNachEmail("sponsor@sp.ch")).thenReturn(userId);
        when(paketService.findePaketAnfrageInfo(paketId))
                .thenReturn(new SponsoringPaketService.PaketAnfrageInfo(
                        paketId, "Gold", new BigDecimal("500"),
                        "Sommerfest 2026", "sommerfest-2026", empfaengerView));
        when(accessControl.kannOrgEditieren(eq(empfaengerId), any())).thenReturn(true);
        when(mitgliedschaftService.findeMeineOrgsAusser(userId, empfaengerId))
                .thenReturn(List.of(empfaengerView));

        mockMvc.perform(post("/anfragen/erstellen")
                        .param("paketId", paketId.toString())
                        .param("anfragenderOrgId", empfaengerId.toString())
                        .param("nachricht", "Wir wollen uns selbst sponsern.")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(view().name("anfrage-neu"));

        verify(anfrageService, never()).erstelleNachIds(any(), any(), any(), any(), any(), any());
    }

    @Test
    @WithMockUser("editor@verein.ch")
    @DisplayName("MANF-08: /anfragen splittet ausgehende in meine vs. Org-ohne-mich")
    void splittetAusgehendeInZweiBuckets() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID vereinsOrgId = UUID.randomUUID();

        when(appUserService.findeIdNachEmail("editor@verein.ch")).thenReturn(userId);
        when(mitgliedschaftService.findeAnfragenSeitenDaten(userId))
                .thenReturn(new MitgliedschaftService.AnfragenSeitenDaten(
                        List.of(vereinsOrgId), List.of(vereinsOrgId), List.of("FC Test")));
        when(anfrageService.findeAlleEingehendenViews(any())).thenReturn(List.of());
        when(anfrageService.findeAusgehendeVonUserViews(userId)).thenReturn(List.of());
        when(anfrageService.findeAusgehendeMeinerOrgsOhneUserViews(eq(List.of(vereinsOrgId)), eq(userId)))
                .thenReturn(List.of());

        mockMvc.perform(get("/anfragen"))
                .andExpect(status().isOk())
                .andExpect(model().attributeExists("meineAusgehendeAnfragen", "orgAusgehendeAnfragen", "meineOrgNamen"))
                .andExpect(model().attribute("kannKontaktanfrageStellen", true));

        verify(anfrageService).findeAusgehendeVonUserViews(userId);
        verify(anfrageService).findeAusgehendeMeinerOrgsOhneUserViews(List.of(vereinsOrgId), userId);
    }

    @Test
    @WithMockUser("angreifer@sp.ch")
    @DisplayName("MANF-07: POST /anfragen/{id}/annehmen ohne Edit-Recht auf Empfänger-Org → 403 (IDOR-Schutz)")
    void idorSchutzBeimAnnehmen() throws Exception {
        UUID anfrageId = UUID.randomUUID();
        UUID empfaengerOrgId = UUID.randomUUID();

        when(anfrageService.findeEmpfaengerOrgId(anfrageId)).thenReturn(empfaengerOrgId);
        when(accessControl.kannOrgEditieren(eq(empfaengerOrgId), any())).thenReturn(false);

        mockMvc.perform(post("/anfragen/{id}/annehmen", anfrageId).with(csrf()))
                .andExpect(status().isForbidden());

        verify(anfrageService, never()).annehme(any(), any());
    }

    // -------- Helpers --------

    private OrganisationView neueOrgView(UUID id, String name, String slug) {
        return new OrganisationView(id, name, slug,
                null, null, null, null, null,
                null, null, null, null,
                null, null, null);
    }
}
