-- V3: AppUser und Mitgliedschaft (Phase 0.2)

CREATE TABLE app_user (
    id UUID NOT NULL PRIMARY KEY,
    email VARCHAR(255) NOT NULL,
    passwort_hash VARCHAR(255) NOT NULL,
    anzeigename VARCHAR(100) NOT NULL,
    platform_rolle VARCHAR(30) DEFAULT NULL,
    aktiv BOOLEAN NOT NULL DEFAULT TRUE,
    registriert_am TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_app_user_email UNIQUE (email),
    CONSTRAINT chk_app_user_platform_rolle CHECK (platform_rolle IN ('PLATFORM_ADMIN','PLATFORM_MODERATOR','PLATFORM_SUPPORT'))
);

CREATE INDEX idx_app_user_platform_rolle ON app_user (platform_rolle);

CREATE TABLE mitgliedschaft (
    id UUID NOT NULL PRIMARY KEY,
    user_id UUID NOT NULL,
    org_id UUID NOT NULL,
    rolle VARCHAR(20) NOT NULL,
    eingeladen_von UUID DEFAULT NULL,
    beigetreten_am TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_mitgliedschaft_user_org UNIQUE (user_id, org_id),
    CONSTRAINT chk_mitgliedschaft_rolle CHECK (rolle IN ('ORG_OWNER','ORG_EDITOR','ORG_VIEWER')),
    CONSTRAINT fk_mitgliedschaft_user FOREIGN KEY (user_id) REFERENCES app_user(id) ON DELETE CASCADE,
    CONSTRAINT fk_mitgliedschaft_org FOREIGN KEY (org_id) REFERENCES organisation(id) ON DELETE CASCADE,
    CONSTRAINT fk_mitgliedschaft_eingeladen_von FOREIGN KEY (eingeladen_von) REFERENCES app_user(id) ON DELETE SET NULL
);

CREATE INDEX idx_mitgliedschaft_org ON mitgliedschaft (org_id);

