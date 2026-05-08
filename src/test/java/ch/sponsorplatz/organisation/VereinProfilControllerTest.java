package ch.sponsorplatz.organisation;
import ch.sponsorplatz.shared.exception.GlobalExceptionHandler;

import ch.sponsorplatz.shared.config.SecurityConfig;
import ch.sponsorplatz.projekt.ProjektService;
import ch.sponsorplatz.benutzer.SponsorplatzUserDetailsService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = VereinProfilController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class})
@ActiveProfiles("dev")
class VereinProfilControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private OrganisationService orgService;

    @MockBean
    private ProjektService projektService;

    @MockBean
    private SponsorplatzUserDetailsService userDetailsService;

    /** VP-01: Vereinsprofil ist öffentlich erreichbar. */
    @Test
    void profilIstPublic() throws Exception {
        Organisation org = new Organisation();
        org.setId(UUID.randomUUID());
        org.setName("FC Muster");
        org.setSlug("fc-muster");
        org.setTyp(OrgTyp.VEREIN);

        when(orgService.findeNachSlug("fc-muster")).thenReturn(Optional.of(org));
        when(projektService.findeNachOrg(org.getId())).thenReturn(List.of());

        mockMvc.perform(get("/vereine/fc-muster"))
                .andExpect(status().isOk())
                .andExpect(view().name("verein-profil"))
                .andExpect(model().attributeExists("org", "projekte"));
    }

    /** VP-02: Unbekannter Verein → 404. */
    @Test
    void unbekannterVereinIst404() throws Exception {
        when(orgService.findeNachSlug("gibts-nicht")).thenReturn(Optional.empty());

        mockMvc.perform(get("/vereine/gibts-nicht"))
                .andExpect(status().isNotFound());
    }
}

