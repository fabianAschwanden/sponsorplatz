package ch.sponsorplatz.shared.exception;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.stereotype.Controller;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;

import ch.sponsorplatz.shared.config.SecurityConfig;
import ch.sponsorplatz.benutzer.SponsorplatzUserDetailsService;
import org.springframework.test.context.ActiveProfiles;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

/**
 * Tests für GlobalExceptionHandler — Mapping aller Exceptions auf gerenderte error-View
 * mit korrekten HTTP-Status-Codes.
 *
 * Nutzt einen lokalen Test-Controller, der die jeweiligen Exceptions wirft.
 */
@WebMvcTest(controllers = GlobalExceptionHandlerTest.WerfendeTestController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class, GlobalExceptionHandlerTest.WerfendeTestController.class})
@ActiveProfiles("dev")
class GlobalExceptionHandlerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private SponsorplatzUserDetailsService userDetailsService;

    /** EXC-01: NotFoundException → 404 + View error. */
    @Test
    @WithMockUser
    void notFoundIst404() throws Exception {
        mockMvc.perform(get("/test/not-found"))
            .andExpect(status().isNotFound())
            .andExpect(view().name("error"));
    }

    /** EXC-02: IllegalArgumentException → 400 + View error. */
    @Test
    @WithMockUser
    void illegalArgumentIst400() throws Exception {
        mockMvc.perform(get("/test/illegal-argument"))
            .andExpect(status().isBadRequest())
            .andExpect(view().name("error"));
    }

    /** EXC-03: IllegalStateException → 409 + View error. */
    @Test
    @WithMockUser
    void illegalStateIst409() throws Exception {
        mockMvc.perform(get("/test/illegal-state"))
            .andExpect(status().isConflict())
            .andExpect(view().name("error"));
    }

    /** EXC-04: AccessDeniedException → 403 + View error. */
    @Test
    @WithMockUser
    void accessDeniedIst403() throws Exception {
        mockMvc.perform(get("/test/access-denied"))
            .andExpect(status().isForbidden())
            .andExpect(view().name("error"));
    }

    /** EXC-05: Error-View enthält die erwarteten Model-Attribute. */
    @Test
    @WithMockUser
    void errorViewHatModelAttribute() throws Exception {
        mockMvc.perform(get("/test/not-found"))
            .andExpect(model().attributeExists("status", "error", "message"))
            .andExpect(model().attribute("status", 404));
    }

    /**
     * EXC-06: Unerwarteter Throwable (hier ein {@link Error}, wie ihn das AWT/
     * Font-Subsystem werfen kann) → 500 + gerenderte error-View statt nacktem
     * JSON-500. Auffangnetz-Handler (BETA-V09).
     */
    @Test
    @WithMockUser
    void unerwarteterFehlerIst500() throws Exception {
        mockMvc.perform(get("/test/internal-error"))
            .andExpect(status().isInternalServerError())
            .andExpect(view().name("error"))
            .andExpect(model().attribute("status", 500));
    }

    /**
     * Test-Controller, der gezielt Exceptions wirft, damit der GlobalExceptionHandler
     * sie behandelt. Wird nur über den Slice-Test instanziiert.
     */
    @Controller
    static class WerfendeTestController {

        @GetMapping("/test/not-found")
        public String notFound() {
            throw new NotFoundException("Ressource gibt es nicht");
        }

        @GetMapping("/test/illegal-argument")
        public String illegalArgument() {
            throw new IllegalArgumentException("Ungültiger Parameter");
        }

        @GetMapping("/test/illegal-state")
        public String illegalState() {
            throw new IllegalStateException("Inkonsistenter Zustand");
        }

        @GetMapping("/test/access-denied")
        public String accessDenied() {
            throw new AccessDeniedException("Verboten");
        }

        @GetMapping("/test/internal-error")
        public String internalError() {
            // Simuliert einen AWT/Font-Subsystem-Fehler (Error, nicht Exception),
            // der bisher als roher 500 ohne error-View durchschlug.
            throw new InternalError("Simuliertes Grafik-Subsystem-Problem");
        }
    }
}
