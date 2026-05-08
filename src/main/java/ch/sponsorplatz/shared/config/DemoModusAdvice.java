package ch.sponsorplatz.shared.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

/**
 * Stellt das Model-Attribut {@code demoModus} bereit, damit Templates
 * einen Disclaimer-Banner anzeigen können wenn das Demo-Profil aktiv ist.
 *
 * <p>{@code basePackages = "ch.sponsorplatz"} schränkt die Advice auf eigene
 * Controller ein — Spring-interne Controller (z.B. Actuator) bekommen das
 * Model-Attribut nicht; konsistent zu {@code CurrentUserAdvice}.
 */
@ControllerAdvice(basePackages = "ch.sponsorplatz")
public class DemoModusAdvice {

    private final boolean demoModus;

    public DemoModusAdvice(@Value("${sponsorplatz.demo-modus:false}") boolean demoModus) {
        this.demoModus = demoModus;
    }

    @ModelAttribute("demoModus")
    public boolean demoModus() {
        return demoModus;
    }
}

