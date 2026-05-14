package ch.sponsorplatz.projekt;
import ch.sponsorplatz.shared.exception.GlobalExceptionHandler;

import ch.sponsorplatz.shared.config.SecurityConfig;
import ch.sponsorplatz.benutzer.AppUser;
import ch.sponsorplatz.benutzer.AppUserService;
import ch.sponsorplatz.benutzer.SponsorplatzUserDetailsService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
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

    @MockitoBean
    private WatchlistService watchlistService;

    @MockitoBean
    private ProjektService projektService;

    @MockitoBean
    private AppUserService appUserService;

    @MockitoBean
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
        when(appUserService.findeNachEmail("test@example.com")).thenReturn(Optional.of(user));
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

        when(appUserService.findeNachEmail("test@example.com")).thenReturn(Optional.of(user));
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

        when(appUserService.findeNachEmail("test@example.com")).thenReturn(Optional.of(user));
        when(projektService.findeNachSlug("sommerfest")).thenReturn(Optional.of(projekt));

        mockMvc.perform(post("/watchlist/entfernen/sommerfest").with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/watchlist"));
    }
}

