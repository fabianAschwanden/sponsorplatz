package ch.sponsorplatz.shared.einstellungen;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

/**
 * Stellt jedem Template den aktiven Plattform-Style als {@code aktiverStyle}
 * Model-Attribut bereit. Das Styles-Fragment lädt darauf basierend ggf. ein
 * Theme-Override-Stylesheet (z.B. {@code theme-css-ch.css}) zusätzlich zu
 * {@code dashboard.css}.
 *
 * <p>{@link PlattformEinstellungenService} wird als {@link ObjectProvider}
 * injiziert, damit {@code @WebMvcTest}-Slices ohne JPA-Beans laden — der
 * Lookup liefert dort {@code null} und das Attribut fällt auf
 * {@code "default"} zurück.
 */
@ControllerAdvice(basePackages = "ch.sponsorplatz")
public class StyleAdvice {

    private final ObjectProvider<PlattformEinstellungenService> serviceProvider;

    public StyleAdvice(ObjectProvider<PlattformEinstellungenService> serviceProvider) {
        this.serviceProvider = serviceProvider;
    }

    @ModelAttribute("aktiverStyle")
    public String aktiverStyle() {
        PlattformEinstellungenService service = serviceProvider.getIfAvailable();
        if (service == null) {
            return "default";
        }
        try {
            return service.ladeAktivenStyle();
        } catch (RuntimeException e) {
            // Defensive: falls Singleton-Row noch fehlt (zwischen Migrationen)
            // → kein Build-Bruch, einfach Default-Style.
            return "default";
        }
    }
}
