-- =============================================================================
-- V15: Plattform-Einstellungen (Singleton-Row)
-- =============================================================================
-- Speichert SMTP-Settings, die im Admin-UI (/admin/mail-einstellungen) editiert
-- werden können. Priorität pro Setting: DB > ENV > leer.
--
-- Singleton-Row-Pattern: genau eine Row, beim Migrate vorbelegt.
-- =============================================================================

CREATE TABLE plattform_einstellungen (
    id                        UUID         PRIMARY KEY,
    smtp_host                 VARCHAR(255),
    smtp_port                 INTEGER,
    smtp_user                 VARCHAR(255),
    smtp_password             VARCHAR(500),
    smtp_auth                 BOOLEAN      NOT NULL DEFAULT TRUE,
    smtp_starttls             BOOLEAN      NOT NULL DEFAULT TRUE,
    mail_absender             VARCHAR(255),
    mail_test_empfaenger      VARCHAR(255),
    aktualisiert_am           TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    aktualisiert_von          VARCHAR(255)
);

-- Singleton-Row anlegen (UUID hardcoded, damit Updates idempotent sind)
INSERT INTO plattform_einstellungen (id)
VALUES ('00000000-0000-0000-0000-000000000001');
