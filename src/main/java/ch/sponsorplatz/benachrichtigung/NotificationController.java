package ch.sponsorplatz.benachrichtigung;

import ch.sponsorplatz.shared.exception.NotFoundException;
import ch.sponsorplatz.benutzer.AppUser;
import ch.sponsorplatz.benutzer.AppUserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Controller für In-App-Benachrichtigungen.
 *
 * <ul>
 *   <li>{@code GET  /benachrichtigungen}            — HTML-Liste (vom Sidebar-Bell-Click)</li>
 *   <li>{@code GET  /benachrichtigungen/anzahl}     — JSON für Badge-Polling</li>
 *   <li>{@code GET  /benachrichtigungen/liste}      — JSON-API für AJAX-Dropdowns</li>
 *   <li>{@code POST /benachrichtigungen/gelesen}    — alle als gelesen</li>
 *   <li>{@code POST /benachrichtigungen/{id}/gelesen} — einzelne als gelesen
 *       (Owner-Check gegen IDOR im Service)</li>
 * </ul>
 */
@Controller
@RequestMapping("/benachrichtigungen")
public class NotificationController {

    private final NotificationService notificationService;
    private final AppUserRepository appUserRepository;

    public NotificationController(NotificationService notificationService,
                                  AppUserRepository appUserRepository) {
        this.notificationService = notificationService;
        this.appUserRepository = appUserRepository;
    }

    /**
     * HTML-Liste — wird vom Sidebar-Bell-Click angesprungen. Markiert beim
     * Laden alle als gelesen, damit die Badge sofort verschwindet.
     */
    @GetMapping
    public String htmlListe(Authentication auth, Model model) {
        AppUser user = ladeUser(auth);
        List<Benachrichtigung> liste = notificationService.letzteNachrichtenFuer(user.getId());
        model.addAttribute("benachrichtigungen", BenachrichtigungView.von(liste));
        notificationService.markiereAlleAlsGelesen(user.getId());
        return "benachrichtigungen";
    }

    @GetMapping("/anzahl")
    @ResponseBody
    public ResponseEntity<Map<String, Long>> anzahlUngelesen(Authentication auth) {
        AppUser user = ladeUser(auth);
        long anzahl = notificationService.zaehleUngelesen(user.getId());
        return ResponseEntity.ok(Map.of("ungelesen", anzahl));
    }

    @GetMapping("/liste")
    @ResponseBody
    public ResponseEntity<List<BenachrichtigungView>> liste(Authentication auth) {
        AppUser user = ladeUser(auth);
        List<Benachrichtigung> liste = notificationService.letzteNachrichtenFuer(user.getId());
        return ResponseEntity.ok(BenachrichtigungView.von(liste));
    }

    @PostMapping("/gelesen")
    @ResponseBody
    public ResponseEntity<Void> alleGelesen(Authentication auth) {
        AppUser user = ladeUser(auth);
        notificationService.markiereAlleAlsGelesen(user.getId());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{id}/gelesen")
    @ResponseBody
    public ResponseEntity<Void> einzelnGelesen(@PathVariable UUID id, Authentication auth) {
        AppUser user = ladeUser(auth);
        notificationService.markiereAlsGelesen(id, user.getId());
        return ResponseEntity.ok().build();
    }

    private AppUser ladeUser(Authentication auth) {
        return appUserRepository.findByEmail(auth.getName())
                .orElseThrow(() -> new NotFoundException("User nicht gefunden"));
    }
}
