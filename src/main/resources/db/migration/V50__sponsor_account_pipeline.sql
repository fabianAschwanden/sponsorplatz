-- =============================================================================
-- V50 — SponsorAccount Pipeline-Stufe + Forecast (CRM Cluster 3, Lücke #4)
-- =============================================================================
-- Additiv: erweitert sponsor_account um die Vertriebs-Pipeline. pipeline_stage
-- bildet die Akquise-Reise ab (LEAD → QUALIFIZIERT → ANGEBOT → GEWONNEN/VERLOREN),
-- forecast_betrag_chf das erwartete Sponsoring-Volumen. Die gewichtete Forecast-
-- Summe (Betrag × Stufen-Wahrscheinlichkeit) wird im View berechnet, nicht
-- gespeichert.
--
-- Kein CHECK-Constraint auf pipeline_stage: das Enum PipelineStage ist alleinige
-- Source of Truth (Pattern V44/V45/V46). DEFAULT 'LEAD' für Bestands-Accounts.
-- =============================================================================

ALTER TABLE sponsor_account ADD COLUMN pipeline_stage     VARCHAR(20)   NOT NULL DEFAULT 'LEAD';
ALTER TABLE sponsor_account ADD COLUMN forecast_betrag_chf NUMERIC(12,2);
