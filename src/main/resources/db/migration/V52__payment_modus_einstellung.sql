-- =============================================================================
-- V52: Payment-Modus-Einstellung auf Plattform-Ebene
-- =============================================================================
-- Admins können zwischen 'QR_RECHNUNG' (Swiss QR-Bill per E-Mail, kostenlos)
-- und 'DATATRANS' (Online-Zahlung via Hosted Payment Page) umschalten.
-- Default ist QR_RECHNUNG — Datatrans wird erst aktiviert, wenn Credentials
-- konfiguriert sind.
-- =============================================================================

ALTER TABLE plattform_einstellungen ADD COLUMN payment_modus VARCHAR(20) NOT NULL DEFAULT 'QR_RECHNUNG';

