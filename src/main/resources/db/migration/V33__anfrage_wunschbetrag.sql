-- V33: Wunsch-Betrag auf Sponsoring-Anfragen
--
-- Kontakt-Anfragen (Verein → Sponsor, paket_id IS NULL) hatten bisher
-- keinen Weg, einen Richt-Preis zu kommunizieren. Vereine mussten den
-- Betrag in nachricht freitextlich erwähnen — Sponsor + Verein-Owner
-- konnten ihn beim Vertrag-Entwurf nicht maschinell wiederverwenden.
--
-- Spalte ist NULLABLE:
-- - Bei Paket-Anfragen NULL → Preis kommt aus paket.preis_chf.
-- - Bei Kontakt-Anfragen optional NULL (kein Richtbetrag) oder >0
--   (Richtbetrag, der vom VertragService als Initial-Preis übernommen wird).
--
-- CHECK ≥ 0 verhindert Eingabefehler (negative Beträge ergeben fachlich
-- keinen Sinn). Naturalien-Sponsoring (preis = 0) bleibt erlaubt.

ALTER TABLE sponsoring_anfrage
    ADD COLUMN wunsch_betrag_chf NUMERIC(12, 2) NULL;

ALTER TABLE sponsoring_anfrage
    ADD CONSTRAINT chk_anfrage_wunsch_betrag_nicht_negativ
    CHECK (wunsch_betrag_chf IS NULL OR wunsch_betrag_chf >= 0);
