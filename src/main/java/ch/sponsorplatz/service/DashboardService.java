package ch.sponsorplatz.service;
import ch.sponsorplatz.benutzer.AppUser;

import ch.sponsorplatz.dto.DashboardDaten;
import ch.sponsorplatz.benutzer.AppUserRepository;
import ch.sponsorplatz.organisation.MitgliedschaftRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Aggregiert Dashboard-Kennzahlen für einen Benutzer.
 *
 * <p>H3-Fix: Statt N+1-Loops über die Mitglied-Orgs läuft das Laden in genau
 * <strong>4 Queries</strong>, unabhängig von der Anzahl Mitgliedschaften:</p>
 *
 * <ol>
 *   <li>{@code AppUser} per E-Mail (1 Query)</li>
 *   <li>{@code findOrgIdsByUserId} — direkte Projection auf nur die {@code org_id}-Spalte (1 Query, kein Org-Lazy-Load)</li>
 *   <li>{@code zaehleOeffentlicheNachOrgIds} — Aggregat-COUNT (1 Query)</li>
 *   <li>{@code zaehleEingehende} + {@code zaehleNeue} — je 1 Aggregat-COUNT (2 Queries)</li>
 * </ol>
 */
@Service
@Transactional(readOnly = true)
public class DashboardService {

    private final AppUserRepository appUserRepository;
    private final MitgliedschaftRepository mitgliedschaftRepository;
    private final ProjektService projektService;
    private final SponsoringAnfrageService anfrageService;

    public DashboardService(AppUserRepository appUserRepository,
                            MitgliedschaftRepository mitgliedschaftRepository,
                            ProjektService projektService,
                            SponsoringAnfrageService anfrageService) {
        this.appUserRepository = appUserRepository;
        this.mitgliedschaftRepository = mitgliedschaftRepository;
        this.projektService = projektService;
        this.anfrageService = anfrageService;
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
                    return DashboardDaten.von(
                            orgIds.size(),
                            projektService.zaehleOeffentlicheNachOrgIds(orgIds),
                            anfrageService.zaehleEingehende(orgIds),
                            anfrageService.zaehleNeue(orgIds)
                    );
                })
                .orElse(DashboardDaten.leer());
    }
}
