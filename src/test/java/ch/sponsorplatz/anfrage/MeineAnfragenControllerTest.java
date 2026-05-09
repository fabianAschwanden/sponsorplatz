package ch.sponsorplatz.anfrage;

import ch.sponsorplatz.benutzer.AppUser;
import ch.sponsorplatz.benutzer.AppUserRepository;
import ch.sponsorplatz.organisation.MitgliedschaftRepository;
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
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Tests für {@link MeineAnfragenController}.
 * Test-IDs: MANF-01..03 in {@code specs/TESTSTRATEGIE.md}.
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
        when(anfrageService.zaehleNeue(any(java.util.Collection.class))).thenReturn(0L);

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
        when(anfrageService.zaehleNeue(any(java.util.Collection.class))).thenReturn(5L);

        mockMvc.perform(get("/anfragen"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("anzahlOffene", 5L));
    }
}

