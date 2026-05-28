-- =============================================================================
-- V48 — KontaktPerson (CRM Cluster 2, ADR-0011 private Sponsor-Layer)
-- =============================================================================
-- MS-Dynamics-Pattern: Contact unter Account. Ein Kontakt gehört zu genau einem
-- SponsorAccount (sponsor_account_id ≙ Dynamics 'parentcustomerid'). Modelliert
-- externe Ansprechpartner (Präsident, Trainer, Pressesprecher), die KEINEN
-- Plattform-Account haben — daher nur Visitenkarten-Felder, kein FK auf app_user.
--
-- Isolation: besitzer_sponsor_org_id ist (denormalisiert vom Account) der
-- Mandanten-Schlüssel für direkten Query-Scope. Zugriff läuft über
-- KontaktPersonService → AccessControl.kannSponsorDatenSehen. Kein CHECK auf
-- kontakt_rolle (Enum KontaktRolle ist Source of Truth, Pattern V44-V47).
--
-- "Primary Contact" (Dynamics primarycontactid) wird über
-- kontakt_rolle = HAUPTANSPRECHPARTNER ausgedrückt — vermeidet die zirkuläre
-- FK account↔contact. Business-Regel "max. ein Hauptkontakt pro Account" liegt
-- im Service, nicht als DB-Constraint.
-- =============================================================================

CREATE TABLE kontakt_person (
    id                       UUID         DEFAULT gen_random_uuid() PRIMARY KEY,
    besitzer_sponsor_org_id  UUID         NOT NULL REFERENCES organisation(id) ON DELETE CASCADE,
    sponsor_account_id       UUID         NOT NULL REFERENCES sponsor_account(id) ON DELETE CASCADE,
    vorname                  VARCHAR(120) NOT NULL,
    nachname                 VARCHAR(120) NOT NULL,
    funktion                 VARCHAR(160),
    kontakt_rolle            VARCHAR(30)  NOT NULL DEFAULT 'SONSTIGE',
    email                    VARCHAR(255),
    telefon                  VARCHAR(60),
    mobile                   VARCHAR(60),
    notiz                    TEXT,
    erstellt_am              TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    aktualisiert_am          TIMESTAMP
);

CREATE INDEX idx_kontakt_person_account  ON kontakt_person(sponsor_account_id);
CREATE INDEX idx_kontakt_person_besitzer ON kontakt_person(besitzer_sponsor_org_id);
