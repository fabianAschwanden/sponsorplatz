# Datenmodell

## Aktueller Stand

| Migration | Inhalt | Phase |
|---|---|---|
| V1 | Schema-Baseline (Marker-Tabelle) | 0 |
| V2 | `organisation` | 0.1 ✓ |
| V3 | `app_user` + `mitgliedschaft` | 0.2 ✓ |
| V4 | `email_verifizierung` (Felder auf app_user) | 1.2 ✓ |
| V5 | `projekt` + `sponsoring_paket` | 2 ✓ |
| V6 | `sponsoring_anfrage` | 2 ✓ |
| V7 | `watchlist` | 2 ✓ |
| V8 | `einladung` | 1.2 ✓ |
| V9 | `einladung_idempotenz` | 1.2 ✓ |
| V10 | `volltextsuche_indizes` | 2 ✓ |
| V11 | `medien_asset` | 2 ✓ |
| V12 | `branche` Health-Fokus (NOT NULL + CHECK) | 3 ✓ |
| V13 | `nachricht` (Inbox-Thread an `sponsoring_anfrage`) | 4 ✓ |
| V14 | `audit_log` | 5+ ✓ |
| V15 | `plattform_einstellungen` (Singleton — SMTP-Settings) | 5+ ✓ |
| V16 | `vertrag` (Sponsoring-Vertrag aus angenommener Anfrage) | 5+ ✓ |
| V17 | `rechnung` + `iban` auf Organisation (Swiss QR-Bill) | 5+ ✓ |
| V18 | Profil-Felder auf `app_user` (`profilbild_id`, `sprache`, `telefon`, `bio`, `position_titel`, `ort`, `website_url`) | 6 ✓ |
| V19 | `benachrichtigung` (In-App-Notifications) | 5+ ✓ |
| V20 | Passwort-Reset Token-Felder auf `app_user` | 5+ ✓ |
| V21 | `feature_backlog` (interner Ideen-Tracker) | 5+ ✓ |
| V23 | `medien_asset` CHECK-Constraints um `USER`/`PROFILBILD` erweitern | 6 ✓ |
| V24 | `uebergeordnete_org_id` Self-Ref + Hierarchie | 5+ ✓ |
| V25 | `sponsor_branche` getrennt von `branche` (Verein-Achse vs. Unternehmen-Industrie) | 5+ ✓ |
| V26 | `event` (Vereins-Events) | 9.3 ✓ |
| V27 | Backlog-Item für OIDC-Anbindung (Daten-Seed) | 1.4 ✓ |
| V28 | `federierte_identitaet` (OIDC-Subject-Mapping) | 1.4 ✓ |
| V29 | `sponsoring_anfrage`-Status-Cleanup (IN_PRUEFUNG/ZURUECKGEZOGEN entfernt) | 5+ ✓ |
| V30 | `sponsoring_anfrage.paket_id` nullable + `betreff`-Spalte (Kontakt-Anfrage) | 11 ✓ |
| V31 | `app_user.onboarding_gesehen` (Wizard-State) | 11 ✓ |
| V32 | `sponsoring_anfrage.erstellt_von_id` (Audit-Spur) | 11 ✓ |
| V33 | `sponsoring_anfrage.wunsch_betrag_chf` (Kontakt-Anfrage Richtbetrag) | 11 ✓ |
| V34 | `rechnung.storno_grund` | 11.11 ✓ |
| V35 | `vertrag.gekuendigt_am` + `kuendigungs_grund` (State-Machine sauber) | 11.11 ✓ |
| V36 | `aufgaben_definition` + `aufgabe` (customizable Task-Engine, fünf System-Seed-Defs) | 12 ✓ |
| V37 | `plattform_einstellungen.aktiver_style` (Style-Switcher: default ↔ css-ch) | 11 ✓ |
| V38 | `aufgabe` Link-Template-Felder zur `/meine-anfragen`-Sicht | 12 ✓ |
| V39 | Backlog-Seed: 2FA (Priorität HOCH) | 13.2 ✓ |
| V40 | Backlog-Seed: A11y-Smoke für authentifizierte Seiten | 13.1 ✓ |
| V41 | `audit_log.umgebung` (Cross-Cloud-Sync-Schutz, NOT NULL DEFAULT 'unknown') + Index | 15.3 ✓ |
| V42 | Backfill `audit_log.umgebung='unknown' → 'oci-staging-free'` (Pre-Multi-Cloud-Daten) | 15.3 ✓ |

> **Hinweis:** V22 wurde reserviert für die Postgres-`tsvector`-Migration (Phase 5+). Die Datei ist für H2 nicht relevant und liegt nur als Postgres-spezifische Variante vor (siehe TECHNISCHE_SPEZIFIKATION.md → Volltextsuche).

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
| `branche` | VARCHAR(50) | – | **Health-Fokus, NOT NULL ab V12, CHECK-Constraint:** `SPORT`, `BEWEGUNG`, `REHA`, `BEHINDERTENSPORT`, `SENIORENSPORT`, `PRAEVENTION`, `MENTAL_HEALTH`, `ERNAEHRUNG`, `WELLNESS`, `SELBSTHILFE`, `PATIENTENORGANISATION` |
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
- `branche` Index für Marktplatz-Filter nach Health-Branche (V12)

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

## Domain-Tabellen-Übersicht (V5–V30)

Die untenstehenden Tabellen sind ab V5 stabil im Code; Field-Detail-Dokumentation siehe Java-Entity-Klassen unter `ch.sponsorplatz.*`. Hier nur das Mapping Tabelle ↔ Entity ↔ Bounded Context.

| Tabelle | Entity | Bounded Context | Kurzbeschreibung |
|---|---|---|---|
| `organisation` | `Organisation` | `organisation/` | Verein/Unternehmen/Stiftung. Enthält `branche` (Verein-Health-Achse), `sponsor_branche` (Unternehmen-Industrie, V25), `uebergeordnete_org_id` (Hierarchie, V24), `iban` + Adresse (V17), `zefix_uid`. |
| `app_user` | `AppUser` | `benutzer/` | Konto. Profil-Felder ab V18 (`profilbild_id`, `sprache`, `telefon`, `bio`, `position_titel`, `ort`, `website_url`), Passwort-Reset (V20), E-Mail-Verifizierung (V4). |
| `mitgliedschaft` | `Mitgliedschaft` | `organisation/` | User ↔ Org mit `rolle` (ORG_OWNER/ORG_EDITOR/ORG_VIEWER). |
| `federierte_identitaet` | `FederierteIdentitaet` | `benutzer/` | OIDC-Subject pro App-User für SSO-Login (V28). Siehe `AUTH_SSO_OIDC.md`. |
| `einladung` | `Einladung` | `einladung/` | Token-basierte Einladung in eine Org, 7 Tage gültig (V8). |
| `projekt` | `Projekt` | `projekt/` | Sponsoring-Projekt einer Org. `sichtbarkeit` ENTWURF/OEFFENTLICH/ARCHIVIERT, `slug`, Kategorie, Ort, Datum-Range. |
| `sponsoring_paket` | `SponsoringPaket` | `projekt/` | Paket innerhalb eines Projekts (Name, Preis, Sortierung, aktiv). |
| `medien_asset` | `MedienAsset` | `projekt/` | Bild-/Datei-Upload, polymorph via `entity_typ` (PROJEKT/ORGANISATION/USER) + `asset_typ` (COVER/GALERIE/PITCH_DECK/LOGO/PROFILBILD/ANHANG). |
| `watchlist` | `WatchlistEintrag` | `projekt/` | User merkt Projekt vor (UNIQUE user+projekt). |
| `event` | `Event` | `projekt/` | Vereins-Event mit Datum, Ort, Kapazität (V26). |
| `sponsoring_anfrage` | `SponsoringAnfrage` | `anfrage/` | Anfrage mit `status` (NEU/ANGENOMMEN/ABGELEHNT — V29-Cleanup), `paket_id` jetzt nullable (V30) für Kontakt-Anfragen, dann statt Paket-Bezug ein `betreff`. |
| `nachricht` | `Nachricht` | `anfrage/` | Konversations-Thread zu einer angenommenen Anfrage (V13). |
| `vertrag` | `Vertrag` | `anfrage/` | Sponsoring-Vertrag, generiert aus angenommener Anfrage (V16); PDF-fähig. |
| `rechnung` | `Rechnung` | `anfrage/` | QR-Bill-Rechnung aus Vertrag (V17), `status` OFFEN/BEZAHLT/STORNIERT. |
| `benachrichtigung` | `Benachrichtigung` | `benachrichtigung/` | In-App-Glocke (V19), Typen NEUE_ANFRAGE/ANGENOMMEN/ABGELEHNT/NEUE_NACHRICHT/MITGLIED_HINZUGEFUEGT/ORG_VERIFIZIERT/ORG_SUSPENDIERT/EINLADUNG_ERHALTEN/SYSTEM. |
| `audit_log` | `AuditLog` | `audit/` | Plattform-Aktionen (V14), async befüllt. Spalte `umgebung` (V41, NOT NULL) trägt die Quell-Umgebung — `lokal` / `oci-staging-free` / `azure-staging` — zur Auflösung der Mehrdeutigkeit nach Cross-Cloud-DB-Sync. AuditService liest `sponsorplatz.umgebung` und schreibt sie pro Insert. |
| `feature_backlog` | `FeatureBacklog` | `admin/` | Interner Ideen-Tracker (V21), V27 seedet OIDC-Item. |
| `plattform_einstellungen` | `PlattformEinstellungen` | `shared/einstellungen/` | Singleton-Row mit SMTP-Settings (V15) — DB > ENV > leer. |
| `aufgaben_definition` | `AufgabenDefinition` | `aufgabe/` | „Workflow-Vorlage" (V36, Phase 12): `trigger_entity_typ` ENUM (ORG/ANFRAGE/VERTRAG/RECHNUNG/PROJEKT), `trigger_status` + optional `ziel_status` (freie Strings, weil pro Domain-Aggregat eigene Status-Enums), `assignee_regel` ENUM (PLATFORM_ADMIN, ORG_MITGLIEDER, ANFRAGE_EMPFAENGER_ORG, ANFRAGE_ANFRAGENDER_ORG, VERTRAG_VEREIN_ORG, VERTRAG_SPONSOR_ORG, RECHNUNG_VEREIN_ORG), `aktiv` + `system_definition` (V36-Seeds nicht löschbar). |
| `aufgabe` | `Aufgabe` | `aufgabe/` | Instanz einer Definition (V36): polymorphe Entity-Referenz via `entity_typ` + `entity_id` (kein FK — typunabhängig), `status` OFFEN/ERLEDIGT/ENTFALLEN, Sichtbarkeit via `assignee_org_id` (alle Org-Mitglieder) oder `nur_platform_admin=true`. |

### `aufgabe` + `aufgaben_definition` — generische Task-Engine (V36)

- **Definition** ist die customizable Vorlage: ein Admin pflegt im UI an, dass ein Status-Wechsel eines bestimmten Entity-Typs (`trigger_entity_typ`) auf einen Status-Wert (`trigger_status`) eine Aufgabe erzeugen soll, und optional dass ein anderer Status-Wert (`ziel_status`) sie als ERLEDIGT markiert.
- **Aufgabe** ist die Instanz für ein konkretes Domain-Aggregat. `entity_typ` + `entity_id` ist polymorph — bewusst kein FK, weil die Tabelle auf 5 verschiedene Aggregate zeigt; integritätsschutz auf Engine-Seite (Soft-Delete bzw. CASCADE auf `assignee_org_id` reicht).
- **System-Defs** (`system_definition=true`, V36-Seed): Trigger-Felder im Admin-UI gesperrt, damit die im Code verdrahteten Service-Trigger nicht ins Leere zeigen. Anzeige-Text + `aktiv` + `link_template` bleiben editierbar.
- **Idempotenz**: Engine prüft `existsByDefinitionIdAndEntityIdAndStatus(OFFEN)` vor jedem Save — doppelte Trigger-Aufrufe (z.B. Status setzen + danach setzen mit gleichem Wert) erzeugen keine Duplikate.

### `sponsoring_anfrage` — zwei Anfrage-Typen ab V30

- **Paket-Anfrage** (klassisch, Sponsor → Verein): `paket_id` gesetzt, `betreff` = NULL. Erstellt via Marktplatz-Detail-Klick auf ein Paket.
- **Kontakt-Anfrage** (Verein → Sponsor): `paket_id` = NULL, `betreff` gesetzt. Erstellt via `/anfragen/neu-kontakt` durch ein Vereins-Mitglied; Empfänger ist eine Org vom Typ UNTERNEHMEN.

Im View-DTO unterscheidet `AnfrageView.istPaketAnfrage()`. Vertrag-/Konversations-Aktionen sind nur für Paket-Anfragen relevant.

## Migrations-Strategie

- Versionierte Flyway-Migrationen unter `src/main/resources/db/migration/V*.sql`
- Jede Migration **additiv**, niemals destruktiv ändern
- Bei Spalten-Umbenennung: neue Spalte + Backfill + alte droppen in nächster Version
- Vor Deployment auf prod: gegen Prod-Snapshot im Staging testen
- `ddl-auto=validate` in beiden Profilen — Hibernate prüft, dass das Schema zur Annotation passt
- V29 zeigt das Pattern für Status-Bereinigung: bestehende Daten erst migrieren (UPDATE), dann CHECK-Constraint neu setzen — keine destruktive Spalten-Änderung
