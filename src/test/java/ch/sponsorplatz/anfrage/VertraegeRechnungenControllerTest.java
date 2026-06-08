package ch.sponsorplatz.anfrage;

import ch.sponsorplatz.benutzer.AppUserService;
import ch.sponsorplatz.organisation.MitgliedschaftService;
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
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

/**
 * Tests für {@link VertraegeRechnungenController}.
 * Test-IDs: VRUEB-01..03.
 */
@WebMvcTest(VertraegeRechnungenController.class)
@Import(SecurityConfig.class)
@ActiveProfiles("dev")
class VertraegeRechnungenControllerTest {

    @Autowired private MockMvc mockMvc;

    @MockitoBean private VertragService vertragService;
    @MockitoBean private RechnungService rechnungService;
    @MockitoBean private AppUserService appUserService;
    @MockitoBean private MitgliedschaftService mitgliedschaftService;
    @MockitoBean private ch.sponsorplatz.benutzer.SponsorplatzUserDetailsService userDetailsService;

    private static final UUID USER_ID = UUID.randomUUID();

    @Test
    @WithMockUser
    @DisplayName("VRUEB-01: GET /vertraege-rechnungen rendert Übersicht mit beiden Listen")
    void uebersichtRendert() throws Exception {
        when(appUserService.findeIdNachEmail(any())).thenReturn(USER_ID);
        when(mitgliedschaftService.findeOrgIdsVonUserMitRollen(any(), any()))
                .thenReturn(List.of(UUID.randomUUID()));
        when(vertragService.findeViewsNachOrgs(anyCollection())).thenReturn(List.of(vertragView()));
        when(rechnungService.findeViewsNachOrgs(anyCollection())).thenReturn(List.of(rechnungView()));

        mockMvc.perform(get("/vertraege-rechnungen"))
                .andExpect(status().isOk())
                .andExpect(view().name("anfrage/vertraege-rechnungen"))
                .andExpect(model().attributeExists("vertraege", "rechnungen", "vListe", "rListe"));
    }

    @Test
    @WithMockUser
    @DisplayName("VRUEB-04: Suche + Sortierung werden auf die Verträge-Liste angewandt")
    void filterUndSortierung() throws Exception {
        when(appUserService.findeIdNachEmail(any())).thenReturn(USER_ID);
        when(mitgliedschaftService.findeOrgIdsVonUserMitRollen(any(), any()))
                .thenReturn(List.of(UUID.randomUUID()));
        when(vertragService.findeViewsNachOrgs(anyCollection()))
                .thenReturn(List.of(vertragView("Alpha AG"), vertragView("Beta AG")));
        when(rechnungService.findeViewsNachOrgs(anyCollection())).thenReturn(List.of());

        mockMvc.perform(get("/vertraege-rechnungen")
                        .param("vSuche", "alpha").param("vSort", "partner").param("vDir", "asc"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("vAnzahlGezeigt", 1))
                .andExpect(model().attribute("vAnzahlGesamt", 2))
                .andExpect(model().attribute("vFilterAktiv", true));
    }

    @Test
    @WithMockUser
    @DisplayName("VRUEB-05: Default-Sortierung — aktive Verträge (ENTWURF/UNTERZEICHNET) vor GEKÜNDIGT")
    void defaultSortierungAktiveZuerst() throws Exception {
        when(appUserService.findeIdNachEmail(any())).thenReturn(USER_ID);
        when(mitgliedschaftService.findeOrgIdsVonUserMitRollen(any(), any()))
                .thenReturn(List.of(UUID.randomUUID()));
        // Reihenfolge wie aus der DB (erstelltAm DESC): gekündigt zuerst, dann aktiv.
        when(vertragService.findeViewsNachOrgs(anyCollection())).thenReturn(List.of(
                vertragView("Gekündigt AG", VertragsStatus.GEKUENDIGT),
                vertragView("Aktiv AG", VertragsStatus.UNTERZEICHNET)));
        when(rechnungService.findeViewsNachOrgs(anyCollection())).thenReturn(List.of());

        var ergebnis = mockMvc.perform(get("/vertraege-rechnungen"))
                .andExpect(status().isOk())
                .andReturn();

        @SuppressWarnings("unchecked")
        var liste = (ch.sponsorplatz.shared.util.ListenSeite<VertragView>)
                ergebnis.getModelAndView().getModel().get("vListe");
        // Default-Sort hebt den aktiven Vertrag trotz älteren Datums nach oben.
        org.assertj.core.api.Assertions.assertThat(liste.inhalt().get(0).status())
                .isEqualTo(VertragsStatus.UNTERZEICHNET);
        org.assertj.core.api.Assertions.assertThat(liste.inhalt().get(1).status())
                .isEqualTo(VertragsStatus.GEKUENDIGT);
    }

    @Test
    @WithMockUser
    @DisplayName("VRUEB-02: ohne Edit-Org-Mitgliedschaft → leere Listen, kein Fehler")
    void leereListenOhneOrgs() throws Exception {
        when(appUserService.findeIdNachEmail(any())).thenReturn(USER_ID);
        when(mitgliedschaftService.findeOrgIdsVonUserMitRollen(any(), any())).thenReturn(List.of());
        when(vertragService.findeViewsNachOrgs(anyCollection())).thenReturn(List.of());
        when(rechnungService.findeViewsNachOrgs(anyCollection())).thenReturn(List.of());

        mockMvc.perform(get("/vertraege-rechnungen"))
                .andExpect(status().isOk())
                .andExpect(view().name("anfrage/vertraege-rechnungen"));
    }

    @Test
    @DisplayName("VRUEB-03: unauthentifiziert → Redirect auf Login")
    void unauthRedirect() throws Exception {
        mockMvc.perform(get("/vertraege-rechnungen"))
                .andExpect(status().is3xxRedirection());
    }

    private VertragView vertragView() {
        return vertragView("Sponsor");
    }

    private VertragView vertragView(String sponsorName) {
        return vertragView(sponsorName, VertragsStatus.UNTERZEICHNET);
    }

    private VertragView vertragView(String sponsorName, VertragsStatus status) {
        return new VertragView(
                UUID.randomUUID(), UUID.randomUUID(), status,
                "FC Test", "fc-test", sponsorName, "s@t.ch", "Sponsor AG",
                "Gold", "Beschreibung", BigDecimal.valueOf(5000),
                LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31),
                "Logo", "5000 CHF", Instant.now(), "admin@t.ch", Instant.now(), "owner@t.ch",
                true, "vertrag.pdf", 1234L, Instant.now(), "admin@t.ch");
    }

    private RechnungView rechnungView() {
        return new RechnungView(
                UUID.randomUUID(), UUID.randomUUID(), "FC Test", "fc-test",
                "R-2026-00001", RechnungsStatus.OFFEN, BigDecimal.valueOf(2500),
                "CH93 0076 2011 6238 5295 7", "210000000003139471430009017",
                "CSS Versicherung", "finance@css.ch", "Luzern",
                "Sponsoring", Instant.now(), "admin@t.ch",
                LocalDate.of(2026, 7, 15), null, null);
    }
}
