package ch.sponsorplatz.crm;

import ch.sponsorplatz.organisation.OrganisationService;
import ch.sponsorplatz.shared.config.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * CRM-CTRL-01..04 — Web-Schicht-Schutz des SponsorAccountController. Der Service
 * setzt die Mandanten-Isolation durch (AccessDeniedException); der Controller
 * darf sie nicht verschlucken → muss als 403 durchschlagen.
 */
@WebMvcTest(controllers = SponsorAccountController.class)
@Import(SecurityConfig.class)
@ActiveProfiles("dev")
class SponsorAccountControllerTest {

    @Autowired private MockMvc mockMvc;

    @MockitoBean private SponsorAccountService accountService;
    @MockitoBean private KontaktPersonService kontaktService;
    @MockitoBean private AktivitaetService aktivitaetService;
    @MockitoBean private RenewalService renewalService;
    @MockitoBean private OrganisationService organisationService;
    @MockitoBean private ch.sponsorplatz.benutzer.SponsorplatzUserDetailsService userDetailsService;

    private final UUID sponsorOrgId = UUID.randomUUID();

    /** CRM-CTRL-01: Service wirft AccessDenied → Controller schlägt als 403 durch. */
    @Test
    @WithMockUser
    void portfolioFremderSponsor403() throws Exception {
        when(organisationService.findeIdNachSlug("css")).thenReturn(sponsorOrgId);
        when(accountService.findePortfolio(eq(sponsorOrgId), any()))
                .thenThrow(new AccessDeniedException("kein Zugriff"));

        mockMvc.perform(get("/crm/css"))
                .andExpect(status().isForbidden());
    }

    /** CRM-CTRL-02: erstelle ohne CSRF-Token → 403 (CSRF-Schutz greift). */
    @Test
    @WithMockUser
    void erstelleOhneCsrfWird403() throws Exception {
        mockMvc.perform(post("/crm/css").param("vereinOrgId", UUID.randomUUID().toString()))
                .andExpect(status().isForbidden());
    }

    /** CRM-CTRL-03: erstelle mit CSRF + Zugriff → Redirect zurück zum Portfolio. */
    @Test
    @WithMockUser
    void erstelleMitZugriffRedirect() throws Exception {
        UUID vereinId = UUID.randomUUID();
        when(organisationService.findeIdNachSlug("css")).thenReturn(sponsorOrgId);
        when(accountService.erstelle(eq(sponsorOrgId), eq(vereinId), any())).thenReturn(null);

        mockMvc.perform(post("/crm/css").with(csrf()).param("vereinOrgId", vereinId.toString()))
                .andExpect(status().is3xxRedirection());
    }
}
