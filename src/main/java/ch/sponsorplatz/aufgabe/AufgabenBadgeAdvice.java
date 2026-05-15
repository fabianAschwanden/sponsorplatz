package ch.sponsorplatz.aufgabe;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

/**
 * Stellt die Anzahl offener Aufgaben des eingeloggten Users jedem Controller
 * als {@code badgeAufgaben} Model-Attribut zur Verfügung. Das Sidebar-Fragment
 * rendert daraus den roten Badge neben dem „Aufgaben"-Menüpunkt.
 *
 * <p>{@link AufgabenService} wird als {@link ObjectProvider} injiziert, damit
 * {@code @WebMvcTest}-Slices ohne JPA-Beans laden können — der Lookup liefert
 * dort {@code null} und das Attribut bleibt leer.
 */
@ControllerAdvice(basePackages = "ch.sponsorplatz")
public class AufgabenBadgeAdvice {

    private final ObjectProvider<AufgabenService> aufgabenServiceProvider;

    public AufgabenBadgeAdvice(ObjectProvider<AufgabenService> aufgabenServiceProvider) {
        this.aufgabenServiceProvider = aufgabenServiceProvider;
    }

    @ModelAttribute("badgeAufgaben")
    public Long badgeAufgaben(Authentication authentication) {
        if (authentication == null
                || !authentication.isAuthenticated()
                || "anonymousUser".equals(authentication.getPrincipal())) {
            return null;
        }
        AufgabenService service = aufgabenServiceProvider.getIfAvailable();
        if (service == null) {
            return null;
        }
        long offen = service.zaehleMeineOffenen(authentication.getName());
        return offen > 0 ? offen : null;
    }
}
