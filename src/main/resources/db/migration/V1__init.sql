-- =============================================================================
-- V1 — Schema-Baseline für Sponsorplatz
-- =============================================================================
-- Diese Migration ist absichtlich leer. Sie markiert nur den Schema-Anfang
-- für Flyway. Echte Tabellen kommen ab V2 (Phase 0: Organisation, Mitgliedschaft).
-- =============================================================================

-- Pseudo-Statement, damit Flyway eine Migration sieht
CREATE TABLE IF NOT EXISTS schema_version_marker (
    id SMALLINT PRIMARY KEY,
    version VARCHAR(20) NOT NULL,
    erstellt_am TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

INSERT INTO schema_version_marker (id, version) VALUES (1, '0.1.0-baseline');
