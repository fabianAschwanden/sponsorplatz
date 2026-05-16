-- Plattform-Style-Switch (Admin kann zwischen default und 'css-ch' wählen).
-- aktiver_style ist NOT NULL mit Default 'default' — bestehende Singleton-Row
-- bekommt das beim Backfill, neue Rows erben den Default-Constraint.

ALTER TABLE plattform_einstellungen
    ADD COLUMN aktiver_style VARCHAR(32) NOT NULL DEFAULT 'default';
