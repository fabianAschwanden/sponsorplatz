package ch.sponsorplatz.anfrage;

import ch.sponsorplatz.benutzer.AppUser;
import ch.sponsorplatz.benutzer.AppUserRepository;
import ch.sponsorplatz.organisation.AccessControl;
import ch.sponsorplatz.organisation.Mitgliedschaft;
import ch.sponsorplatz.organisation.MitgliedschaftRepository;
import ch.sponsorplatz.organisation.OrgTyp;
import ch.sponsorplatz.organisation.Organisation;
import ch.sponsorplatz.organisation.OrganisationService;
import ch.sponsorplatz.organisation.Rolle;
import ch.sponsorplatz.projekt.Projekt;
import ch.sponsorplatz.projekt.SponsoringPaket;
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

import java.util.List;
import java.util.Optional;
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
    @MockitoBean private AppUserRepository appUserRepository;
    @MockitoBean private MitgliedschaftRepository mitgliedschaftRepository;
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
        AppUser user = new AppUser();
        user.setId(UUID.randomUUID());
        user.setEmail("test@sp.ch");
        when(appUserRepository.findByEmail("test@sp.ch")).thenReturn(Optional.of(user));

        UUID orgId = UUID.randomUUID();
        when(mitgliedschaftRepository.findOrgIdsByUserId(user.getId())).thenReturn(List.of(orgId));
        when(anfrageService.findeAlleEingehenden(any())).thenReturn(List.of());
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
        AppUser user = new AppUser();
        user.setId(UUID.randomUUID());
        user.setEmail("test@sp.ch");
        when(appUserRepository.findByEmail("test@sp.ch")).thenReturn(Optional.of(user));

        UUID orgId = UUID.randomUUID();
        when(mitgliedschaftRepository.findOrgIdsByUserId(user.getId())).thenReturn(List.of(orgId));
        when(anfrageService.findeAlleEingehenden(any())).thenReturn(List.of());
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

        AppUser user = neuerUser(userId, "sponsor@sp.ch", "Sponsorin");
        Organisation empfaenger = neueOrg(empfaengerId, "FC Verein", "fc-verein", OrgTyp.VEREIN);
        Organisation sponsorOrg = neueOrg(sponsorOrgId, "ACME AG", "acme", OrgTyp.UNTERNEHMEN);
        SponsoringPaket paket = neuesPaket(paketId, empfaenger);

        when(appUserRepository.findByEmail("sponsor@sp.ch")).thenReturn(Optional.of(user));
        when(paketService.findeNachIdMitProjektUndOrg(paketId)).thenReturn(Optional.of(paket));
        when(mitgliedschaftRepository.findByUserIdAndRolleInMitOrg(eq(userId), any()))
                .thenReturn(List.of(neueMitgliedschaft(user, sponsorOrg, Rolle.ORG_OWNER)));

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

        AppUser user = neuerUser(userId, "sponsor@sp.ch", "Sponsorin");
        Organisation empfaenger = neueOrg(empfaengerId, "FC Verein", "fc-verein", OrgTyp.VEREIN);
        Organisation sponsorOrg = neueOrg(sponsorOrgId, "ACME AG", "acme", OrgTyp.UNTERNEHMEN);
        SponsoringPaket paket = neuesPaket(paketId, empfaenger);

        when(appUserRepository.findByEmail("sponsor@sp.ch")).thenReturn(Optional.of(user));
        when(paketService.findeNachIdMitProjektUndOrg(paketId)).thenReturn(Optional.of(paket));
        when(accessControl.kannOrgEditieren(eq(sponsorOrgId), any())).thenReturn(true);
        when(organisationService.findeNachId(sponsorOrgId)).thenReturn(Optional.of(sponsorOrg));

        mockMvc.perform(post("/anfragen/erstellen")
                        .param("paketId", paketId.toString())
                        .param("anfragenderOrgId", sponsorOrgId.toString())
                        .param("nachricht", "Wir haben grosses Interesse am Sommerfest 2026.")
                        .param("kontaktName", "Sponsorin")
                        .param("kontaktEmail", "sponsor@sp.ch")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/anfragen"));

        verify(anfrageService).erstelle(eq(paket), eq(sponsorOrg), eq(empfaenger),
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

        AppUser user = neuerUser(userId, "sponsor@sp.ch", "Sponsorin");
        Organisation empfaenger = neueOrg(empfaengerId, "FC Verein", "fc-verein", OrgTyp.VEREIN);
        SponsoringPaket paket = neuesPaket(paketId, empfaenger);

        when(appUserRepository.findByEmail("sponsor@sp.ch")).thenReturn(Optional.of(user));
        when(paketService.findeNachIdMitProjektUndOrg(paketId)).thenReturn(Optional.of(paket));
        when(accessControl.kannOrgEditieren(eq(empfaengerId), any())).thenReturn(true);
        when(mitgliedschaftRepository.findByUserIdAndRolleInMitOrg(eq(userId), any()))
                .thenReturn(List.of(neueMitgliedschaft(user, empfaenger, Rolle.ORG_OWNER)));

        mockMvc.perform(post("/anfragen/erstellen")
                        .param("paketId", paketId.toString())
                        .param("anfragenderOrgId", empfaengerId.toString())
                        .param("nachricht", "Wir wollen uns selbst sponsern.")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(view().name("anfrage-neu"));

        verify(anfrageService, never()).erstelle(any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    @WithMockUser("editor@verein.ch")
    @DisplayName("MANF-08: /anfragen splittet ausgehende in meine vs. Org-ohne-mich")
    void splittetAusgehendeInZweiBuckets() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID vereinsOrgId = UUID.randomUUID();

        AppUser user = neuerUser(userId, "editor@verein.ch", "Editor");
        Organisation vereinsOrg = neueOrg(vereinsOrgId, "FC Test", "fc-test", OrgTyp.VEREIN);
        when(appUserRepository.findByEmail("editor@verein.ch")).thenReturn(Optional.of(user));
        when(mitgliedschaftRepository.findByUserIdAndRolleInMitOrg(eq(userId), any()))
                .thenReturn(List.of(neueMitgliedschaft(user, vereinsOrg, Rolle.ORG_EDITOR)));

        SponsoringAnfrage meineAnfrage = new SponsoringAnfrage();
        meineAnfrage.setId(UUID.randomUUID());
        meineAnfrage.setAnfragenderOrg(vereinsOrg);
        meineAnfrage.setEmpfaengerOrg(vereinsOrg);
        meineAnfrage.setStatus(AnfrageStatus.NEU);

        SponsoringAnfrage orgAnfrage = new SponsoringAnfrage();
        orgAnfrage.setId(UUID.randomUUID());
        orgAnfrage.setAnfragenderOrg(vereinsOrg);
        orgAnfrage.setEmpfaengerOrg(vereinsOrg);
        orgAnfrage.setStatus(AnfrageStatus.NEU);

        when(anfrageService.findeAusgehendeVonUser(userId)).thenReturn(List.of(meineAnfrage));
        when(anfrageService.findeAusgehendeMeinerOrgsOhneUser(eq(List.of(vereinsOrgId)), eq(userId)))
                .thenReturn(List.of(orgAnfrage));

        mockMvc.perform(get("/anfragen"))
                .andExpect(status().isOk())
                .andExpect(model().attributeExists("meineAusgehendeAnfragen", "orgAusgehendeAnfragen", "meineOrgNamen"))
                .andExpect(model().attribute("kannKontaktanfrageStellen", true));

        verify(anfrageService).findeAusgehendeVonUser(userId);
        verify(anfrageService).findeAusgehendeMeinerOrgsOhneUser(List.of(vereinsOrgId), userId);
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

    private AppUser neuerUser(UUID id, String email, String anzeigename) {
        AppUser u = new AppUser();
        u.setId(id);
        u.setEmail(email);
        u.setAnzeigename(anzeigename);
        return u;
    }

    private Organisation neueOrg(UUID id, String name, String slug, OrgTyp typ) {
        Organisation o = new Organisation();
        o.setId(id);
        o.setName(name);
        o.setSlug(slug);
        o.setTyp(typ);
        return o;
    }

    private SponsoringPaket neuesPaket(UUID id, Organisation empfaengerOrg) {
        Projekt projekt = new Projekt();
        projekt.setId(UUID.randomUUID());
        projekt.setName("Sommerfest 2026");
        projekt.setSlug("sommerfest-2026");
        projekt.setOrg(empfaengerOrg);

        SponsoringPaket p = new SponsoringPaket();
        p.setId(id);
        p.setName("Gold");
        p.setProjekt(projekt);
        return p;
    }

    private Mitgliedschaft neueMitgliedschaft(AppUser user, Organisation org, Rolle rolle) {
        Mitgliedschaft m = new Mitgliedschaft();
        m.setUser(user);
        m.setOrg(org);
        m.setRolle(rolle);
        return m;
    }
}
