package ch.sponsorplatz.projekt;

import ch.sponsorplatz.organisation.AccessControl;
import ch.sponsorplatz.organisation.Organisation;
import ch.sponsorplatz.organisation.OrganisationRepository;
import ch.sponsorplatz.shared.config.ModelAttributeNames;
import ch.sponsorplatz.shared.exception.NotFoundException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.UUID;

/**
 * CRUD fuer Vereins-Events unter /organisationen/{slug}/events.
 * Schreib-Operationen erfordern Edit-Recht auf die Organisation.
 */
@Controller
@RequestMapping("/organisationen/{slug}/events")
public class EventController {

    private final EventService eventService;
    private final OrganisationRepository orgRepository;
    private final AccessControl accessControl;

    public EventController(EventService eventService,
                           OrganisationRepository orgRepository,
                           AccessControl accessControl) {
        this.eventService = eventService;
        this.orgRepository = orgRepository;
        this.accessControl = accessControl;
    }

    @GetMapping
    public String liste(@PathVariable String slug, Model model) {
        Organisation org = ladeOrg(slug);
        model.addAttribute(ModelAttributeNames.AKTIVE_SEITE, "events");
        model.addAttribute("orgSlug", slug);
        model.addAttribute("orgName", org.getName());
        model.addAttribute("events", EventView.von(eventService.findeNachOrg(org.getId())));
        return "event-liste";
    }

    @PostMapping("/speichern")
    public String speichern(@PathVariable String slug,
                            @RequestParam String name,
                            @RequestParam(required = false) String beschreibung,
                            @RequestParam(required = false) String ort,
                            @RequestParam String datum,
                            @RequestParam(required = false) String datumEnde,
                            @RequestParam(required = false) Integer kapazitaet,
                            Authentication auth) {
        Organisation org = ladeOrg(slug);
        if (!accessControl.kannOrgEditieren(org.getId(), auth)) {
            throw new org.springframework.security.access.AccessDeniedException("Keine Edit-Berechtigung");
        }
        eventService.erstelle(org.getId(), name, beschreibung, ort,
                LocalDate.parse(datum),
                datumEnde != null && !datumEnde.isBlank() ? LocalDate.parse(datumEnde) : null,
                kapazitaet);
        return "redirect:/organisationen/" + slug + "/events";
    }

    @PostMapping("/{eventId}/loeschen")
    public String loeschen(@PathVariable String slug,
                           @PathVariable UUID eventId,
                           Authentication auth) {
        Organisation org = ladeOrg(slug);
        if (!accessControl.kannOrgEditieren(org.getId(), auth)) {
            throw new org.springframework.security.access.AccessDeniedException("Keine Edit-Berechtigung");
        }
        eventService.loesche(eventId);
        return "redirect:/organisationen/" + slug + "/events";
    }

    private Organisation ladeOrg(String slug) {
        return orgRepository.findBySlug(slug)
                .orElseThrow(() -> new NotFoundException("Organisation nicht gefunden: " + slug));
    }
}

