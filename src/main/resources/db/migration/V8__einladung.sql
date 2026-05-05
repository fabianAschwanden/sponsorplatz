-- V8: Einladungs-Tabelle für Mitglieder-Einladungs-Flow
CREATE TABLE einladung (
    id              UUID            NOT NULL DEFAULT gen_random_uuid(),
    org_id          UUID            NOT NULL,
    email           VARCHAR(255)    NOT NULL,
    rolle           VARCHAR(20)     NOT NULL,
    token           VARCHAR(64)     NOT NULL,
    eingeladen_von  UUID            NOT NULL,
    gueltig_bis     TIMESTAMP       NOT NULL,
    created_at      TIMESTAMP       NOT NULL DEFAULT now(),
    CONSTRAINT pk_einladung PRIMARY KEY (id),
    CONSTRAINT fk_einladung_org FOREIGN KEY (org_id) REFERENCES organisation(id) ON DELETE CASCADE,
    CONSTRAINT fk_einladung_user FOREIGN KEY (eingeladen_von) REFERENCES app_user(id) ON DELETE SET NULL,
    CONSTRAINT chk_einladung_rolle CHECK (rolle IN ('ORG_OWNER','ORG_EDITOR','ORG_VIEWER')),
    CONSTRAINT uq_einladung_token UNIQUE (token),
    CONSTRAINT uq_einladung_org_email UNIQUE (org_id, email)
);

CREATE INDEX IF NOT EXISTS idx_einladung_token ON einladung(token);
CREATE INDEX IF NOT EXISTS idx_einladung_org_id ON einladung(org_id);

-- Index für Admin-Verifizierungs-Queue (PENDING-Orgs)
CREATE INDEX IF NOT EXISTS idx_organisation_status ON organisation(status);

