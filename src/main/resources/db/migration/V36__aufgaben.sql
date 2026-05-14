-- Phase 12: Aufgabenverwaltung (customizable Task-Engine)
-- AufgabenDefinition = "Workflow-Vorlage" (Admin-customizable: Trigger-Entity-Typ + Trigger-Status,
-- Ziel-Status, Assignee-Regel). Aufgabe = Instanz, beim Status-Wechsel einer Entity erzeugt und
-- bei erreichtem Ziel-Status erledigt — siehe AufgabenEngine.

CREATE TABLE aufgaben_definition (
    id UUID PRIMARY KEY,
    titel VARCHAR(200) NOT NULL,
    beschreibung VARCHAR(1000),
    trigger_entity_typ VARCHAR(30) NOT NULL,
    trigger_status VARCHAR(40) NOT NULL,
    ziel_status VARCHAR(40),
    assignee_regel VARCHAR(40) NOT NULL,
    link_template VARCHAR(200),
    aktiv BOOLEAN NOT NULL DEFAULT TRUE,
    system_definition BOOLEAN NOT NULL DEFAULT FALSE,
    erstellt_am TIMESTAMP NOT NULL,
    erstellt_von VARCHAR(100),
    CONSTRAINT chk_aufgaben_def_entity_typ CHECK (trigger_entity_typ IN ('ORG', 'ANFRAGE', 'VERTRAG', 'RECHNUNG', 'PROJEKT')),
    CONSTRAINT chk_aufgaben_def_assignee_regel CHECK (assignee_regel IN ('PLATFORM_ADMIN', 'ORG_MITGLIEDER', 'ANFRAGE_EMPFAENGER_ORG', 'ANFRAGE_ANFRAGENDER_ORG', 'VERTRAG_VEREIN_ORG', 'VERTRAG_SPONSOR_ORG', 'RECHNUNG_VEREIN_ORG'))
);

CREATE TABLE aufgabe (
    id UUID PRIMARY KEY,
    definition_id UUID NOT NULL REFERENCES aufgaben_definition(id),
    entity_typ VARCHAR(30) NOT NULL,
    entity_id UUID NOT NULL,
    titel VARCHAR(250) NOT NULL,
    link VARCHAR(300),
    status VARCHAR(20) NOT NULL DEFAULT 'OFFEN',
    assignee_org_id UUID REFERENCES organisation(id) ON DELETE CASCADE,
    nur_platform_admin BOOLEAN NOT NULL DEFAULT FALSE,
    erstellt_am TIMESTAMP NOT NULL,
    erledigt_am TIMESTAMP,
    erledigt_von_user_id UUID REFERENCES app_user(id) ON DELETE SET NULL,
    CONSTRAINT chk_aufgabe_status CHECK (status IN ('OFFEN', 'ERLEDIGT', 'ENTFALLEN'))
);

CREATE INDEX idx_aufgabe_assignee_org_status ON aufgabe(assignee_org_id, status);
CREATE INDEX idx_aufgabe_admin_status ON aufgabe(nur_platform_admin, status);
CREATE INDEX idx_aufgabe_entity ON aufgabe(entity_typ, entity_id, status);

-- System-Default-Definitionen für die 4 initialen Use-Cases (system_definition=TRUE → nicht löschbar im Admin-UI).
-- IDs sind feste UUIDs, damit Tests/Seeds zuverlässig referenzieren können.
INSERT INTO aufgaben_definition (id, titel, beschreibung, trigger_entity_typ, trigger_status, ziel_status, assignee_regel, link_template, aktiv, system_definition, erstellt_am)
VALUES
    ('aaaaaaa1-0000-0000-0000-000000000001', 'Verein freigeben', 'Neue Organisation wartet auf Verifizierung durch das Plattform-Team.', 'ORG',      'PENDING',  'VERIFIED',      'PLATFORM_ADMIN',           '/admin/verifizierungen', TRUE, TRUE, CURRENT_TIMESTAMP),
    ('aaaaaaa1-0000-0000-0000-000000000002', 'Anfrage bearbeiten', 'Eine Sponsoring-Anfrage wartet auf Antwort.',                       'ANFRAGE',  'NEU',      'ANGENOMMEN',    'ANFRAGE_EMPFAENGER_ORG',   '/meine-anfragen',        TRUE, TRUE, CURRENT_TIMESTAMP),
    ('aaaaaaa1-0000-0000-0000-000000000003', 'Vertrag prüfen (Verein)', 'Vertrag im Entwurf — Verein muss prüfen und unterzeichnen.',  'VERTRAG',  'ENTWURF',  'UNTERZEICHNET', 'VERTRAG_VEREIN_ORG',       '/vertraege',             TRUE, TRUE, CURRENT_TIMESTAMP),
    ('aaaaaaa1-0000-0000-0000-000000000004', 'Vertrag prüfen (Sponsor)', 'Vertrag im Entwurf — Sponsor muss prüfen und unterzeichnen.','VERTRAG',  'ENTWURF',  'UNTERZEICHNET', 'VERTRAG_SPONSOR_ORG',      '/vertraege',             TRUE, TRUE, CURRENT_TIMESTAMP),
    ('aaaaaaa1-0000-0000-0000-000000000005', 'Rechnung versenden / nachverfolgen', 'Offene Rechnung wartet auf Zahlung.',               'RECHNUNG', 'OFFEN',    'BEZAHLT',       'RECHNUNG_VEREIN_ORG',      '/rechnungen',            TRUE, TRUE, CURRENT_TIMESTAMP);
