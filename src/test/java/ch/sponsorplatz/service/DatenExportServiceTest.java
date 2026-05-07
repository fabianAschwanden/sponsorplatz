package ch.sponsorplatz.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ch.sponsorplatz.model.AppUser;
import ch.sponsorplatz.model.PlatformRolle;
import ch.sponsorplatz.repository.AppUserRepository;
import ch.sponsorplatz.repository.MitgliedschaftRepository;
import ch.sponsorplatz.repository.WatchlistRepository;

class DatenExportServiceTest {

    private AppUserRepository appUserRepository;
    private MitgliedschaftRepository mitgliedschaftRepository;
    private WatchlistRepository watchlistRepository;
    private DatenExportService service;

    @BeforeEach
    void setUp() {
        appUserRepository = mock(AppUserRepository.class);
        mitgliedschaftRepository = mock(MitgliedschaftRepository.class);
        watchlistRepository = mock(WatchlistRepository.class);
        service = new DatenExportService(appUserRepository, mitgliedschaftRepository, watchlistRepository);
    }

    /** DSG-01: Export enthält User-Stammdaten. */
    @Test
    void exportEnthaeltStammdaten() {
        AppUser user = new AppUser();
        user.setId(UUID.randomUUID());
        user.setEmail("max@example.com");
        user.setAnzeigename("Max Muster");
        user.setPlatformRolle(PlatformRolle.PLATFORM_ADMIN);

        when(appUserRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(mitgliedschaftRepository.findByUserId(user.getId())).thenReturn(List.of());
        when(watchlistRepository.findByUserIdOrderByCreatedAtDesc(user.getId())).thenReturn(List.of());

        Map<String, Object> export = service.exportiere(user.getId());

        assertThat(export).containsKey("benutzer");
        @SuppressWarnings("unchecked")
        Map<String, Object> benutzer = (Map<String, Object>) export.get("benutzer");
        assertThat(benutzer.get("email")).isEqualTo("max@example.com");
        assertThat(benutzer.get("anzeigename")).isEqualTo("Max Muster");
    }

    /** DSG-02: Export enthält Mitgliedschaften. */
    @Test
    void exportEnthaeltMitgliedschaften() {
        AppUser user = new AppUser();
        user.setId(UUID.randomUUID());
        user.setEmail("max@example.com");
        user.setAnzeigename("Max");
        user.setPlatformRolle(PlatformRolle.PLATFORM_ADMIN);

        when(appUserRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(mitgliedschaftRepository.findByUserId(user.getId())).thenReturn(List.of());
        when(watchlistRepository.findByUserIdOrderByCreatedAtDesc(user.getId())).thenReturn(List.of());

        Map<String, Object> export = service.exportiere(user.getId());

        assertThat(export).containsKey("mitgliedschaften");
    }

    /** DSG-03: Export enthält Watchlist. */
    @Test
    void exportEnthaeltWatchlist() {
        AppUser user = new AppUser();
        user.setId(UUID.randomUUID());
        user.setEmail("max@example.com");
        user.setAnzeigename("Max");
        user.setPlatformRolle(PlatformRolle.PLATFORM_ADMIN);

        when(appUserRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(mitgliedschaftRepository.findByUserId(user.getId())).thenReturn(List.of());
        when(watchlistRepository.findByUserIdOrderByCreatedAtDesc(user.getId())).thenReturn(List.of());

        Map<String, Object> export = service.exportiere(user.getId());

        assertThat(export).containsKey("watchlist");
    }

    /** DSG-04: Export für nicht existierenden User wirft. */
    @Test
    void exportFuerUnbekanntenUserWirft() {
        UUID id = UUID.randomUUID();
        when(appUserRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.exportiere(id))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
