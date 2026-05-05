-- V13: Nachrichten-Tabelle für Inbox (Thread an SponsoringAnfrage)
CREATE TABLE nachricht (
    id          UUID DEFAULT gen_random_uuid() NOT NULL PRIMARY KEY,
    anfrage_id  UUID        NOT NULL REFERENCES sponsoring_anfrage(id) ON DELETE CASCADE,
    absender_id UUID        NOT NULL REFERENCES app_user(id),
    text        TEXT        NOT NULL,
    created_at  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),

    CONSTRAINT chk_nachricht_text_nicht_leer CHECK (length(trim(text)) >= 1)
);

CREATE INDEX idx_nachricht_anfrage ON nachricht(anfrage_id, created_at);

