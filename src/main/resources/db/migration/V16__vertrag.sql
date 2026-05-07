-- =============================================================================
-- V16: Vertrag (Sponsoring-Vertrag aus angenommener Anfrage)
-- =============================================================================
-- Schliesst den Anfrage-Loop: ANGENOMMEN → Vertrag erzeugen → Unterzeichnen.
--
-- 1:1 zu sponsoring_anfrage. Snapshot-Felder (orgName/sponsorName/paketName/
-- preis_chf) werden bei der Erstellung kopiert, damit der Vertrag auch nach
-- nachträglichen Änderungen am Paket / Org-Profil seine ursprünglichen
-- Konditionen behält. Vertrag ist die rechtsverbindliche Quelle.
-- =============================================================================

CREATE TABLE vertrag (
    id                  UUID         PRIMARY KEY,
    anfrage_id          UUID         NOT NULL UNIQUE
                                     REFERENCES sponsoring_anfrage(id) ON DELETE CASCADE,
    status              VARCHAR(20)  NOT NULL DEFAULT 'ENTWURF',
    org_name            VARCHAR(255) NOT NULL,
    org_id              UUID         NOT NULL REFERENCES organisation(id),
    sponsor_name        VARCHAR(255),
    sponsor_email       VARCHAR(255),
    sponsor_org_id      UUID         REFERENCES organisation(id),
    paket_name          VARCHAR(255) NOT NULL,
    paket_beschreibung  TEXT,
    preis_chf           NUMERIC(10,2) NOT NULL,
    laufzeit_von        DATE,
    laufzeit_bis        DATE,
    leistung_verein     TEXT,
    leistung_sponsor    TEXT,
    erstellt_am         TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    erstellt_von        VARCHAR(255),
    unterzeichnet_am    TIMESTAMP,
    unterzeichnet_von   VARCHAR(255),

    CONSTRAINT chk_vertrag_status CHECK (status IN ('ENTWURF', 'UNTERZEICHNET', 'GEKUENDIGT'))
);

CREATE INDEX idx_vertrag_org_id ON vertrag(org_id);
CREATE INDEX idx_vertrag_status ON vertrag(status);
