package ch.sponsorplatz.service;

import ch.sponsorplatz.model.AppUser;
import ch.sponsorplatz.model.Projekt;
import ch.sponsorplatz.model.WatchlistEintrag;
import ch.sponsorplatz.repository.WatchlistRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class WatchlistServiceTest {

    private WatchlistRepository repository;
    private WatchlistService service;

    @BeforeEach
    void setUp() {
        repository = mock(WatchlistRepository.class);
        service = new WatchlistService(repository);
    }

    /** WL-01: Projekt zur Watchlist hinzufügen. */
    @Test
    void hinzufuegen() {
        AppUser user = new AppUser();
        user.setId(UUID.randomUUID());
        Projekt projekt = new Projekt();
        projekt.setId(UUID.randomUUID());

        when(repository.existsByUserIdAndProjektId(user.getId(), projekt.getId())).thenReturn(false);
        when(repository.save(any(WatchlistEintrag.class))).thenAnswer(inv -> inv.getArgument(0));

        WatchlistEintrag eintrag = service.hinzufuegen(user, projekt);

        assertThat(eintrag.getUser()).isEqualTo(user);
        assertThat(eintrag.getProjekt()).isEqualTo(projekt);
    }

    /** WL-02: Bereits gemerkt → wirft. */
    @Test
    void doppeltHinzufuegenWirft() {
        AppUser user = new AppUser();
        user.setId(UUID.randomUUID());
        Projekt projekt = new Projekt();
        projekt.setId(UUID.randomUUID());

        when(repository.existsByUserIdAndProjektId(user.getId(), projekt.getId())).thenReturn(true);

        assertThatThrownBy(() -> service.hinzufuegen(user, projekt))
                .isInstanceOf(IllegalStateException.class);
    }

    /** WL-03: Entfernen delegiert ans Repository. */
    @Test
    void entfernen() {
        UUID userId = UUID.randomUUID();
        UUID projektId = UUID.randomUUID();

        service.entferne(userId, projektId);

        verify(repository).deleteByUserIdAndProjektId(userId, projektId);
    }

    /** WL-04: Watchlist eines Users laden. */
    @Test
    void findeNachUser() {
        UUID userId = UUID.randomUUID();
        when(repository.findByUserIdOrderByCreatedAtDesc(userId)).thenReturn(List.of());

        List<WatchlistEintrag> result = service.findeNachUser(userId);

        assertThat(result).isEmpty();
        verify(repository).findByUserIdOrderByCreatedAtDesc(userId);
    }

    /** WL-05: Ist gemerkt prüfen. */
    @Test
    void istGemerkt() {
        UUID userId = UUID.randomUUID();
        UUID projektId = UUID.randomUUID();
        when(repository.existsByUserIdAndProjektId(userId, projektId)).thenReturn(true);

        assertThat(service.istGemerkt(userId, projektId)).isTrue();
    }
}

