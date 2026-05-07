-- =============================================================================
-- V21: Feature-Backlog — interner Ideen-Tracker direkt in der App
-- =============================================================================
-- Während der täglichen Nutzung fallen Verbesserungs-Vorschläge an.
-- Statt nach GitHub-Issues zu wechseln, werden sie direkt in der App
-- erfasst und gepflegt. Backlog-Items sind PLATFORM_ADMIN-only.
-- =============================================================================

CREATE TABLE backlog_item (
    id            UUID         DEFAULT gen_random_uuid() PRIMARY KEY,
    titel         VARCHAR(200) NOT NULL,
    beschreibung  TEXT,
    status        VARCHAR(20)  NOT NULL DEFAULT 'OFFEN',
    prioritaet    VARCHAR(10)  NOT NULL DEFAULT 'MITTEL',
    erstellt_am   TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    erstellt_von  VARCHAR(100),
    erledigt_am   TIMESTAMP,

    CONSTRAINT chk_backlog_status CHECK (status IN ('OFFEN', 'IN_ARBEIT', 'ERLEDIGT', 'VERWORFEN')),
    CONSTRAINT chk_backlog_prio   CHECK (prioritaet IN ('HOCH', 'MITTEL', 'NIEDRIG'))
);

CREATE INDEX idx_backlog_status      ON backlog_item(status);
CREATE INDEX idx_backlog_erstellt_am ON backlog_item(erstellt_am DESC);
