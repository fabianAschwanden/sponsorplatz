package ch.sponsorplatz.projekt;

import ch.sponsorplatz.benutzer.AppUser;
import ch.sponsorplatz.benutzer.AppUserRepository;
import ch.sponsorplatz.shared.exception.NotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class WatchlistService {

    private final WatchlistRepository repository;
    private final AppUserRepository appUserRepository;
    private final ProjektRepository projektRepository;

    public WatchlistService(WatchlistRepository repository,
                            AppUserRepository appUserRepository,
                            ProjektRepository projektRepository) {
        this.repository = repository;
        this.appUserRepository = appUserRepository;
        this.projektRepository = projektRepository;
    }

    /**
     * Controller-Variante: lädt User+Projekt selbst über die IDs/Slug,
     * legt den Eintrag an und gibt den Projekt-Namen für die Flash-Message
     * zurück. Controller berührt keine Entity (ARCH-02).
     */
    public String hinzufuegenNachSlug(UUID userId, String projektSlug) {
        AppUser user = appUserRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User nicht gefunden: " + userId));
        Projekt projekt = projektRepository.findBySlug(projektSlug)
                .orElseThrow(() -> new NotFoundException("Projekt nicht gefunden: " + projektSlug));
        hinzufuegen(user, projekt);
        return projekt.getName();
    }

    /** Controller-Variante: entfernt anhand Slug und gibt den Projekt-Namen zurück. */
    public String entfernenNachSlug(UUID userId, String projektSlug) {
        Projekt projekt = projektRepository.findBySlug(projektSlug)
                .orElseThrow(() -> new NotFoundException("Projekt nicht gefunden: " + projektSlug));
        repository.deleteByUserIdAndProjektId(userId, projekt.getId());
        return projekt.getName();
    }

    @Transactional(readOnly = true)
    public List<WatchlistEintrag> findeNachUser(UUID userId) {
        return repository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    /** View-Variante — Controller braucht keine Entity-Liste (ARCH-02). */
    @Transactional(readOnly = true)
    public List<WatchlistEintragView> findeViewsNachUser(UUID userId) {
        return WatchlistEintragView.von(repository.findByUserIdOrderByCreatedAtDesc(userId));
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

