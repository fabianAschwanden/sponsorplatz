package ch.sponsorplatz.controller;

import ch.sponsorplatz.config.SecurityConfig;
import ch.sponsorplatz.model.AppUser;
import ch.sponsorplatz.model.Projekt;
import ch.sponsorplatz.model.WatchlistEintrag;
import ch.sponsorplatz.repository.AppUserRepository;
import ch.sponsorplatz.service.ProjektService;
import ch.sponsorplatz.service.SponsorplatzUserDetailsService;
import ch.sponsorplatz.service.WatchlistService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = WatchlistController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class})
@ActiveProfiles("dev")
class WatchlistControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private WatchlistService watchlistService;

    @MockBean
    private ProjektService projektService;

    @MockBean
    private AppUserRepository appUserRepository;

    @MockBean
    private SponsorplatzUserDetailsService userDetailsService;

    private AppUser testUser() {
        AppUser user = new AppUser();
        user.setId(UUID.randomUUID());
        user.setEmail("test@example.com");
        return user;
    }

    /** WLCTRL-01: Watchlist-Seite erreichbar für eingeloggte User. */
    @Test
    @WithMockUser(username = "test@example.com")
    void listeIst200() throws Exception {
        AppUser user = testUser();
        when(appUserRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));
        when(watchlistService.findeNachUser(user.getId())).thenReturn(List.of());

        mockMvc.perform(get("/watchlist"))
                .andExpect(status().isOk())
                .andExpect(view().name("watchlist"))
                .andExpect(model().attributeExists("eintraege"));
    }

    /** WLCTRL-02: Hinzufügen redirected zur Marktplatz-Detailseite. */
    @Test
    @WithMockUser(username = "test@example.com")
    void hinzufuegenRedirected() throws Exception {
        AppUser user = testUser();
        Projekt projekt = new Projekt();
        projekt.setId(UUID.randomUUID());
        projekt.setName("Sommerfest");
        projekt.setSlug("sommerfest");

        when(appUserRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));
        when(projektService.findeNachSlug("sommerfest")).thenReturn(Optional.of(projekt));
        when(watchlistService.hinzufuegen(any(), any())).thenReturn(new WatchlistEintrag());

        mockMvc.perform(post("/watchlist/hinzufuegen/sommerfest").with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/marktplatz/sommerfest"));
    }

    /** WLCTRL-03: Entfernen redirected zur Watchlist. */
    @Test
    @WithMockUser(username = "test@example.com")
    void entfernenRedirected() throws Exception {
        AppUser user = testUser();
        Projekt projekt = new Projekt();
        projekt.setId(UUID.randomUUID());
        projekt.setName("Sommerfest");
        projekt.setSlug("sommerfest");

        when(appUserRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));
        when(projektService.findeNachSlug("sommerfest")).thenReturn(Optional.of(projekt));

        mockMvc.perform(post("/watchlist/entfernen/sommerfest").with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/watchlist"));
    }
}

