-- V11: Medien-Assets für Projekte und Organisationen (Cover, Logo, Galerie, Pitch-Deck)
CREATE TABLE medien_asset (
    id              UUID            NOT NULL DEFAULT gen_random_uuid(),
    dateiname       VARCHAR(255)    NOT NULL,
    content_type    VARCHAR(100)    NOT NULL,
    groesse_bytes   BIGINT          NOT NULL,
    storage_pfad    VARCHAR(500)    NOT NULL,
    entity_typ      VARCHAR(20)     NOT NULL,
    entity_id       UUID            NOT NULL,
    asset_typ       VARCHAR(20)     NOT NULL,
    sortierung      INT             NOT NULL DEFAULT 0,
    erstellt_am     TIMESTAMP       NOT NULL DEFAULT now(),
    CONSTRAINT pk_medien_asset PRIMARY KEY (id),
    CONSTRAINT chk_medien_entity_typ CHECK (entity_typ IN ('PROJEKT', 'ORGANISATION')),
    CONSTRAINT chk_medien_asset_typ CHECK (asset_typ IN ('COVER', 'GALERIE', 'PITCH_DECK', 'LOGO'))
);

CREATE INDEX idx_medien_asset_entity ON medien_asset(entity_typ, entity_id);
CREATE INDEX idx_medien_asset_typ ON medien_asset(entity_typ, entity_id, asset_typ);

