package ch.sponsorplatz.anfrage;

import ch.sponsorplatz.organisation.Branche;
import ch.sponsorplatz.organisation.Organisation;
import ch.sponsorplatz.projekt.Projekt;
import ch.sponsorplatz.projekt.SponsoringPaket;
import ch.sponsorplatz.shared.config.SecurityConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = EngagementController.class)
@Import(SecurityConfig.class)
@ActiveProfiles("dev")
@org.springframework.security.test.context.support.WithMockUser
class EngagementControllerTest {

    @Autowired private MockMvc mockMvc;
    @MockitoBean private EngagementService engagementService;

    @Test
    @DisplayName("ENG-01-CTRL: GET /marken/{slug}/engagements → 200 + engagement-schaufenster View")
    void schaufensterRendert() throws Exception {
        SponsoringAnfrage anfrage = erstelleAnfrage();
        when(engagementService.findeNachSponsorSlug("css-versicherung"))
                .thenReturn(List.of(anfrage));

        mockMvc.perform(get("/marken/css-versicherung/engagements"))
                .andExpect(status().isOk())
                .andExpect(view().name("engagement-schaufenster"))
                .andExpect(model().attributeExists("engagements", "sponsorSlug", "alleBranchen"));
    }

    @Test
    @DisplayName("ENG-02-CTRL: Region-Filter wird an Service delegiert")
    void regionFilter() throws Exception {
        when(engagementService.findeNachSponsorSlugUndRegion("css-versicherung", "Zürich"))
                .thenReturn(List.of());

        mockMvc.perform(get("/marken/css-versicherung/engagements").param("region", "Zürich"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("filterRegion", "Zürich"));
    }

    @Test
    @DisplayName("ENG-03-CTRL: Branche-Filter wird an Service delegiert")
    void brancheFilter() throws Exception {
        when(engagementService.findeNachSponsorSlugUndBranche("css-versicherung", Branche.SPORT))
                .thenReturn(List.of());

        mockMvc.perform(get("/marken/css-versicherung/engagements").param("branche", "SPORT"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("filterBranche", Branche.SPORT));
    }

    private SponsoringAnfrage erstelleAnfrage() {
        Organisation sponsor = new Organisation();
        sponsor.setId(UUID.randomUUID());
        sponsor.setName("CSS Versicherung");
        sponsor.setSlug("css-versicherung");
        sponsor.setBranche(Branche.PRAEVENTION);

        Organisation verein = new Organisation();
        verein.setId(UUID.randomUUID());
        verein.setName("FC Beispiel");
        verein.setSlug("fc-beispiel");
        verein.setBranche(Branche.SPORT);

        Projekt p = new Projekt();
        p.setName("Sommerfest");
        p.setSlug("sommerfest");
        p.setOrt("Zürich");
        p.setOrg(verein);

        SponsoringPaket paket = new SponsoringPaket();
        paket.setName("Gold");
        paket.setProjekt(p);

        SponsoringAnfrage a = new SponsoringAnfrage();
        a.setId(UUID.randomUUID());
        a.setAnfragenderOrg(sponsor);
        a.setEmpfaengerOrg(verein);
        a.setPaket(paket);
        a.setStatus(AnfrageStatus.ANGENOMMEN);
        a.setBeantwortetAm(Instant.now());
        return a;
    }
}

