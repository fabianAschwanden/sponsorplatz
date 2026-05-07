-- =============================================================================
-- V17: Rechnung mit Swiss QR-Bill + IBAN auf Organisation
-- =============================================================================
-- Verein hinterlegt seinen IBAN einmalig im Org-Profil. Aus einem Vertrag wird
-- eine Rechnung generiert (PDF mit Swiss QR-Code), Sponsor zahlt per Banking-
-- App-Scan auf den Verein-IBAN. Verein markiert manuell als bezahlt.
--
-- Stripe Connect / Online-Zahlung folgt später als optionales Add-on.
-- =============================================================================

-- IBAN auf Organisation (für Rechnungen)
ALTER TABLE organisation ADD COLUMN iban         VARCHAR(34);
-- Postadresse für Creditor in der QR-Bill (Spec von Six Group: Pflicht)
ALTER TABLE organisation ADD COLUMN strasse      VARCHAR(70);
ALTER TABLE organisation ADD COLUMN postleitzahl VARCHAR(16);
ALTER TABLE organisation ADD COLUMN ort          VARCHAR(70);

-- Rechnung — eine pro Vertrag (UNIQUE)
CREATE TABLE rechnung (
    id                  UUID         PRIMARY KEY,
    vertrag_id          UUID         NOT NULL UNIQUE
                                     REFERENCES vertrag(id) ON DELETE CASCADE,
    org_id              UUID         NOT NULL REFERENCES organisation(id),
    rechnungsnummer     VARCHAR(50)  NOT NULL,
    status              VARCHAR(20)  NOT NULL DEFAULT 'OFFEN',
    betrag_chf          NUMERIC(10,2) NOT NULL,
    iban                VARCHAR(34)  NOT NULL,
    qr_referenz         VARCHAR(27),
    sponsor_name        VARCHAR(255) NOT NULL,
    sponsor_email       VARCHAR(255),
    sponsor_adresse     VARCHAR(500),
    zahlungszweck       VARCHAR(140),
    erstellt_am         TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    erstellt_von        VARCHAR(255),
    faellig_am          DATE         NOT NULL,
    bezahlt_am          TIMESTAMP,
    bezahlt_von         VARCHAR(255),

    CONSTRAINT chk_rechnung_status CHECK (status IN ('OFFEN', 'BEZAHLT', 'STORNIERT'))
);

CREATE INDEX idx_rechnung_org_id ON rechnung(org_id);
CREATE INDEX idx_rechnung_status ON rechnung(status);
CREATE UNIQUE INDEX uq_rechnung_org_nummer ON rechnung(org_id, rechnungsnummer);
