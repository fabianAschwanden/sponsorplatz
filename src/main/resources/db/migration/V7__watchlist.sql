-- V7: Watchlist (Phase 5 — Sponsoren merken sich Projekte)

CREATE TABLE watchlist_eintrag (
    id UUID NOT NULL PRIMARY KEY,
    user_id UUID NOT NULL,
    projekt_id UUID NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_watchlist_user_projekt UNIQUE (user_id, projekt_id),
    CONSTRAINT fk_watchlist_user FOREIGN KEY (user_id) REFERENCES app_user(id) ON DELETE CASCADE,
    CONSTRAINT fk_watchlist_projekt FOREIGN KEY (projekt_id) REFERENCES projekt(id) ON DELETE CASCADE
);

CREATE INDEX idx_watchlist_user ON watchlist_eintrag (user_id);

