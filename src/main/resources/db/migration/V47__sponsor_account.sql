-- =============================================================================
-- V47 — SponsorAccount (CRM Cluster 2, ADR-0011 private Sponsor-Layer)
-- =============================================================================
-- Erste Entität der privaten Sponsor-CRM-Layer. Modelliert die Beziehung
-- zwischen einer Sponsor-Org (Eigentümer der CRM-Daten) und einem gesponserten
-- Verein als First-Class-Account.
--
-- Isolation: besitzer_sponsor_org_id ist der Mandanten-Schlüssel. Jeder Lese-/
-- Schreibzugriff läuft über AccessControl.kannSponsorDatenSehen(besitzer, auth)
-- im SponsorAccountService — Daten erscheinen NIE im Marktplatz oder bei anderen
-- Sponsoren. Kein CHECK-Constraint auf status/tier: die Enums AccountStatus /
-- AccountTier sind alleinige Source of Truth (Pattern V44/V45/V46).
-- =============================================================================

CREATE TABLE sponsor_account (
    id                       UUID         DEFAULT gen_random_uuid() PRIMARY KEY,
    besitzer_sponsor_org_id  UUID         NOT NULL REFERENCES organisation(id) ON DELETE CASCADE,
    verein_org_id            UUID         NOT NULL REFERENCES organisation(id) ON DELETE CASCADE,
    account_owner_user_id    UUID                  REFERENCES app_user(id) ON DELETE SET NULL,
    status                   VARCHAR(30)  NOT NULL DEFAULT 'LEAD',
    tier                     VARCHAR(20),
    notiz                    TEXT,
    erstellt_am              TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    aktualisiert_am          TIMESTAMP,

    -- Ein Account pro Sponsor↔Verein-Paar — verhindert Dubletten im Portfolio.
    CONSTRAINT uq_sponsor_account_paar UNIQUE (besitzer_sponsor_org_id, verein_org_id)
);

-- Portfolio-Liste eines Sponsors ist der häufigste Query → Index auf den
-- Mandanten-Schlüssel.
CREATE INDEX idx_sponsor_account_besitzer ON sponsor_account(besitzer_sponsor_org_id);
