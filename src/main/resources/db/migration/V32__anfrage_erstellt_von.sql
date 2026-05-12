-- V32: Tracking, welcher User eine Anfrage tatsächlich gestellt hat
--
-- Bisher hielten wir nur kontakt_name/kontakt_email als Freitext fest. Das
-- reicht nicht, um auf der „Meine Anfragen"-Seite saubere Pflicht-Trennung
-- zwischen „von mir gestellt" und „von meiner Org gestellt" zu rendern.
--
-- Spalte ist NULLABLE: historische Anfragen vor diesem Patch haben keinen
-- Creator zugeordnet — sie landen in der Org-Bucket (= „von meiner
-- Organisation gestellt"), niemand bekommt sie versehentlich als „meine".
--
-- ON DELETE SET NULL: User-Konto-Löschung darf den Anfrage-Audit-Trail
-- nicht abreißen; die kontakt_email bleibt zur Nachverfolgung erhalten.

ALTER TABLE sponsoring_anfrage
    ADD COLUMN erstellt_von_id UUID NULL REFERENCES app_user(id) ON DELETE SET NULL;

CREATE INDEX idx_anfrage_erstellt_von ON sponsoring_anfrage(erstellt_von_id);
