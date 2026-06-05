-- =============================================================================
-- V51: Payment-Transaction — Audit + Idempotenz für Zahlungs-Provider-Webhooks
-- =============================================================================
-- Jeder Provider-Aufruf (Datatrans Init, Webhook-Bestätigung) wird als
-- Transaktion persistiert. Der UNIQUE-Constraint auf (provider, provider_reference)
-- stellt sicher, dass doppelte Webhook-Lieferungen keine Duplikate erzeugen.
-- =============================================================================

CREATE TABLE payment_transaction (
    id                    UUID         PRIMARY KEY,
    rechnung_id           UUID         NOT NULL REFERENCES rechnung(id),
    provider              VARCHAR(50)  NOT NULL,
    provider_reference    VARCHAR(255) NOT NULL,
    status                VARCHAR(20)  NOT NULL DEFAULT 'ERSTELLT',
    betrag_chf            NUMERIC(10,2) NOT NULL,
    checkout_url          VARCHAR(2000),
    raw_payload           TEXT,
    erstellt_am           TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    aktualisiert_am       TIMESTAMP,

    CONSTRAINT chk_pay_tx_status   CHECK (status IN ('ERSTELLT','AUTORISIERT','BEZAHLT','FEHLGESCHLAGEN','STORNIERT')),
    CONSTRAINT chk_pay_tx_provider CHECK (provider IN ('stub','datatrans')),
    CONSTRAINT uq_pay_provider_ref UNIQUE (provider, provider_reference)
);

CREATE INDEX idx_pay_tx_rechnung ON payment_transaction(rechnung_id);
CREATE INDEX idx_pay_tx_status   ON payment_transaction(status);

