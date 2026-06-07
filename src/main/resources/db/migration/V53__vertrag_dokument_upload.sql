-- V53 — Hochgeladenes Vertragsdokument (extern unterschriebenes PDF).
--
-- Neben dem aus den Konditionen generierten PDF kann der Verein ein eigenes,
-- z.B. physisch unterschriebenes oder anwaltlich aufgesetztes Vertrags-PDF
-- hochladen. Ist ein Dokument hinterlegt, hat es in der PDF-Route Vorrang vor
-- dem generierten PDF (ein Vertrag, ein Download).
--
-- Additiv (CLAUDE.md: niemals destruktiv). Alle Spalten nullable — Bestands-
-- Verträge ohne Upload bleiben unangetastet und liefern weiter das generierte PDF.

ALTER TABLE vertrag ADD COLUMN dokument_storage_pfad   VARCHAR(500);
ALTER TABLE vertrag ADD COLUMN dokument_dateiname       VARCHAR(255);
ALTER TABLE vertrag ADD COLUMN dokument_content_type    VARCHAR(100);
ALTER TABLE vertrag ADD COLUMN dokument_groesse_bytes   BIGINT;
ALTER TABLE vertrag ADD COLUMN dokument_hochgeladen_am  TIMESTAMP;
ALTER TABLE vertrag ADD COLUMN dokument_hochgeladen_von VARCHAR(255);
