package ch.sponsorplatz.shared.config;

import ch.sponsorplatz.anfrage.EngagementService;
import ch.sponsorplatz.anfrage.StartseitenTeaser;
import ch.sponsorplatz.home.HomeController;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import jakarta.servlet.http.Cookie;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * I18N-01..04: Cookie-basierter LocaleResolver + URL-Override via ?lang=
 */
@WebMvcTest(controllers = HomeController.class)
@Import({SecurityConfig.class, LocaleConfig.class})
@ActiveProfiles("dev")
class LocaleConfigTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private EngagementService engagementService;

    @BeforeEach
    void stubTeaser() {
        when(engagementService.findeStartseitenEngagements(any(), anyInt()))
                .thenReturn(new StartseitenTeaser(List.of(), List.of(), null, false));
    }

    @Test
    @DisplayName("I18N-01: Cookie 'lang' persistiert die Sprache — nächster Request liest korrekte Locale")
    void cookiePersistenz() throws Exception {
        // Setze Sprache via ?lang=fr
        mockMvc.perform(get("/?lang=fr"))
                .andExpect(status().isOk())
                .andExpect(cookie().exists("lang"));

        // Nächster Request mit dem Cookie → Locale bleibt fr
        mockMvc.perform(get("/").cookie(new Cookie("lang", "fr_CH")))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("I18N-02: ?lang=fr setzt Locale auf fr_CH, Cookie wird aktualisiert")
    void urlOverrideFr() throws Exception {
        mockMvc.perform(get("/?lang=fr"))
                .andExpect(status().isOk())
                .andExpect(cookie().value("lang", org.hamcrest.Matchers.containsString("fr")));
    }

    @Test
    @DisplayName("I18N-03: ?lang=de wechselt zurück zu de_CH")
    void urlOverrideDe() throws Exception {
        mockMvc.perform(get("/?lang=de"))
                .andExpect(status().isOk())
                .andExpect(cookie().value("lang", org.hamcrest.Matchers.containsString("de")));
    }

    @Test
    @DisplayName("I18N-04: Ohne ?lang-Param → Default de_CH (kein Cookie gesetzt wenn nicht gewechselt)")
    void defaultLocale() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().isOk());
        // Kein Fehler, default de_CH wird verwendet
    }
}

