package ch.sponsorplatz.dto;

import ch.sponsorplatz.model.WatchlistEintrag;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Read-only View-DTO für Watchlist-Einträge mit nested ProjektView.
 */
public record WatchlistEintragView(
        UUID id,
        Instant createdAt,
        ProjektView projekt
) {

    public static WatchlistEintragView von(WatchlistEintrag e) {
        return new WatchlistEintragView(
                e.getId(),
                e.getCreatedAt(),
                ProjektView.von(e.getProjekt())
        );
    }

    public static List<WatchlistEintragView> von(List<WatchlistEintrag> eintraege) {
        return eintraege.stream().map(WatchlistEintragView::von).toList();
    }
}
