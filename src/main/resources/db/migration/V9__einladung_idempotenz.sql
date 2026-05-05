-- V9: Idempotenz-Marker für Einladungs-Annahme (M4-Fix)
-- Statt die Einladung beim Annehmen zu löschen, setzen wir 'angenommen_am'.
-- Beim erneuten Klick auf den Link kann der Service erkennen: "schon angenommen,
-- du bist Mitglied" — keine 400-Antwort mehr für legitime User.
-- Cleanup nach Token-Ablauf erfolgt weiter über den EinladungsCleanupJob.

ALTER TABLE einladung ADD COLUMN angenommen_am TIMESTAMP NULL;
