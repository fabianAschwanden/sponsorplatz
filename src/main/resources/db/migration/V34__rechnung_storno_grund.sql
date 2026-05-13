-- V34: Storno-Grund auf Rechnungen
--
-- Bisher konnte eine Rechnung von OFFEN auf STORNIERT gesetzt werden, ohne
-- dass der Grund erfasst wurde — Buchhaltungs-Audit und der Sponsor wussten
-- nachträglich nicht, warum eine Rechnung storniert wurde.
--
-- Spec: SPONSORING_ZAHLUNGSFLUSS.md §8.1
-- Audit-Event: RECHNUNG_STORNIERT (siehe AuditAktion)
--
-- NULLABLE: historische Storno-Datensätze vor V34 haben keinen Grund.
-- Maxlänge 500 Zeichen reicht für freitextliche Begründung; falls länger
-- gewünscht, kann das Feld nachträglich erweitert werden.

ALTER TABLE rechnung ADD COLUMN storno_grund VARCHAR(500) NULL;
