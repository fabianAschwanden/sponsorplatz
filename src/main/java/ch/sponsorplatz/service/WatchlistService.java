package ch.sponsorplatz.service;

import ch.sponsorplatz.benutzer.AppUser;
import ch.sponsorplatz.model.Projekt;
import ch.sponsorplatz.model.WatchlistEintrag;
import ch.sponsorplatz.repository.WatchlistRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class WatchlistService {

    private final WatchlistRepository repository;

    public WatchlistService(WatchlistRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public List<WatchlistEintrag> findeNachUser(UUID userId) {
        return repository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    @Transactional(readOnly = true)
    public boolean istGemerkt(UUID userId, UUID projektId) {
        return repository.existsByUserIdAndProjektId(userId, projektId);
    }

    public WatchlistEintrag hinzufuegen(AppUser user, Projekt projekt) {
        if (repository.existsByUserIdAndProjektId(user.getId(), projekt.getId())) {
            throw new IllegalStateException("Projekt ist bereits auf der Watchlist");
        }

        WatchlistEintrag eintrag = new WatchlistEintrag();
        eintrag.setUser(user);
        eintrag.setProjekt(projekt);
        return repository.save(eintrag);
    }

    public void entferne(UUID userId, UUID projektId) {
        repository.deleteByUserIdAndProjektId(userId, projektId);
    }
}

