-- V5: Projekt und SponsoringPaket (Phase 2)

CREATE TABLE projekt (
    id UUID NOT NULL PRIMARY KEY,
    org_id UUID NOT NULL,
    name VARCHAR(255) NOT NULL,
    slug VARCHAR(120) NOT NULL,
    beschreibung TEXT,
    sichtbarkeit VARCHAR(20) NOT NULL DEFAULT 'ENTWURF',
    kategorie VARCHAR(50),
    ort VARCHAR(100),
    start_datum DATE,
    end_datum DATE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    veroeffentlicht_am TIMESTAMP,
    CONSTRAINT uq_projekt_slug UNIQUE (slug),
    CONSTRAINT chk_projekt_sichtbarkeit CHECK (sichtbarkeit IN ('ENTWURF','INTERN','OEFFENTLICH','ARCHIVIERT')),
    CONSTRAINT fk_projekt_org FOREIGN KEY (org_id) REFERENCES organisation(id) ON DELETE CASCADE
);

CREATE INDEX idx_projekt_org ON projekt (org_id);
CREATE INDEX idx_projekt_sichtbarkeit ON projekt (sichtbarkeit);

CREATE TABLE sponsoring_paket (
    id UUID NOT NULL PRIMARY KEY,
    projekt_id UUID NOT NULL,
    name VARCHAR(255) NOT NULL,
    beschreibung TEXT,
    preis_chf DECIMAL(10,2),
    gegenleistungen TEXT,
    sortierung INT NOT NULL DEFAULT 0,
    aktiv BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_paket_projekt FOREIGN KEY (projekt_id) REFERENCES projekt(id) ON DELETE CASCADE
);

CREATE INDEX idx_sponsoring_paket_projekt ON sponsoring_paket (projekt_id);

