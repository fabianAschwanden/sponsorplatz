-- V4: E-Mail-Verifizierung (Phase 1.2)
-- Felder auf app_user für Token-basierte E-Mail-Verifikation

ALTER TABLE app_user ADD COLUMN email_verifiziert BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE app_user ADD COLUMN verifikations_token VARCHAR(64) DEFAULT NULL;
ALTER TABLE app_user ADD COLUMN token_gueltig_bis TIMESTAMP DEFAULT NULL;

CREATE INDEX idx_app_user_verifikations_token ON app_user (verifikations_token);

