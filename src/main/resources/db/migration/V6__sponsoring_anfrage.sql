-- V6: SponsoringAnfrage (Phase 4)

CREATE TABLE sponsoring_anfrage (
    id UUID NOT NULL PRIMARY KEY,
    paket_id UUID NOT NULL,
    anfragender_org_id UUID NOT NULL,
    empfaenger_org_id UUID NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'NEU',
    nachricht TEXT,
    antwort TEXT,
    kontakt_name VARCHAR(255),
    kontakt_email VARCHAR(255),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    beantwortet_am TIMESTAMP,
    CONSTRAINT chk_anfrage_status CHECK (status IN ('NEU','IN_PRUEFUNG','ANGENOMMEN','ABGELEHNT','ZURUECKGEZOGEN')),
    CONSTRAINT fk_anfrage_paket FOREIGN KEY (paket_id) REFERENCES sponsoring_paket(id) ON DELETE CASCADE,
    CONSTRAINT fk_anfrage_anfragender FOREIGN KEY (anfragender_org_id) REFERENCES organisation(id),
    CONSTRAINT fk_anfrage_empfaenger FOREIGN KEY (empfaenger_org_id) REFERENCES organisation(id)
);

CREATE INDEX idx_anfrage_paket ON sponsoring_anfrage (paket_id);
CREATE INDEX idx_anfrage_empfaenger ON sponsoring_anfrage (empfaenger_org_id);
CREATE INDEX idx_anfrage_anfragender ON sponsoring_anfrage (anfragender_org_id);
CREATE INDEX idx_anfrage_status ON sponsoring_anfrage (status);

