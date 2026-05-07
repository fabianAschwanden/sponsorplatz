package ch.sponsorplatz.controller;

import ch.sponsorplatz.dto.BenachrichtigungView;
import ch.sponsorplatz.exception.NotFoundException;
import ch.sponsorplatz.model.AppUser;
import ch.sponsorplatz.model.Benachrichtigung;
import ch.sponsorplatz.repository.AppUserRepository;
import ch.sponsorplatz.service.NotificationService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Controller für In-App-Benachrichtigungen.
 * Liefert JSON für AJAX-Badge und HTML-Seite für die vollständige Liste.
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
     * JSON-API: Anzahl ungelesener Notifications (für Badge-Polling).
     */
    @GetMapping("/anzahl")
    @ResponseBody
    public ResponseEntity<Map<String, Long>> anzahlUngelesen(Authentication auth) {
        AppUser user = ladeUser(auth);
        long anzahl = notificationService.zaehleUngelesen(user.getId());
        return ResponseEntity.ok(Map.of("ungelesen", anzahl));
    }

    /**
     * JSON-API: Letzte 20 Benachrichtigungen.
     */
    @GetMapping("/liste")
    @ResponseBody
    public ResponseEntity<List<BenachrichtigungView>> liste(Authentication auth) {
        AppUser user = ladeUser(auth);
        List<Benachrichtigung> liste = notificationService.letzteNachrichtenFuer(user.getId());
        return ResponseEntity.ok(BenachrichtigungView.von(liste));
    }

    /**
     * POST: Alle als gelesen markieren.
     */
    @PostMapping("/gelesen")
    @ResponseBody
    public ResponseEntity<Void> alleGelesen(Authentication auth) {
        AppUser user = ladeUser(auth);
        notificationService.markiereAlleAlsGelesen(user.getId());
        return ResponseEntity.ok().build();
    }

    /**
     * POST: Einzelne als gelesen markieren.
     */
    @PostMapping("/{id}/gelesen")
    @ResponseBody
    public ResponseEntity<Void> einzelnGelesen(@PathVariable UUID id) {
        notificationService.markiereAlsGelesen(id);
        return ResponseEntity.ok().build();
    }

    private AppUser ladeUser(Authentication auth) {
        return appUserRepository.findByEmail(auth.getName())
                .orElseThrow(() -> new NotFoundException("User nicht gefunden"));
    }
}

