package ch.sponsorplatz.audit;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ch.sponsorplatz.benutzer.AppUser;
import ch.sponsorplatz.benutzer.AppUserRepository;
import ch.sponsorplatz.organisation.MitgliedschaftRepository;
import ch.sponsorplatz.projekt.WatchlistRepository;

/**
 * DSG-konformer Datenexport: Gibt alle gespeicherten Daten eines Users als Map
 * zurück
 * (JSON-serialisierbar für Download).
 */
@Service
@Transactional(readOnly = true)
public class DatenExportService implements ch.sponsorplatz.benutzer.BenutzerDatenExport {

    private final AppUserRepository appUserRepository;
    private final MitgliedschaftRepository mitgliedschaftRepository;
    private final WatchlistRepository watchlistRepository;

    public DatenExportService(AppUserRepository appUserRepository,
            MitgliedschaftRepository mitgliedschaftRepository,
            WatchlistRepository watchlistRepository) {
        this.appUserRepository = appUserRepository;
        this.mitgliedschaftRepository = mitgliedschaftRepository;
        this.watchlistRepository = watchlistRepository;
    }

    public Map<String, Object> exportiere(UUID userId) {
        AppUser user = appUserRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User nicht gefunden: " + userId));

        Map<String, Object> export = new LinkedHashMap<>();
        export.put("benutzer", benutzerDaten(user));
        export.put("mitgliedschaften", mitgliedschaftDaten(userId));
        export.put("watchlist", watchlistDaten(userId));
        export.put("exportiertAm", java.time.Instant.now().toString());
        return export;
    }

    private Map<String, Object> benutzerDaten(AppUser user) {
        Map<String, Object> daten = new LinkedHashMap<>();
        daten.put("id", user.getId().toString());
        daten.put("email", user.getEmail());
        daten.put("anzeigename", user.getAnzeigename());
        daten.put("platformRolle", user.getPlatformRolle() != null ? user.getPlatformRolle().name() : "KEINE");
        daten.put("emailVerifiziert", user.isEmailVerifiziert());
        daten.put("registriertAm", user.getRegistriertAm() != null ? user.getRegistriertAm().toString() : null);
        return daten;
    }

    private List<Map<String, String>> mitgliedschaftDaten(UUID userId) {
        return mitgliedschaftRepository.findByUserId(userId).stream()
                .map(m -> Map.of(
                        "organisation", m.getOrg().getName(),
                        "rolle", m.getRolle().name()))
                .toList();
    }

    private List<Map<String, String>> watchlistDaten(UUID userId) {
        return watchlistRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(w -> Map.of(
                        "projekt", w.getProjekt().getName(),
                        "gemerkAm", w.getCreatedAt().toString()))
                .toList();
    }
}
