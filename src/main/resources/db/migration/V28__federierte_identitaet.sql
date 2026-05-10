-- =============================================================================
-- V28 — Föderierte Identitäten (OIDC SSO)
-- =============================================================================
-- Spec: AUTH_SSO_OIDC.md §5.2 (dort als "V25" bezeichnet — V25 ist aber bereits
-- für sponsor_branche vergeben, daher V28).
--
-- Speichert die Verknüpfung zwischen einem AppUser und seinem Identity-Provider-
-- Subject. Erlaubt mehrere Provider pro User (Entra ID jetzt, später Google etc.).
-- =============================================================================

CREATE TABLE federierte_identitaet (
    id                UUID         DEFAULT gen_random_uuid() PRIMARY KEY,
    user_id           UUID         NOT NULL REFERENCES app_user(id) ON DELETE CASCADE,
    provider          VARCHAR(50)  NOT NULL,
    subject           VARCHAR(255) NOT NULL,
    email_at_provider VARCHAR(255),
    verbunden_am      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    letzter_login_am  TIMESTAMP,

    CONSTRAINT chk_provider          CHECK (provider IN ('ENTRA_ID')),
    CONSTRAINT uq_provider_subject   UNIQUE (provider, subject)
);

CREATE INDEX idx_federierte_user_id ON federierte_identitaet(user_id);
