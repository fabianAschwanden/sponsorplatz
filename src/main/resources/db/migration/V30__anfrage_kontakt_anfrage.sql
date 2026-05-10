-- V30: Kontakt-Anfrage — Verein darf einen Sponsor proaktiv anschreiben.
--
-- Bisher war eine SponsoringAnfrage immer paket-bezogen (Sponsor klickt auf
-- Paket eines Vereins). Mit dem neuen Verein→Sponsor-Flow gibt es freie
-- Kontaktanfragen ohne Paket-Bezug — paket_id wird nullable, neuer Betreff.
--
-- Bestehende Daten bleiben unverändert (paket_id ist gesetzt für alle alten
-- Rows). Neue Rows können entweder paket_id ODER betreff haben.

ALTER TABLE sponsoring_anfrage
    ALTER COLUMN paket_id DROP NOT NULL;

ALTER TABLE sponsoring_anfrage
    ADD COLUMN betreff VARCHAR(255);
