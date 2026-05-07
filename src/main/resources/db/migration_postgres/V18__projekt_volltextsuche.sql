-- =============================================================================
-- V18 (Postgres-only): Volltextsuche auf projekt via tsvector
-- =============================================================================
-- Generated Column kombiniert Name (Gewicht A), Beschreibung (B), Kategorie + Ort
-- (C). Sprache: german — Stemmer kennt Sport-/Gesundheits-Begriffe (Verein →
-- vereine, Bewegung → bewegung etc.) und entfernt Stop-Words.
--
-- Org-Name wird NICHT in den tsvektor aufgenommen (cross-table-reference geht
-- nicht in einer GENERATED ALWAYS), sondern in der WHERE-Klausel via JOIN +
-- LIKE ergänzt — als Sekundär-Treffer ohne tsvektor-Score.
--
-- Diese Migration liegt in db/migration_postgres (NICHT als Subordner von
-- db/migration — Flyway scannt rekursiv) und wird nur in prod/cloud-free
-- via spring.flyway.locations geladen. dev (H2) fällt zur Laufzeit auf
-- LIKE zurück, weil tsvector dort nicht existiert.
-- =============================================================================

ALTER TABLE projekt ADD COLUMN tsvektor TSVECTOR
    GENERATED ALWAYS AS (
        setweight(to_tsvector('german', coalesce(name, '')),         'A') ||
        setweight(to_tsvector('german', coalesce(beschreibung, '')), 'B') ||
        setweight(to_tsvector('german', coalesce(kategorie, '')),    'C') ||
        setweight(to_tsvector('german', coalesce(ort, '')),          'C')
    ) STORED;

CREATE INDEX idx_projekt_tsvektor ON projekt USING GIN(tsvektor);
