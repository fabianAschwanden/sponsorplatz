-- V19: In-App-Benachrichtigungen
CREATE TABLE benachrichtigung (
    id          UUID DEFAULT gen_random_uuid() NOT NULL PRIMARY KEY,
    empfaenger_id UUID NOT NULL REFERENCES app_user(id) ON DELETE CASCADE,
    typ         VARCHAR(50) NOT NULL,
    titel       VARCHAR(255) NOT NULL,
    text        TEXT,
    link        VARCHAR(500),
    gelesen     BOOLEAN NOT NULL DEFAULT FALSE,
    created_at  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),

    CONSTRAINT chk_benachrichtigung_typ CHECK (typ IN (
        'NEUE_ANFRAGE', 'ANFRAGE_ANGENOMMEN', 'ANFRAGE_ABGELEHNT',
        'NEUE_NACHRICHT', 'ORG_VERIFIZIERT', 'ORG_SUSPENDIERT',
        'EINLADUNG_ERHALTEN', 'MITGLIED_HINZUGEFUEGT', 'SYSTEM'
    ))
);

CREATE INDEX idx_benachrichtigung_empfaenger ON benachrichtigung(empfaenger_id, gelesen, created_at DESC);

