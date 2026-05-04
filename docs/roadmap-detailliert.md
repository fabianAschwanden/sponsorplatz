# Umsetzungsplan — von der `sponsoren-app` zur Sponsoring-Plattform

**Version:** 1.0
**Bezug:** `00_Konzept_v3_Kollaborative-Plattform.md`, `05_Rollenkonzept.md`
**Codebasis:** `~/Documents/SCA - Sponsoring/Sponsoring-new/sponsoren-app`
**Methodik:** TDD-konform — jeder Schritt folgt **Spec → Test → Impl** wie in `.instructions.md` der bestehenden App festgelegt.

---

## Inhaltsverzeichnis

1. [Plan-Übersicht](#1-plan-übersicht)
2. [Pre-Flight: Vorbereitung](#2-pre-flight-vorbereitung)
3. [Phase 0 — Organisations-Profil & Mitgliedschaften](#3-phase-0--organisations-profil--mitgliedschaften-2-wochen)
4. [Phase 1 — Selbstregistrierung & Verifizierung](#4-phase-1--selbstregistrierung--verifizierung-2-wochen)
5. [Phase 2 — Sponsoring-Pakete, Sichtbarkeit & Medien](#5-phase-2--sponsoring-pakete-sichtbarkeit--medien-3-wochen)
6. [Phase 3 — Marktplatz Public](#6-phase-3--marktplatz-public-3-4-wochen)
7. [Phase 4 — Anfragen & Konversation](#7-phase-4--anfragen--konversation-3-wochen)
8. [Phase 5 — Wachstum](#8-phase-5--wachstum-laufend)
9. [Gesamtaufwand & Kritischer Pfad](#9-gesamtaufwand--kritischer-pfad)
10. [Begleitende Aktivitäten](#10-begleitende-aktivitäten)
11. [Rückfall-Strategien](#11-rückfall-strategien)

---

## 1. Plan-Übersicht

```
Pre-Flight ──→ Phase 0 ──→ Phase 1 ──→ Phase 2 ──→ Phase 3 ──→ Phase 4 ──→ Phase 5
   1 W           2 W          2 W         3 W        3-4 W        3 W       offen
                Multi-Org   Self-Reg     Pakete    Marktplatz   Anfragen   Wachstum
                            +Verifik.    +Medien    +SEO        +Konvers.
```

**Iterations-Prinzip:**
Jede Iteration liefert lauffähigen, getesteten Code. Master/Main bleibt jederzeit deploybar. Feature-Branches ≤ 5 Tage Lebensdauer. Pull-Requests werden gegen die `.instructions.md`-Standards geprüft (Clean Code, TDD, Spec-Update).

**Definition of Done (pro Iteration):**
- [ ] Spec in `specs/` aktualisiert
- [ ] Tests geschrieben **und** vorher rot gewesen
- [ ] `mvn test` grün (alle Tests, nicht nur neue)
- [ ] Code-Review durchgeführt
- [ ] Migrations-Skript versioniert (Flyway)
- [ ] Funktionalität in Staging deployt
- [ ] Smoke-Test im Browser durchgeführt
- [ ] `README.md` und `.instructions.md` synchron

---

## 2. Pre-Flight: Vorbereitung

**Aufwand:** 3–5 Tage
**Ziel:** Saubere Ausgangslage, keine technischen Blocker während der Plattform-Erweiterung.

### Pre-Flight-1: Java 21 LTS Upgrade

- **Warum:** Records, Sealed Interfaces, Pattern Matching, Virtual Threads. Bringt deutlichen Code-Vorteil bei DTOs und Permission-Checks.
- **Wie:** `pom.xml` `java.version` auf 21, CI auf 21 umstellen, Build & Tests laufen lassen.
- **Risiko:** Gering bei Spring Boot 3.3.4 (offiziell unterstützt).
- **Tests:** Bestehende 24 Test-Klassen müssen grün bleiben.
- **Definition of Done:** App startet, alle Tests grün, Docker-Image mit JRE 21.

### Pre-Flight-2: Lokales Plattform-Repo aus `sponsoren-app` heraus

- Diskussion: Bleibt der Code in `sponsoren-app` oder wird er ein neues Modul / neues Repo?
  - **Empfehlung:** Bleibt im selben Repo. Plattform ist Evolution, kein Neustart. Branch-Strategie: `main` bleibt stabil, neue Features via `feature/plattform-*`.
  - Optional: Maven Multi-Module später, falls Code-Größe es rechtfertigt. Nicht jetzt.

### Pre-Flight-3: Issue-Tracker einrichten

- GitHub Issues mit Labels: `phase-0`, `phase-1`, …, `iteration-X.Y`, `spec`, `test`, `impl`, `chore`.
- Milestone pro Phase.
- Issue-Template mit Spec-Referenz.

### Pre-Flight-4: Staging-Environment validieren

- OCI-Staging muss aktuell und funktionsfähig sein (`infra/envs/staging`).
- Smoke-Test der bestehenden App auf Staging — wenn sie dort läuft, läuft auch jede neue Migration sauber.

### Pre-Flight-5: Backup-Strategie für Production-DB

- Vor Phase-0-Migration V8: Snapshot der Prod-DB sichern.
- Restore-Test im Staging dokumentieren.
- Rollback-Skripte für V8/V9 schon im Pre-Flight schreiben (auch wenn nicht benötigt).

---

## 3. Phase 0 — Organisations-Profil & Mitgliedschaften (2 Wochen)

**Ziel:** Bestehende App wird "organisations-aware". Für SCA verändert sich nichts — alle Daten gehören weiterhin SCA, alle Tests bleiben grün, alle Workflows funktionieren.

### Iteration 0.1 — `Organisation`-Entity einführen (2 Tage)

**Spec-Update:** Neue Datei `specs/PLATTFORM_DATENMODELL.md` anlegen. Erweitert `DATENMODELL.md` um Organisation-Konzept. Dokumentiert `Organisation`-Entity, Felder, Status-Werte.

**Tests (red):**
- `OrganisationRepositoryTest` — `@DataJpaTest`, prüft Persistierung, Slug-Uniqueness, Default-Status `PENDING`
- `OrganisationServiceTest` — Unit-Tests für CRUD-Operationen
- `DatabaseInitializerOrganisationTest` — beim Startup wird SCA-Org automatisch angelegt (idempotent)

**Implementation (green):**
- Neue Klassen: `model/Organisation.java`, `model/OrgTyp.java` (Enum), `model/OrgStatus.java` (Enum)
- `repository/OrganisationRepository.java`
- `service/OrganisationService.java`
- Erweiterung `startup/DatabaseInitializer.java` — SCA-Organisation seeden mit fixer UUID
- Flyway-Migration `V8__create_organisation.sql`:
  ```sql
  CREATE TABLE organisation (
      id UUID PRIMARY KEY,
      typ VARCHAR(20) NOT NULL,
      name VARCHAR(255) NOT NULL,
      slug VARCHAR(120) UNIQUE NOT NULL,
      rechtsform VARCHAR(50),
      branche VARCHAR(50),
      beschreibung TEXT,
      website_url VARCHAR(500),
      logo_asset_id UUID,
      status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
      verifiziert_am TIMESTAMPTZ,
      zefix_uid VARCHAR(20),
      registriert_am TIMESTAMPTZ NOT NULL DEFAULT now(),
      created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
      updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
  );
  CREATE INDEX idx_organisation_status ON organisation(status);
  CREATE INDEX idx_organisation_typ ON organisation(typ);

  INSERT INTO organisation(id, typ, name, slug, status, registriert_am)
  VALUES ('00000000-0000-0000-0000-000000000001',
          'VEREIN', 'SCA Sponsoring', 'sca', 'ACTIVE', now());
  ```

**DoD:** SCA-Org existiert in DB nach Startup, Repository findet sie, alle 24+ Tests grün.

### Iteration 0.2 — `Mitgliedschaft`-Entity & `AccessControl`-Bean (2 Tage)

**Spec-Update:** `specs/ROLLENKONZEPT.md` anlegen — übernimmt die Permission-Matrix aus `05_Rollenkonzept.md` (kompaktere Spec-Variante).

**Tests (red):**
- `MitgliedschaftRepositoryTest` — Basis-CRUD, Unique-Constraint
- `AccessControlTest` — alle `kann…`-Methoden mit Mock-Repo
- `MitgliedschaftServiceTest` — Mitglied einladen, Rolle ändern, Owner-Count-Invariante

**Implementation (green):**
- Neue Klassen: `model/Mitgliedschaft.java`, `model/Rolle.java` (Enum: ORG_OWNER, ORG_EDITOR, ORG_VIEWER)
- `repository/MitgliedschaftRepository.java` mit `existsByUserSubjectAndOrganisationIdAndRolleIn(...)`
- `service/MitgliedschaftService.java`
- `service/security/AccessControl.java` (Spring `@Component("accessControl")`)
- Flyway `V9__create_mitgliedschaft.sql`:
  ```sql
  CREATE TABLE mitgliedschaft (
      id UUID PRIMARY KEY,
      user_subject VARCHAR(255) NOT NULL,
      organisation_id UUID NOT NULL REFERENCES organisation(id) ON DELETE CASCADE,
      rolle VARCHAR(20) NOT NULL,
      eingeladen_von VARCHAR(255),
      created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
      UNIQUE (user_subject, organisation_id, rolle)
  );
  CREATE INDEX idx_mitgliedschaft_user ON mitgliedschaft(user_subject);
  CREATE INDEX idx_mitgliedschaft_org ON mitgliedschaft(organisation_id);

  -- SCA-Mitarbeiter aus OIDC-Subjects als ORG_OWNER seeden
  -- (manuell oder per ENV-Konfiguration; siehe Application-Config)
  ```

**DoD:** `AccessControl.kannOrgEditieren(scaOrgId, fabianAuth)` gibt `true` zurück.

### Iteration 0.3 — Bestehende Entities mit `besitzer_organisation_id` ergänzen (2 Tage)

**Spec-Update:** `specs/PLATTFORM_DATENMODELL.md` erweitern — Migrationsstrategie dokumentieren.

**Tests (red):**
- `SponsorTest`, `ProjektTest`, `SaisonTest`, `EmailVorlageTest`, `KommunikationTest` — neues Feld `besitzerOrganisationId` ist nullable, Default beim Anlegen
- `SponsorRepositoryTest` — bestehende Queries laufen unverändert (kein Lese-Filter!)

**Implementation (green):**
- JPA-Entity-Felder ergänzen: `besitzerOrganisationId` als `UUID` (nicht FK-Mapping nötig — wir wollen kein Eager-Loading)
- Flyway `V10__besitzer_organisation_id.sql`:
  ```sql
  ALTER TABLE sponsoren        ADD COLUMN besitzer_organisation_id UUID;
  ALTER TABLE projekte         ADD COLUMN organisation_id UUID;          -- Eigentum, NOT NULL via Backfill
  ALTER TABLE saisons          ADD COLUMN besitzer_organisation_id UUID;
  ALTER TABLE email_vorlagen   ADD COLUMN besitzer_organisation_id UUID;
  ALTER TABLE kommunikationen  ADD COLUMN besitzer_organisation_id UUID;

  -- Backfill mit SCA
  UPDATE sponsoren        SET besitzer_organisation_id = '00000000-...-0001';
  UPDATE projekte         SET organisation_id          = '00000000-...-0001';
  UPDATE saisons          SET besitzer_organisation_id = '00000000-...-0001';
  UPDATE email_vorlagen   SET besitzer_organisation_id = '00000000-...-0001';
  UPDATE kommunikationen  SET besitzer_organisation_id = '00000000-...-0001';

  ALTER TABLE projekte ALTER COLUMN organisation_id SET NOT NULL;

  CREATE INDEX idx_sponsor_besitzer  ON sponsoren(besitzer_organisation_id);
  CREATE INDEX idx_projekt_org       ON projekte(organisation_id);
  ```

**DoD:** Alle bestehenden Daten haben SCA als Besitzer, alle Tests grün.

### Iteration 0.4 — Bestehende `@PreAuthorize` auf `AccessControl` umstellen (1 Tag)

**Spec-Update:** Routen-Tabelle in `TECHNISCHE_SPEZIFIKATION.md` aktualisieren.

**Tests (red):**
- Erweiterung `ProdSecurityIntegrationTest` — Tests mit Mitgliedschaft-Setup pro User
- Neuer Test `RollenIntegrationTest` — pro Rolle: was geht / was nicht geht

**Implementation (green):**
- In allen Controllern (`SponsorController`, `ProjektController`, `SaisonController`, …) `@PreAuthorize`-Annotationen umstellen:
  ```java
  // VORHER
  @PreAuthorize("hasRole('EDITOR') or hasRole('ADMIN')")

  // NACHHER (für SCA-spezifische Routen, wo orgId aus Kontext kommt)
  @PreAuthorize("@accessControl.kannOrgEditieren(#orgId, authentication)")
  ```
- Wo `orgId` nicht direkt aus Pfad kommt: aus `ProjektService.findeOrgId(projektId)` o.ä. ableiten — Pre-Authorize unterstützt `@beanName.method()`.
- `SecurityConfig` — globale Plattform-Rollen aus OIDC-Gruppen mappen.

**DoD:** SCA-Workflows funktionieren wie vorher, plus: ein neu eingelegter Test-Verein-User hat keinen Zugriff auf SCA-Daten zum Editieren.

### Iteration 0.5 — Org-Auswahl in der UI (für Multi-Org-User) (2 Tage)

**Spec-Update:** UI-Konzept in `specs/PLATTFORM_UI.md` — Header mit Org-Switcher.

**Tests (red):**
- `OrgSwitcherControllerTest` — Wechsel der aktiven Org speichert Session-Attribut
- Template-Test: Header zeigt aktuelle Org-Auswahl

**Implementation (green):**
- `OrgKontextService` — RequestScope-Bean, hält aktuelle Org-Auswahl (Session-Attribut)
- Header-Fragment in `templates/layout.html` — Dropdown mit Mitgliedschaften
- Bei Anlegen neuer Entitäten (Projekt, Sponsor, Paket): Default ist aktuelle Org-Auswahl
- Single-Org-User (z.B. typische Lea): Dropdown unsichtbar oder nur als Anzeige

**DoD:** Marco mit Mitgliedschaften in 2 Orgs kann zwischen ihnen wechseln. Lea mit 1 Org sieht keine Auswahl.

### Iteration 0.6 — Smoke-Test & Phase-0-Abschluss (1 Tag)

- E2E-Smoke-Test im Staging:
  - SCA-User loggt sich ein → sieht alle Sponsoren wie vorher
  - Excel-Export funktioniert
  - Word-Serienbrief funktioniert
  - Datenbereinigung läuft durch
- Manuell zweiten Test-Verein anlegen (per SQL) und Mitgliedschaft hinzufügen → loggt sich ein → kann eigene Daten anlegen, aber SCA-Daten nicht editieren
- Phase-0 als Tag in Git markieren: `phase-0-complete`

**Phase-0-Erfolgsmetrik:**
- ✅ Alle 24+ Tests grün, plus mind. 15 neue Tests
- ✅ SCA-Workflow funktional unverändert
- ✅ Zweite Test-Org lebt parallel ohne Konflikte
- ✅ `AccessControl` deckt alle Permission-Matrix-Zeilen ab

---

## 4. Phase 1 — Selbstregistrierung & Verifizierung (2 Wochen)

**Ziel:** Neue Vereine können sich ohne Hilfe registrieren. Sponsor-Orgs auch. Plattform-Admin verifiziert manuell oder via Zefix automatisch.

### Iteration 1.1 — Local-Identity-Schema & Dual-Auth (3 Tage)

**Hintergrund:** OCI IAM Domains unterstützen Self-Reg nur eingeschränkt. Wir bauen ein lokales User-Schema **zusätzlich** zu OIDC. SCA-Mitarbeiter behalten OIDC-Login.

**Spec-Update:** `specs/AUTHENTIFIZIERUNG.md` — Beide Auth-Pfade dokumentieren.

**Tests (red):**
- `LocalUserRepositoryTest`
- `RegistrierungsServiceTest` — User anlegen, E-Mail-Token, Verifikation
- `DualAuthSecurityTest` — sowohl OIDC- als auch Form-Login funktionieren

**Implementation (green):**
- Tabelle `app_user` (id, email, password_hash BCrypt, email_verified_at, created_at)
- Tabelle `email_token` (token, app_user_id, typ EMAIL_VERIFY/PASSWORT_RESET, expires_at, used_at)
- Flyway `V11__local_identity.sql`
- `service/RegistrierungsService.java`
- `controller/RegistrierungController.java` — `/registrieren`, `/verifizieren/{token}`, `/login`, `/passwort-vergessen`
- `SecurityConfig`-Erweiterung: `formLogin()` parallel zu `oauth2Login()`

**DoD:** Neuer Test-User kann sich anmelden, E-Mail bekommen, verifizieren, einloggen.

### Iteration 1.2 — Verein-Self-Registration-Flow (2 Tage)

**Spec-Update:** `specs/ONBOARDING.md` — Schritt-für-Schritt-Flow.

**Tests (red):**
- `VereinRegistrierungsTest` — kompletter Flow: Form → User + Org + Mitgliedschaft anlegen
- Validierungen: E-Mail eindeutig, Vereinsname Pflicht

**Implementation (green):**
- Multi-Step-Form: (1) E-Mail+Passwort, (2) Vereins-Daten, (3) Bestätigung
- Bei Submit:
  - `app_user` anlegen
  - `organisation` anlegen (typ=VEREIN, status=PENDING)
  - `mitgliedschaft` anlegen (rolle=ORG_OWNER)
  - E-Mail mit Verifizierungs-Token
- Templates: `templates/registrieren/verein.html`

**DoD:** Anonymer User kann komplett durch den Flow, sieht danach „Wir prüfen Deine Anmeldung" und kann nach Verifizierung einloggen + Daten anlegen.

### Iteration 1.3 — Sponsor-Org-Self-Registration-Flow (1 Tag)

Analog zu 1.2, aber für `typ=UNTERNEHMEN` (oder STIFTUNG/ANDERE).

### Iteration 1.4 — Org-Profil-Edit-Page (1 Tag)

**Spec-Update:** `specs/PLATTFORM_UI.md` erweitern.

**Tests (red):**
- Permission: nur ORG_OWNER kann editieren

**Implementation (green):**
- `OrgProfilController` — `/organisation/{slug}/profil`
- Formular: Logo-Upload, Beschreibung (Markdown), Website, Branche

**DoD:** ORG_OWNER kann Profil ändern, ORG_EDITOR sieht „nur lesen", anonymer Marktplatz-Besucher sieht das Profil unter `/marktplatz/organisationen/{slug}` (ggf. erst aktiv ab Phase 3).

### Iteration 1.5 — Plattform-Admin-Verifizierungs-Queue (2 Tage)

**Spec-Update:** `specs/VERIFIZIERUNG.md` neu.

**Tests (red):**
- `PlattformAdminControllerTest` — Liste sortiert nach `registriert_am DESC`, nur PLATFORM_ADMIN sieht es
- `VerifizierungsServiceTest` — Verifizieren / Ablehnen löst E-Mail aus, setzt Status

**Implementation (green):**
- `controller/PlattformAdminController.java` — `/admin/verifizierung`
- Liste der `PENDING` Orgs, Detail-View, Aktionen
- Audit-Event `ORG_VERIFIZIERT` / `ORG_ABGELEHNT`
- E-Mail an alle ORG_OWNER der Org

**DoD:** Plattform-Admin sieht alle pending Vereine, kann verifizieren, Verein bekommt E-Mail.

### Iteration 1.6 — Auto-Verifizierung via Zefix (2 Tage)

**Hintergrund:** Bestehender `ZefixClient` aus `service/cleanup/` wird für Verein-Verifizierung genutzt.

**Spec-Update:** `specs/VERIFIZIERUNG.md` ergänzen — Zefix-Pfad.

**Tests (red):**
- `AutoVerifizierungServiceTest` — bei UID-Match → Status VERIFIED + zefix_uid gesetzt

**Implementation (green):**
- `service/AutoVerifizierungService.java` (orchestriert ZefixClient)
- Trigger: nach E-Mail-Verifikation läuft Zefix-Lookup async (Spring `@Async`)
- Bei Match: Status VERIFIED, `verifiziert_am`, `zefix_uid` gesetzt
- Bei kein Match: bleibt PENDING, Plattform-Admin entscheidet

**DoD:** Test mit echter Schweizer Vereins-UID → automatisch verifiziert.

### Iteration 1.7 — Mitglieder-Einladungs-Flow (2 Tage)

**Spec-Update:** `specs/MITGLIEDER.md` neu.

**Tests (red):**
- `EinladungsServiceTest` — Token, Verfall, Annahme
- Owner-Count-Invariante: letzter Owner kann sich nicht entfernen

**Implementation (green):**
- Tabelle `einladung` (token, organisation_id, email, rolle, expires_at, accepted_at)
- `controller/MitgliederController.java` — `/organisation/{slug}/mitglieder`
- Aktionen: einladen, Rolle ändern, entfernen
- E-Mail mit Einladungs-Link
- Bei Klick auf Link: User loggt sich ein (oder registriert sich), Mitgliedschaft wird angelegt
- Audit-Events

**DoD:** Lea kann Marco einladen → Marco bekommt E-Mail → klickt → ist ORG_EDITOR bei SCA.

**Phase-1-Erfolgsmetrik:**
- ✅ Mind. 1 Test-Verein und 1 Test-Sponsor-Org via Self-Reg angelegt
- ✅ Plattform-Admin-Workflow funktioniert
- ✅ Zefix-Auto-Verifizierung wird ausgelöst und führt bei valider UID zum VERIFIED-Status

---

## 5. Phase 2 — Sponsoring-Pakete, Sichtbarkeit & Medien (3 Wochen)

**Ziel:** Vereine können Projekte mit strukturierten Sponsoring-Paketen ausstatten und Medien (Pitch-Decks, Bilder) hochladen. Veröffentlichungs-Logik vorhanden — Marktplatz folgt in Phase 3.

### Iteration 2.1 — Projekt-Sichtbarkeit & Slug (2 Tage)

**Spec-Update:** `specs/PROJEKTE.md` erweitern — neue Felder.

**Tests (red):**
- `ProjektTest` — Sichtbarkeit-Default, Slug-Generierung, Slug-Eindeutigkeit
- `SlugGeneratorTest` — Name+Datum → URL-tauglicher Slug

**Implementation (green):**
- Felder auf `Projekt`: `slug`, `sichtbarkeit` (Enum), `veroeffentlichtAm`, `branche`, `erwarteteBesucher`, `zielgruppe`, `finanzierungszielChf`
- Flyway `V12__projekt_sichtbarkeit.sql`
- `service/SlugGenerator.java` (Kebab-Case, ASCII-Konvertierung)
- Bestehende Projekte: Slugs werden migriert beim Backfill (`UPDATE projekte SET slug = …`)

**DoD:** Bestehende SCA-Projekte haben Slugs, neue Felder sind verwendbar.

### Iteration 2.2 — `SponsoringPaket`-Entity & CRUD (3 Tage)

**Spec-Update:** `specs/SPONSORING_PAKETE.md` neu.

**Tests (red):**
- `SponsoringPaketRepositoryTest`, `SponsoringPaketServiceTest`
- Constraint: `quantity_taken <= quantity_total`

**Implementation (green):**
- Entity, Repository, Service
- `controller/SponsoringPaketController.java` — eingebettet in Projekt-Detail-Page
- Templates: Inline-Pakete-Verwaltung
- Flyway `V13__sponsoring_paket.sql`

**DoD:** Verein kann Pakete anlegen, bearbeiten, deaktivieren.

### Iteration 2.3 — Migration: Bestehende Kategorien → Standard-Pakete (1 Tag)

**Spec-Update:** Migrations-Strategie dokumentieren.

**Tests (red):**
- `KategorieZuPaketMigrationTest` — bestehende SCA-Projekte bekommen Standard-Pakete (BRONZE/SILBER/GOLD) basierend auf historischen Kategorien

**Implementation (green):**
- Einmalige Migration in V13 oder als Spring-Component beim Startup
- Per Projekt: für jede Kategorie mit bestehenden Beteiligungen ein Standard-Paket anlegen
- Beteiligungen bekommen `paket_id`-Referenz

**DoD:** Alle SCA-Projekte haben rückwirkend Pakete, Beteiligungen sind verlinkt.

### Iteration 2.4 — `MedienAsset`-Entity & FileStorage-Anbindung (3 Tage)

**Spec-Update:** `specs/MEDIEN.md` neu.

**Tests (red):**
- `MedienAssetRepositoryTest`
- `MedienServiceTest` — Upload via FileStorage, Metadaten persistieren, MIME-Validierung

**Implementation (green):**
- Entity, Repository, Service
- Bestehender `FileStorage` (Local/OCI Object Storage) wird Backend
- Validierung: MIME-Whitelist (JPG/PNG/WebP/PDF), Größenlimit (10 MB Bild, 25 MB PDF)
- `service/medien/Thumbnailator` für Auto-Thumbnails (Phase optional)
- Flyway `V14__medien_asset.sql`

**DoD:** Datei-Upload funktioniert, Datei landet in Object Storage, Metadaten in DB.

### Iteration 2.5 — Cover-Bild & Galerie & Pitch-Deck im Projekt-Wizard (3 Tage)

**Spec-Update:** `specs/PROJEKTE.md` ergänzen.

**Tests (red):**
- `ProjektWizardTest` — Multi-Step-Form mit Medien-Upload
- Drag&Drop-Reihenfolge der Galerie-Bilder

**Implementation (green):**
- Erweiterte Projekt-Edit-Seite: Wizard-Schritte (Basics → Beschreibung → Pakete → Medien → Veröffentlichung)
- File-Upload via `multipart/form-data`, dann via MedienService persistieren
- Cover-Bild-Auswahl
- Galerie-Reordering
- Pitch-Deck-Slot

**DoD:** Verein lädt Cover-Bild + Pitch-Deck hoch, sieht Vorschau im Edit-Modus.

### Iteration 2.6 — Veröffentlichungs-Flow (1 Tag)

**Spec-Update:** Sichtbarkeits-Übergänge dokumentieren.

**Tests (red):**
- `VeroeffentlichungTest` — Übergang DRAFT → OEFFENTLICH setzt `veroeffentlicht_am`
- Validierung: Pflichtfelder vor Veröffentlichung

**Implementation (green):**
- Button „Veröffentlichen" auf Projekt-Edit-Seite
- Pre-Check: alle Pflichtfelder ausgefüllt? Mind. 1 Paket?
- Status-Wechsel mit Audit-Event

**DoD:** Verein kann ein Projekt nicht veröffentlichen, wenn das Cover-Bild fehlt.

**Phase-2-Erfolgsmetrik:**
- ✅ SCA hat ein Projekt mit ≥3 Paketen, Cover-Bild und Pitch-Deck angelegt
- ✅ Status `OEFFENTLICH` ist erreichbar (Marktplatz folgt in Phase 3)

---

## 6. Phase 3 — Marktplatz Public (3–4 Wochen)

**Ziel:** Anonyme User finden veröffentlichte Projekte auf einer öffentlichen Marktplatz-Seite, können filtern, suchen und Details ansehen.

### Iteration 3.1 — Public-Routes-Skeleton (2 Tage)

**Spec-Update:** `specs/MARKTPLATZ.md` neu.

**Tests (red):**
- `MarktplatzControllerTest` — anonymer Zugriff funktioniert (HTTP 200), nur OEFFENTLICH-Projekte erscheinen
- 404 für Slugs, die nicht existieren oder nicht OEFFENTLICH

**Implementation (green):**
- `controller/MarktplatzController.java` — `/marktplatz`, `/marktplatz/projekte/{slug}`, `/marktplatz/organisationen/{slug}`
- Templates: `templates/marktplatz/liste.html`, `detail.html`, `organisation.html`
- `SecurityConfig`: `/marktplatz/**` als `permitAll()`

**DoD:** Anonymer Browser-Zugriff zeigt veröffentlichte Projekte.

### Iteration 3.2 — Filter-UI (2 Tage)

**Tests (red):**
- `MarktplatzFilterTest` — Branche, Region (PLZ-Bereich), Datum, Budget filtern korrekt

**Implementation (green):**
- Filter-Sidebar im Marktplatz-Listing
- Repository-Methoden mit Spring Data JPA `Specification`
- URL-Parameter erhalten Filter-State (für Sharing)

**DoD:** Filter wirken sich auf Liste aus, URL ist teilbar.

### Iteration 3.3 — Volltextsuche mit PostgreSQL `tsvector` (3 Tage)

**Spec-Update:** Such-Strategie dokumentieren.

**Tests (red):**
- `VolltextsucheTest` — Suche nach Projekttitel, Beschreibung, Vereinsname

**Implementation (green):**
- Flyway `V15__tsvector_search.sql` — generated columns mit `tsvector`, GIN-Index
- Native Query oder Hibernate-Custom-Function für `@@`-Match
- Search-Box auf Marktplatz-Liste

**DoD:** Suche nach „Sommer" findet alle Projekte mit „Sommer" in Titel/Beschreibung.

### Iteration 3.4 — SEO: Sitemap, Schema.org, Open Graph (2 Tage)

**Spec-Update:** `specs/SEO.md` neu.

**Tests (red):**
- `SitemapControllerTest` — XML-Format korrekt, alle OEFFENTLICH-Projekte enthalten
- `SchemaOrgFragmentTest` — Markup-Validität

**Implementation (green):**
- `controller/SeoController.java` — `/sitemap.xml`
- Template-Fragmente für Schema.org (`Event`-Type) und Open Graph (`og:title`, `og:image`, …)
- `robots.txt` als Static-Resource

**DoD:** Google PageSpeed Insights und Schema.org Validator sind grün.

### Iteration 3.5 — Public Vereinsprofile (2 Tage)

**Tests (red):**
- `OrganisationProfilTest` — Marktplatz-Profil zeigt nur Public-Felder

**Implementation (green):**
- Template `templates/marktplatz/organisation.html` — Logo, Beschreibung, Liste veröffentlichter Projekte, Sponsor-Logos
- Org-DTO `OrganisationPublicView` — nur Public-Felder enthalten

**DoD:** SCA-Verein hat eine schöne öffentliche Profil-Page.

### Iteration 3.6 — Performance & Caching (2 Tage)

**Spec-Update:** `specs/PERFORMANCE.md` ergänzen.

**Tests (red):**
- Lasttest mit JMeter / k6: Marktplatz-Liste mit 1000 Projekten < 300 ms

**Implementation (green):**
- Caching der Marktplatz-Liste (Spring Cache, 5 min TTL)
- Cache-Invalidierung bei Projekt-Veröffentlichung
- Lazy-Loading für Bilder im Listing

**DoD:** Lasttest grün, Marktplatz fühlt sich schnell an.

**Phase-3-Erfolgsmetrik:**
- ✅ Anonymer User browst Marktplatz, sieht SCA-Projekt
- ✅ Google indexiert die Seite
- ✅ Filter und Suche funktionieren

---

## 7. Phase 4 — Anfragen & Konversation (3 Wochen)

**Ziel:** Sponsoren stellen Anfragen, Vereine reagieren via Inbox & Threads. Bei Annahme entsteht automatisch eine `SponsorBeteiligung`.

### Iteration 4.1 — `SponsoringAnfrage` & `Nachricht` (3 Tage)

**Spec-Update:** `specs/ANFRAGEN.md` neu.

**Tests (red):**
- `SponsoringAnfrageRepositoryTest`, `SponsoringAnfrageServiceTest`
- Status-Workflow-Tests (Übergänge)

**Implementation (green):**
- Entities, Repositories, Services
- Flyway `V16__sponsoring_anfrage.sql`, `V17__nachricht.sql`

### Iteration 4.2 — Anfrage-Form auf Paket-Seite (2 Tage)

**Tests (red):**
- `AnfrageControllerTest` — Form rendert, Submit erstellt Entwurf, Validierung

**Implementation (green):**
- Button „Anfrage stellen" auf Paket-Detail (Marktplatz)
- Wenn nicht eingeloggt: Flow zur Sponsor-Org-Self-Reg
- Form: Anschreiben, optional abweichender Betrag
- Status-Übergänge: ENTWURF → EINGEREICHT

### Iteration 4.3 — Verein-Inbox & Anfrage-Detail (2 Tage)

**Tests (red):**
- `VereinInboxControllerTest` — Liste mit Filter, nur eigene Org sichtbar (im offenen Modell sieht ja jeder alles, aber „Inbox" zeigt _meine_ Org)

**Implementation (green):**
- `controller/InboxController.java` — `/organisation/{slug}/inbox`
- Sortierung nach `eingereicht_am DESC`, Filter nach Status
- Detail-View mit Anschreiben, Sponsor-Org-Profil

### Iteration 4.4 — Threaded Nachrichten (2 Tage)

**Tests (red):**
- `NachrichtServiceTest` — Erstellen, Lesen, „gelesen am" setzen

**Implementation (green):**
- Anfrage-Detail mit Konversation am Ende
- POST für neue Nachricht (HTMX optional für SPA-Feel)
- Audit-Event `NACHRICHT_GESENDET`

### Iteration 4.5 — E-Mail-Notifications (1 Tag)

**Tests (red):**
- `BenachrichtigungsServiceTest` — bei Anfrage / Nachricht / Status-Wechsel

**Implementation (green):**
- Re-Use bestehender `MailService`
- Template-System: HTML+Plain
- Empfänger: alle ORG_OWNER+ORG_EDITOR der Verein-Org bei Anfrage; nur Submitter bei Status-Wechsel

### Iteration 4.6 — Bei Annahme: Auto-Beteiligung & Paket-Update (2 Tage)

**Tests (red):**
- `AnfrageAnnehmenTest` — Status-Wechsel erzeugt `SponsorBeteiligung` und inkrementiert `quantity_taken`

**Implementation (green):**
- Service-Logik in `SponsoringAnfrageService.annehmen(...)`
- Sponsor-Org → Sponsor-Karteikarte (im Verein-CRM, falls noch nicht vorhanden)
- Beteiligung mit `paket_id`, `betrag` aus Paket
- Audit-Event `ANFRAGE_ANGENOMMEN`

### Iteration 4.7 — Sponsor-Self-Service-Dashboard (2 Tage)

**Tests (red):**
- `SponsorDashboardTest` — eigene Anfragen, eigene angenommene Sponsorings

**Implementation (green):**
- `controller/SponsorDashboardController.java` — `/sponsor/dashboard`
- Liste aller Anfragen des Sponsors mit Status

**Phase-4-Erfolgsmetrik:**
- ✅ Externer Sponsor stellt Anfrage, Verein nimmt an, Beteiligung wird automatisch angelegt
- ✅ E-Mail-Benachrichtigungen kommen rechtzeitig
- ✅ Sponsor sieht in seinem Dashboard alle eigenen Aktivitäten

---

## 8. Phase 5 — Wachstum (laufend)

Nicht-priorisierte Liste; jede Sub-Phase ist eine eigenständige Mini-Roadmap.

| ID | Feature | Aufwand-Schätzung |
|---|---|---|
| 5.A | Watchlist (Sponsor folgt Verein) | 2 Wochen |
| 5.B | Matching-Empfehlungen (regelbasiert) | 3 Wochen |
| 5.C | Statistiken-Dashboard | 2 Wochen |
| 5.D | Mehrsprachigkeit FR/IT (Public-Layer) | 2 Wochen |
| 5.E | Mehrsprachigkeit EN | 1 Woche |
| 5.F | Vertrags-Generator (PDF aus Word-Template — re-use Serienbrief) | 1 Woche |
| 5.G | Digitale Signatur (Skribble/DocuSign) | 2 Wochen |
| 5.H | Zahlungs-Integration (Datatrans / Stripe / TWINT) | 4–6 Wochen |
| 5.I | Mobile-PWA-Optimierung | 2 Wochen |
| 5.J | API für Verbände (REST) | 3 Wochen |
| 5.K | KI-Matching (Embedding-basiert) | 4 Wochen |

---

## 9. Gesamtaufwand & Kritischer Pfad

```
                        ┌─────────────┐
                        │  Pre-Flight │ 1 W
                        └──────┬──────┘
                               ▼
                       ┌──────────────┐
                       │   Phase 0    │ 2 W   ← Multi-Org-Fundament
                       │  Org+Mitgld  │
                       └──────┬───────┘
                              ▼
                     ┌────────────────┐
                     │   Phase 1      │ 2 W   ← Self-Reg + Verifizierung
                     │  Onboarding    │
                     └───┬────────────┘
                         ▼
              ┌─────────────────────┐
              │     Phase 2         │ 3 W   ← Pakete + Medien
              │ Pakete+Sichtbark.   │
              └─────────┬───────────┘
                        ▼
            ┌─────────────────────────┐
            │       Phase 3           │ 3-4 W ← Marktplatz Public
            │   Marktplatz+SEO        │
            └────────────┬────────────┘
                         ▼
              ┌──────────────────────┐
              │      Phase 4         │ 3 W   ← Anfragen + Konversation
              │   Anfragen+Inbox     │
              └────────┬─────────────┘
                       ▼
                 ┌──────────────┐
                 │  MVP fertig  │
                 └──────┬───────┘
                        ▼
                 Phase 5 (laufend)
```

| Block | Dauer (Solo, 50 % Auslastung) | Kalenderwochen |
|---|---|---|
| Pre-Flight | 1 W | 1 |
| Phase 0 | 2 W | 2–3 |
| Phase 1 | 2 W | 4–5 |
| Phase 2 | 3 W | 6–8 |
| Phase 3 | 3–4 W | 9–12 |
| Phase 4 | 3 W | 13–15 |
| **MVP-Plattform** | **14–15 Wochen** | **~3.5 Monate** |

**Bei 100 % Auslastung:** ~7–8 Kalender-Wochen.

**Kritischer Pfad:** Phase 0 → 1 → 2 → 3 → 4. Phase 5-Items können parallel oder nach MVP einzeln gestartet werden.

---

## 10. Begleitende Aktivitäten

Diese Themen laufen kontinuierlich, nicht in einer einzelnen Iteration:

### 10.1 Dokumentation

Nach jeder Iteration:
- `specs/`-Dokument aktualisieren
- `README.md` Routen-/Feature-Tabellen aktualisieren
- `.instructions.md` Test-Anzahl aktualisieren

### 10.2 CI/CD

- GitHub Actions Pipeline bleibt bestehen — Tests + Docker-Build + OCIR-Push
- Pro Phase einmal: Pipeline reviewen, ggf. neue Workflow-Schritte (z.B. JS-Build wenn HTMX/Alpine.js dazukommt)

### 10.3 Infrastruktur (Terraform)

- Phase 0: keine Änderungen
- Phase 1: SMTP-Konfiguration für Self-Reg-Mails (falls noch nicht vollständig)
- Phase 2: Object-Storage-Bucket für Medien-Assets (existiert evtl. schon)
- Phase 3: ggf. CDN vor OCI Object Storage für Bilder
- Phase 5: Stripe/Datatrans-Webhook-Endpunkte

### 10.4 DSG, AGB, Datenschutz

- Phase 1: AGB & Datenschutzerklärung schreiben (rechtlich reviewen lassen)
- Phase 1: Cookie-Banner (falls Tracking)
- Phase 4: Vereinbarung über Auftragsverarbeitung (AVV) zwischen Plattform und Vereinen
- Phase 4: User-Datenexport (Self-Service-Endpoint) implementieren

### 10.5 Beta-Test mit echten Vereinen

- Ende Phase 1: 1–2 befreundete Vereine als Test-Tenants registrieren
- Ende Phase 3: 5–10 Vereine als Beta-Phase
- Phase 4: 10+ Vereine + erste Sponsor-Anfragen
- Feedback-Sessions nach jeder Phase

### 10.6 Marketing-Vorbereitung

- Phase 2: Domain registrieren (eigene Plattform-Domain)
- Phase 3: Landingpage-Design
- Phase 4: Launch-Communication an Vereins-Verbände

### 10.7 Performance-Monitoring

- OCI Logging + Micrometer + Prometheus (existiert in der bestehenden App)
- Pro Phase: Dashboards prüfen, neue Metriken ergänzen (z.B. Anzahl Anfragen, Conversion-Rate)

---

## 11. Rückfall-Strategien

### 11.1 Wenn Vereine das offene Modell ablehnen

**Trigger:** In Beta-Tests sagen 2+ Vereine: „Wir wollen unsere Sponsoren nicht offen zeigen."

**Plan B:** Zurück zu v2-Multi-Tenant. Mehraufwand: ~3 Wochen für Hibernate-Filter und Repository-Erweiterungen. Datenstruktur ist kompatibel — nur Lesefilter müssen ergänzt werden.

### 11.2 Wenn OIDC-Self-Reg sich als zu sperrig erweist

**Plan B:** Externes Keycloak vor OCI IAM stellen. Mehraufwand: ~2 Wochen.

### 11.3 Wenn Phase 3 SEO-Performance enttäuscht

**Plan B:** SSR mit Next.js-Hybrid für Marktplatz, Spring Boot bleibt API. Mehraufwand: 4 Wochen — nur falls SEO geschäftskritisch ist.

### 11.4 Wenn die Prod-DB-Migration in Phase 0 schief geht

**Plan B:** Snapshot-Restore aus Pre-Flight-Backup, Migration als hotfix-Patch nochmal ausrollen. Erwartete Downtime: 30 min.

### 11.5 Wenn Eigentümer-Org-Modell für geteilten Sponsor-Pool politisch zu konfliktreich wird

**Plan B:** Sponsor-Stammdaten doch pro Org dupliziert halten (`organisation_id` als NOT NULL auf Sponsor). Datenmodell akzeptiert das ohne Schema-Änderungen — nur Service-Logik anpassen, dann reduziert sich der „Update vorschlagen"-Workflow zu „eigene Karteikarten editieren".

---

## Anhang A: Erste konkrete Aufgaben (Tag 1–3)

| # | Tag | Aufgabe |
|---|---|---|
| 1 | Tag 1 | Prod-DB Backup, Restore-Test im Staging dokumentieren |
| 2 | Tag 1 | Java 21 in `pom.xml` aktivieren, lokal `mvn clean test` |
| 3 | Tag 2 | GitHub Issues mit Labels `phase-0`, `iteration-0.1` … `0.6` anlegen |
| 4 | Tag 2 | Branch `feature/plattform-phase-0-org-entity` erstellen |
| 5 | Tag 2 | `specs/PLATTFORM_DATENMODELL.md` mit Organisation-Section schreiben |
| 6 | Tag 3 | `OrganisationRepositoryTest` (red) schreiben |
| 7 | Tag 3 | `Organisation`-Entity + Repository implementieren (green) |
| 8 | Tag 3 | Flyway `V8__create_organisation.sql` schreiben + lokal testen |
| 9 | Tag 3 | PR aufmachen, CI-Pipeline grün, mergen |

→ Damit ist Iteration 0.1 in 3 Tagen abgeschlossen, Phase 0 startet konkret.

---

## Anhang B: Akzeptanzkriterien für „MVP-Plattform fertig"

Am Ende von Phase 4 ist die Plattform MVP-fertig, wenn:

1. ✅ Mind. 5 Vereine sind verifiziert und auf der Plattform aktiv.
2. ✅ Mind. 10 Projekte sind im Marktplatz veröffentlicht.
3. ✅ Mind. 5 Sponsoren-Orgs haben sich registriert.
4. ✅ Mind. 10 Anfragen wurden erfolgreich gestellt — davon 3+ angenommen.
5. ✅ Test-Suite hat 80+ Test-Klassen, 95%+ Coverage in Service-Schicht.
6. ✅ Alle Specs unter `specs/` sind aktuell.
7. ✅ Staging und Production sind synchronisiert mit Phase-4-Stand.
8. ✅ DSG-Datenexport und Lösch-Workflow funktionieren.
9. ✅ Marktplatz lädt < 300 ms p95.
10. ✅ AGB, Datenschutzerklärung, Impressum sind rechtlich reviewt und live.
