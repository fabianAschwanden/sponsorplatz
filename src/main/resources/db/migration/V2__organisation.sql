-- =============================================================================
-- V2 — Tabelle organisation
-- Phase 0.1: Wurzel-Entität für Vereine + Sponsoren
-- =============================================================================

CREATE TABLE organisation (
    id              UUID         NOT NULL PRIMARY KEY,
    typ             VARCHAR(20)  NOT NULL,
    name            VARCHAR(255) NOT NULL,
    slug            VARCHAR(120) NOT NULL UNIQUE,
    rechtsform      VARCHAR(50),
    branche         VARCHAR(50),
    beschreibung    TEXT,
    website_url     VARCHAR(500),
    status          VARCHAR(20)  NOT NULL DEFAULT 'PENDING',
    verifiziert_am  TIMESTAMP,
    zefix_uid       VARCHAR(20),
    registriert_am  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_organisation_typ
        CHECK (typ IN ('VEREIN', 'UNTERNEHMEN', 'STIFTUNG', 'ANDERE')),
    CONSTRAINT chk_organisation_status
        CHECK (status IN ('PENDING', 'VERIFIED', 'ACTIVE', 'SUSPENDED'))
);

CREATE INDEX idx_organisation_status ON organisation(status);
CREATE INDEX idx_organisation_typ    ON organisation(typ);
