-- V25: Getrennte Branche-Achsen für Verein vs Unternehmen
--
-- Bisher hatte JEDE Org eine Health/Sport-Branche aus dem Branche-Enum —
-- für UNTERNEHMEN (Sponsor-Firmen wie Versicherung, Bank, Pharma) ist das
-- semantisch falsch. Lösung: zweite Spalte sponsor_branche für Industrie,
-- mit XOR-CHECK je nach OrgTyp.
--
-- Backfill-Strategie für bestehende UNTERNEHMEN: setze sponsor_branche=ANDERE,
-- existing branche bleibt vorerst gesetzt (in einer späteren Migration löschbar).

ALTER TABLE organisation ADD COLUMN sponsor_branche VARCHAR(50);

-- Backfill: bestehende UNTERNEHMEN bekommen ANDERE als Default
UPDATE organisation SET sponsor_branche = 'ANDERE' WHERE typ = 'UNTERNEHMEN';

-- branche darf jetzt NULL sein (für UNTERNEHMEN-Orgs); ein Service-Validator
-- forciert die typ-spezifische Pflicht. DROP NOT NULL ist Postgres-Standard
-- und wird auch von H2 (PostgreSQL-Mode) akzeptiert.
ALTER TABLE organisation ALTER COLUMN branche DROP NOT NULL;

-- CHECK-Constraint: SponsorBranche-Werte aus dem Java-Enum
ALTER TABLE organisation ADD CONSTRAINT chk_sponsor_branche_werte
    CHECK (sponsor_branche IS NULL OR sponsor_branche IN (
        'VERSICHERUNG', 'BANK', 'PHARMA', 'LEBENSMITTEL', 'SPORTARTIKEL',
        'MOBILITAET', 'ENERGIE', 'TELEKOM', 'RETAIL', 'MEDIEN',
        'IMMOBILIEN', 'BERATUNG', 'ANDERE'));

-- XOR-Pflicht: VEREIN braucht branche, UNTERNEHMEN braucht sponsor_branche.
-- Stiftungen sind flexibel — eines von beiden reicht.
ALTER TABLE organisation ADD CONSTRAINT chk_branche_pro_typ
    CHECK (
        (typ = 'VEREIN'      AND branche IS NOT NULL) OR
        (typ = 'UNTERNEHMEN' AND sponsor_branche IS NOT NULL) OR
        (typ = 'STIFTUNG'    AND (branche IS NOT NULL OR sponsor_branche IS NOT NULL)) OR
        (typ = 'ANDERE')
    );

CREATE INDEX idx_organisation_sponsor_branche ON organisation(sponsor_branche);
