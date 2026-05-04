# Datenmodell

## Aktueller Stand

| Migration | Inhalt | Phase |
|---|---|---|
| V1 | Schema-Baseline (Marker-Tabelle) | 0 |
| V2 | `organisation` | 0.1 ✓ |
| V3 | `app_user` + `mitgliedschaft` | 0.2 ✓ |
| V4 | `email_verifizierung` (Felder auf app_user) | 1.2 ✓ |
| V5 | `projekt` + `sponsoring_paket` | **2 (aktuell)** |

## V2 — Organisation

Eine `Organisation` ist die Wurzel-Entität für Vereine und Sponsor-Unternehmen. Im **kollaborativen Modell** (siehe `ROLLENKONZEPT.md`) ist sie der Edit-Marker für Daten — keine strikte Mandantentrennung.

### Tabelle `organisation`

| Feld | Typ | NULL? | Beschreibung |
|---|---|:---:|---|
| `id` | UUID | – | PK |
| `typ` | VARCHAR(20) | – | ENUM (`VEREIN`, `UNTERNEHMEN`, `STIFTUNG`, `ANDERE`) |
| `name` | VARCHAR(255) | – | Anzeigename |
| `slug` | VARCHAR(120) | – | URL-freundlich, UNIQUE |
| `rechtsform` | VARCHAR(50) | ✓ | z.B. „Verein", „AG", „GmbH", „e.V." |
| `branche` | VARCHAR(50) | ✓ | `SPORT`, `KULTUR`, `SOZIALES`, `BILDUNG`, `UMWELT`, `WIRTSCHAFT`, `ANDERE` |
| `beschreibung` | TEXT | ✓ | öffentliche Beschreibung |
| `website_url` | VARCHAR(500) | ✓ | |
| `status` | VARCHAR(20) | – | ENUM (`PENDING`, `VERIFIED`, `ACTIVE`, `SUSPENDED`); Default `PENDING` |
| `verifiziert_am` | TIMESTAMPTZ | ✓ | wenn Plattform-Admin verifiziert hat |
| `zefix_uid` | VARCHAR(20) | ✓ | UID nach Auto-Verifizierung (Phase 1.6) |
| `registriert_am` | TIMESTAMP | – | Default `now()` |
| `created_at` | TIMESTAMP | – | Default `now()` |
| `updated_at` | TIMESTAMP | – | Default `now()` |

### Constraints

- `slug` UNIQUE
- `name` ≥ 2 Zeichen (Service-Validierung; nicht DB-Constraint, weil flexibel)
- `slug` matched Regex `[a-z0-9-]+` (Service-Validierung)

### Indizes

- `slug` UNIQUE-Index (automatisch)
- `status` Index für Plattform-Admin-Verifizierungs-Queue
- `typ` Index für Filter

### Slug-Generator

`SlugGenerator.fromName(name)` erzeugt URL-tauglichen Slug:
- Umlaute: `ä → ae`, `ö → oe`, `ü → ue`, `ß → ss`
- alles in Kleinbuchstaben
- Leerzeichen → `-`
- alle nicht-`[a-z0-9-]` werden entfernt
- Mehrfach-`-` zu einem reduziert
- führende/abschließende `-` entfernt
- Beispiele:
  - `"FC Beispiel Zürich"` → `"fc-beispiel-zuerich"`
  - `"Verein für Sport & Kultur"` → `"verein-fuer-sport-kultur"`

### Service-Verantwortung

`OrganisationService` bietet:
- `alle()` → `List<Organisation>` sortiert nach `name`
- `findeNachId(UUID id)` → `Optional<Organisation>`
- `findeNachSlug(String slug)` → `Optional<Organisation>`
- `speichere(OrganisationFormDto dto)` → erzeugt oder aktualisiert; Slug aus Name generiert wenn leer; wirft `IllegalArgumentException` bei Slug-Konflikt
- `loesche(UUID id)` → wirft `IllegalStateException` falls Org nicht gelöscht werden darf (Phase 0.2: wenn Mitgliedschaften vorhanden)

### Spätere Phasen

- **V4** (Phase 2): `projekt`, `sponsoring_paket`, `sponsor_beteiligung`
- **V5** (Phase 1): zefix_uid wird durch Auto-Verifizierung gefüllt

---

## V3 — AppUser & Mitgliedschaft

### Tabelle `app_user`

Plattform-Benutzer-Konto. Passwort wird nie im Klartext gespeichert (BCrypt).

| Feld | Typ | NULL? | Beschreibung |
|---|---|:---:|---|
| `id` | UUID | – | PK |
| `email` | VARCHAR(255) | – | Login-Identifier; UNIQUE |
| `passwort_hash` | VARCHAR(255) | – | BCrypt-Hash; niemals Klartext |
| `anzeigename` | VARCHAR(100) | – | Öffentlicher Name |
| `platform_rolle` | VARCHAR(30) | ✓ | `PLATFORM_ADMIN`, `PLATFORM_MODERATOR`, `PLATFORM_SUPPORT`; NULL = normaler Nutzer |
| `aktiv` | BOOLEAN | – | Default `true`; `false` = gesperrt |
| `registriert_am` | TIMESTAMP | – | Default `now()` |
| `created_at` | TIMESTAMP | – | Default `now()` |
| `updated_at` | TIMESTAMP | – | Default `now()` |

#### Constraints

- `email` UNIQUE
- `email` gültige E-Mail-Adresse (Service-Validierung)
- `anzeigename` ≥ 2 Zeichen (Service-Validierung)

#### Indizes

- `email` UNIQUE-Index (automatisch)
- `platform_rolle` Index für Admin-Queries

---

### Tabelle `mitgliedschaft`

Verknüpft einen `app_user` mit einer `organisation` und weist ihm eine Rolle zu. Pro User–Org-Paar ist genau ein Eintrag erlaubt.

| Feld | Typ | NULL? | Beschreibung |
|---|---|:---:|---|
| `id` | UUID | – | PK |
| `user_id` | UUID | – | FK → `app_user(id)` ON DELETE CASCADE |
| `org_id` | UUID | – | FK → `organisation(id)` ON DELETE CASCADE |
| `rolle` | VARCHAR(20) | – | ENUM (`ORG_OWNER`, `ORG_EDITOR`, `ORG_VIEWER`) |
| `eingeladen_von` | UUID | ✓ | FK → `app_user(id)` ON DELETE SET NULL |
| `beigetreten_am` | TIMESTAMP | – | Default `now()` |

#### Constraints

- `UNIQUE (user_id, org_id)` — ein User kann pro Org nur eine Rolle haben
- CHECK: `rolle IN ('ORG_OWNER','ORG_EDITOR','ORG_VIEWER')`

#### Indizes

- `(user_id, org_id)` UNIQUE-Index (automatisch)
- `org_id` Index für Mitglieder-Listen-Queries

---

### Service-Verantwortung

**`AppUserService`** bietet:
- `registriere(AppUserFormDto dto)` → legt User an, hasht Passwort; wirft `IllegalArgumentException` bei doppelter E-Mail
- `findeNachEmail(String email)` → `Optional<AppUser>`
- `findeNachId(UUID id)` → `Optional<AppUser>`

**`MitgliedschaftService`** bietet:
- `fuegeHinzu(UUID orgId, UUID userId, Rolle rolle, UUID eingeladenVonId)` → wirft `IllegalStateException` falls Kombination org/user bereits existiert
- `entferne(UUID mitgliedschaftId, Authentication auth)` → nur ORG_OWNER oder PLATFORM_ADMIN
- `findeNachOrg(UUID orgId)` → `List<Mitgliedschaft>`

**`AccessControl`-Bean** (siehe `ROLLENKONZEPT.md`):
- `kannOrgEditieren(UUID orgId, Authentication auth)` → true für ORG_EDITOR, ORG_OWNER, PLATFORM_ADMIN
- `kannOrgVerwalten(UUID orgId, Authentication auth)` → true für ORG_OWNER, PLATFORM_ADMIN

## Migrations-Strategie

- Versionierte Flyway-Migrationen unter `src/main/resources/db/migration/V*.sql`
- Jede Migration **additiv**, niemals destruktiv ändern
- Bei Spalten-Umbenennung: neue Spalte + Backfill + alte droppen in nächster Version
- Vor Deployment auf prod: gegen Prod-Schnappschuss im Staging testen
- `ddl-auto=validate` in beiden Profilen — Hibernate prüft, dass das Schema zur Annotation passt
