-- V35: Vertrag-Kündigungs-Felder
--
-- Bisher hatte der Vertrag nur einen UNTERZEICHNET-Endpunkt — Kündigung
-- (Status GEKUENDIGT) war als Enum-Wert vorhanden, aber kein Pfad in der
-- Service-Schicht und keine Zeitstempel-Snapshot-Felder.
--
-- Spec: SPONSORING_ZAHLUNGSFLUSS.md §3.1 (Statusmaschine), §3.3 (Kündigung
-- mit offener vs. bezahlter Rechnung).
--
-- NULLABLE beide Spalten: nur gesetzt wenn Vertrag tatsächlich gekündigt
-- wird. Historische UNTERZEICHNET- und ENTWURF-Verträge bleiben NULL.
-- Maxlänge 500 für den Freitext-Grund wie bei rechnung.storno_grund (V34).

ALTER TABLE vertrag ADD COLUMN gekuendigt_am TIMESTAMP NULL;
ALTER TABLE vertrag ADD COLUMN kuendigungs_grund VARCHAR(500) NULL;
