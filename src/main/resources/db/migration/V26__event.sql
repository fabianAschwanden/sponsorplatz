-- V26: Event-Tabelle fuer Vereins-Events
CREATE TABLE event (
    id UUID NOT NULL PRIMARY KEY,
    org_id UUID NOT NULL REFERENCES organisation(id),
    name VARCHAR(255) NOT NULL,
    slug VARCHAR(255) NOT NULL,
    beschreibung TEXT,
    ort VARCHAR(200),
    datum DATE NOT NULL,
    datum_ende DATE,
    kapazitaet INTEGER,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_event_slug UNIQUE (slug)
);

CREATE INDEX idx_event_org_datum ON event (org_id, datum);
