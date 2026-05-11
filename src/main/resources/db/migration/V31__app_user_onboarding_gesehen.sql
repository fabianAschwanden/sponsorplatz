-- V31: Onboarding-Wizard nur einmal nach der Registrierung zeigen
--
-- Bisher wurde jeder User ohne Mitgliedschaft bei jedem Login auf das
-- Onboarding umgeleitet. Mit diesem Flag wird das Onboarding nur einmal
-- nach der Registrierung gezeigt; nach dem ersten Aufruf merkt sich der
-- User-Datensatz, dass es bereits gesehen wurde.
--
-- Bestehende User werden auf TRUE gesetzt — sie hatten bereits ihre
-- Chance bzw. sollen nicht erneut in den Wizard geschickt werden.
-- Neue Registrierungen werden vom Default FALSE erfasst (siehe Entity).

ALTER TABLE app_user ADD COLUMN onboarding_gesehen BOOLEAN NOT NULL DEFAULT FALSE;

UPDATE app_user SET onboarding_gesehen = TRUE;
