package ch.sponsorplatz.benutzer;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import java.util.UUID;

/**
 * Macht Daten des eingeloggten Users (insb. Profilbild-URL) jedem Controller
 * via {@code aktuellerUser} Model-Attribut zugänglich. Das Sidebar-Fragment
 * nutzt dies, um konsistent das Profilbild unten links anzuzeigen.
 *
 * <p>{@link AppUserRepository} wird als {@link ObjectProvider} injiziert,
 * damit die Advice in {@code @WebMvcTest}-Slices (die keine JPA-Beans laden)
 * geladen werden kann, ohne dass der Kontext fehlschlägt — der Lookup
 * liefert dort einfach {@code null}.
 */
@ControllerAdvice(basePackages = "ch.sponsorplatz")
public class CurrentUserAdvice {

    private final ObjectProvider<AppUserRepository> appUserRepositoryProvider;

    public CurrentUserAdvice(ObjectProvider<AppUserRepository> appUserRepositoryProvider) {
        this.appUserRepositoryProvider = appUserRepositoryProvider;
    }

    @ModelAttribute("aktuellerUser")
    public AktuellerUser aktuellerUser(Authentication authentication) {
        if (authentication == null
                || !authentication.isAuthenticated()
                || "anonymousUser".equals(authentication.getPrincipal())) {
            return null;
        }
        AppUserRepository repo = appUserRepositoryProvider.getIfAvailable();
        if (repo == null) {
            return null;
        }
        return repo.findByEmail(authentication.getName().toLowerCase().trim())
                .map(AktuellerUser::von)
                .orElse(null);
    }

    /**
     * Schlanker Header-View — niemals passwortHash o.ä. Profilbild-URL ist
     * derselbe öffentliche {@code /medien/{id}}-Endpoint wie überall sonst.
     */
    public record AktuellerUser(UUID id, String anzeigename, String email, String profilbildUrl) {
        static AktuellerUser von(AppUser u) {
            String url = u.getProfilbildId() != null ? "/medien/" + u.getProfilbildId() : null;
            return new AktuellerUser(u.getId(), u.getAnzeigename(), u.getEmail(), url);
        }
    }
}
