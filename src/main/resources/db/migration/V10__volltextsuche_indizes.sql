-- V10: Indizes für Volltextsuche auf Projekt-Feldern.
-- Die JPQL-LIKE-Suche funktioniert auf beiden DBs.
-- Einfache Spalten-Indizes beschleunigen Prefix-Suchen.

CREATE INDEX IF NOT EXISTS idx_projekt_name ON projekt(name);
CREATE INDEX IF NOT EXISTS idx_projekt_kategorie ON projekt(kategorie);
CREATE INDEX IF NOT EXISTS idx_projekt_ort ON projekt(ort);
