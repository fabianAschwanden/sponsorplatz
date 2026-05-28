-- =============================================================================
-- V49 — Aktivitaet (CRM Cluster 2, ADR-0011 private Sponsor-Layer)
-- =============================================================================
-- MS-Dynamics-Pattern: Activity mit 'regarding'. Die Aktivität bezieht sich
-- immer auf einen SponsorAccount (sponsor_account_id, Pflicht — Mandanten-
-- Scope) und optional auf eine KontaktPerson (kontakt_person_id ≙ Dynamics
-- 'regarding contact'). Bildet die Interaktions-Timeline pro Account:
-- Anrufe, E-Mails, Meetings, Event-Besuche, Notizen.
--
-- Isolation: besitzer_sponsor_org_id (denormalisiert) ist der Mandanten-
-- Schlüssel. Zugriff über AktivitaetService → kannSponsorDatenSehen. Kein
-- CHECK auf typ (Enum AktivitaetTyp ist Source of Truth, Pattern V44-V48).
--
-- 'datum' ist das fachliche Datum der Interaktion (vom User gesetzt),
-- 'erstellt_am' der technische Audit-Zeitstempel.
-- =============================================================================

CREATE TABLE aktivitaet (
    id                       UUID         DEFAULT gen_random_uuid() PRIMARY KEY,
    besitzer_sponsor_org_id  UUID         NOT NULL REFERENCES organisation(id) ON DELETE CASCADE,
    sponsor_account_id       UUID         NOT NULL REFERENCES sponsor_account(id) ON DELETE CASCADE,
    kontakt_person_id        UUID                  REFERENCES kontakt_person(id) ON DELETE SET NULL,
    typ                      VARCHAR(30)  NOT NULL DEFAULT 'NOTIZ',
    datum                    DATE         NOT NULL,
    betreff                  VARCHAR(200) NOT NULL,
    notiz                    TEXT,
    erstellt_am              TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    erstellt_von_user_id     UUID
);

-- Timeline-Query (Account, sortiert nach Datum) ist der Hot-Path.
CREATE INDEX idx_aktivitaet_account ON aktivitaet(sponsor_account_id, datum DESC);
CREATE INDEX idx_aktivitaet_besitzer ON aktivitaet(besitzer_sponsor_org_id);
