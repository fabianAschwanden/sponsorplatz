package ch.sponsorplatz.dashboard;
import ch.sponsorplatz.projekt.EventView;
import ch.sponsorplatz.projekt.EventService;
import ch.sponsorplatz.projekt.ProjektService;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ch.sponsorplatz.anfrage.SponsoringAnfrageService;
import ch.sponsorplatz.benutzer.AppUserRepository;
import ch.sponsorplatz.organisation.MitgliedschaftRepository;

/**
 * Aggregiert Dashboard-Kennzahlen für einen Benutzer.
 *
 * <p>
 * H3-Fix: Statt N+1-Loops über die Mitglied-Orgs läuft das Laden in genau
 * <strong>4 Queries</strong>, unabhängig von der Anzahl Mitgliedschaften:
 * </p>
 *
 * <ol>
 * <li>{@code AppUser} per E-Mail (1 Query)</li>
 * <li>{@code findOrgIdsByUserId} — direkte Projection auf nur die
 * {@code org_id}-Spalte (1 Query, kein Org-Lazy-Load)</li>
 * <li>{@code zaehleOeffentlicheNachOrgIds} — Aggregat-COUNT (1 Query)</li>
 * <li>{@code zaehleEingehende} + {@code zaehleNeue} — je 1 Aggregat-COUNT (2
 * Queries)</li>
 * </ol>
 */
@Service
@Transactional(readOnly = true)
public class DashboardService {

    private final AppUserRepository appUserRepository;
    private final MitgliedschaftRepository mitgliedschaftRepository;
    private final ProjektService projektService;
    private final SponsoringAnfrageService anfrageService;
    private final EventService eventService;

    public DashboardService(AppUserRepository appUserRepository,
            MitgliedschaftRepository mitgliedschaftRepository,
            ProjektService projektService,
            SponsoringAnfrageService anfrageService,
            EventService eventService) {
        this.appUserRepository = appUserRepository;
        this.mitgliedschaftRepository = mitgliedschaftRepository;
        this.projektService = projektService;
        this.anfrageService = anfrageService;
        this.eventService = eventService;
    }

    /**
     * Lädt aggregierte Kennzahlen für den Benutzer mit der gegebenen E-Mail.
     * Gibt {@link DashboardDaten#leer()} zurück, falls der User unbekannt ist
     * oder keine Mitgliedschaften hat (Short-Circuit, keine Aggregat-Queries).
     */
    public DashboardDaten ladeDashboardDaten(String email) {
        return appUserRepository.findByEmail(email)
                .map(user -> {
                    List<UUID> orgIds = mitgliedschaftRepository.findOrgIdsByUserId(user.getId());
                    if (orgIds.isEmpty()) {
                        return DashboardDaten.leer();
                    }
                    List<EventView> events = EventView.von(
                            eventService.findeKommendeNachOrgIds(orgIds, 3));
                    return DashboardDaten.von(
                            orgIds.size(),
                            projektService.zaehleOeffentlicheNachOrgIds(orgIds),
                            anfrageService.zaehleEingehende(orgIds),
                            anfrageService.zaehleNeue(orgIds),
                            events);
                })
                .orElse(DashboardDaten.leer());
    }
}
