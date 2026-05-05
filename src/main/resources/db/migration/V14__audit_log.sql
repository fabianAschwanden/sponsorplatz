-- V14: Audit-Log Tabelle für Plattform-Aktionen
CREATE TABLE audit_log (
    id          UUID DEFAULT gen_random_uuid() NOT NULL PRIMARY KEY,
    zeitpunkt   TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    aktion      VARCHAR(50)  NOT NULL,
    bereich     VARCHAR(50)  NOT NULL,
    benutzer_id UUID,
    benutzer_email VARCHAR(255),
    ziel_id     UUID,
    ziel_typ    VARCHAR(50),
    details     TEXT,

    CONSTRAINT chk_audit_aktion CHECK (aktion IN (
        'ERSTELLT', 'AKTUALISIERT', 'GELOESCHT', 'VERIFIZIERT', 'SUSPENDIERT',
        'GESPERRT', 'ENTSPERRT', 'ROLLE_GEAENDERT', 'REGISTRIERT', 'LOGIN',
        'PASSWORT_GEAENDERT', 'BACKUP_ERSTELLT', 'EINLADUNG_GESENDET', 'ANFRAGE_ANGENOMMEN', 'ANFRAGE_ABGELEHNT'
    ))
);

CREATE INDEX idx_audit_zeitpunkt ON audit_log(zeitpunkt DESC);
CREATE INDEX idx_audit_bereich ON audit_log(bereich, zeitpunkt DESC);
CREATE INDEX idx_audit_benutzer ON audit_log(benutzer_id, zeitpunkt DESC);

