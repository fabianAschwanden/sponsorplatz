-- =============================================================================
-- V12 — Branche auf Health-Fokus einschränken
-- =============================================================================
-- Sponsorplatz positioniert sich strikt auf Sport und Gesundheit (siehe
-- 07_Marketing_Konzept.md, 00_Konzept_v3_Kollaborative-Plattform.md).
--
-- Ergebnis dieser Migration:
--   • branche ist NOT NULL
--   • CHECK-Constraint erlaubt nur die elf Health-Werte (siehe Branche-Enum)
--
-- Backfill-Strategie:
--   Bestehende Datensätze mit NULL oder nicht erlaubten Werten werden auf
--   SPORT gemappt (defensiver Default — Plattform ist Pre-Launch, keine
--   produktiven Datensätze in Branchen ausserhalb des Fokus existieren).
-- =============================================================================

UPDATE organisation
SET branche = 'SPORT'
WHERE branche IS NULL
   OR branche NOT IN (
        'SPORT', 'BEWEGUNG', 'REHA', 'BEHINDERTENSPORT', 'SENIORENSPORT',
        'PRAEVENTION', 'MENTAL_HEALTH', 'ERNAEHRUNG', 'WELLNESS',
        'SELBSTHILFE', 'PATIENTENORGANISATION'
   );

ALTER TABLE organisation ALTER COLUMN branche SET NOT NULL;

ALTER TABLE organisation
    ADD CONSTRAINT chk_organisation_branche
    CHECK (branche IN (
        'SPORT', 'BEWEGUNG', 'REHA', 'BEHINDERTENSPORT', 'SENIORENSPORT',
        'PRAEVENTION', 'MENTAL_HEALTH', 'ERNAEHRUNG', 'WELLNESS',
        'SELBSTHILFE', 'PATIENTENORGANISATION'
    ));

CREATE INDEX idx_organisation_branche ON organisation(branche);
