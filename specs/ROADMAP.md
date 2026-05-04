# Roadmap

> Vollständige Variante mit Iterations-Details: siehe `Sponsoring Plattform/06_Umsetzungsplan.md`.
> Dieses Dokument ist der Spec-Auszug für aktive Entwicklung.

## Aktueller Stand: **Phase 0 — Skelett** ✓

- Spring Boot App lauffähig
- Layout + Index-Seite
- Docker + CI/CD bereit
- Spec-Dokumente im Repo

## Phase 0.1 — Organisation-Entity ✓

- [x] V2: `organisation`-Tabelle, ENUM-Typen
- [x] `Organisation`-Entity + Repository + Service + SlugGenerator
- [x] CRUD-Controller + 3 Templates (Liste, Formular, Detail)
- [x] Tests: ORG-01..10 (21 Tests, alle grün)
- [x] Specs: DATENMODELL, TECHNISCHE_SPEZIFIKATION, TESTSTRATEGIE aktualisiert

## Phase 0.2 — AppUser + Mitgliedschaft + AccessControl ✓

- [x] Spec-Update DATENMODELL + TESTSTRATEGIE (AU/MG/AC-Test-IDs)
- [x] Tests schreiben: AU-01..05, MG-01..04, AC-01..08
- [x] Migration `V3__app_user_und_mitgliedschaft.sql` (`app_user` + `mitgliedschaft`)
- [x] Entity `AppUser`, Enum `PlatformRolle`
- [x] Entity `Mitgliedschaft`, Enum `Rolle` (ORG_OWNER/ORG_EDITOR/ORG_VIEWER)
- [x] `AppUserRepository`, `MitgliedschaftRepository`
- [x] `AppUserService` (registriere, findeNachEmail, findeNachId)
- [x] `MitgliedschaftService` (fuegeHinzu, entferne, findeNachOrg)
- [x] `AccessControl`-Bean (kannOrgEditieren, kannOrgVerwalten)
- [x] Mitglieder-Verwaltungs-UI unter `/organisationen/{slug}/mitglieder`
- [x] `loesche` in OrganisationService wirft bei vorhandenen Mitgliedschaften (ORG-11)
- [x] Tests grün (42 Tests gesamt)

## Phase 1 — Self-Reg & Verifizierung ✓

### Phase 1.1 — Spring Security + Form-Login ✓

- [x] `SecurityConfig` mit Form-Login, CSRF, permitAll für Public-Routen
- [x] `SponsorplatzUserDetailsService` lädt AppUser aus DB
- [x] Login-Seite (`/login`) + Registrierungs-Seite (`/registrieren`)
- [x] `RegistrierungController` + `LoginController`
- [x] Tests: SEC-01..06, REG-01..04 (10 Tests)

### Phase 1.2 — E-Mail-Verifizierung ✓

- [x] Migration `V4__email_verifizierung.sql` (3 Felder auf app_user)
- [x] `VerifikationsService` (Token generieren, validieren, ablaufen)
- [x] E-Mail senden via Spring Mail (MailHog in dev)
- [x] Verifikations-Endpunkt `/verifizieren?token=...`
- [x] Login nur mit verifizierter E-Mail (`isEnabled()`)
- [x] Tests: EV-01..04

### Phase 1.3 — Plattform-Admin & Zefix (Backlog)

- [ ] Admin-Verifizierungs-Queue (`/admin/verifizierungen`)
- [ ] Auto-Verifizierung via Zefix-API (stub in dev)
- [ ] Mitglieder-Einladungs-Flow (E-Mail mit Einladungs-Link)

## Phase 2 — Pakete, Sichtbarkeit & Medien (aktuelle Iteration)

- [ ] `Projekt` mit `sichtbarkeit`, `slug`
- [ ] `SponsoringPaket`-Entity
- [ ] `MedienAsset` mit Storage-Abstraktion
- [ ] Cover/Galerie/Pitch-Deck im Wizard
- [ ] Veröffentlichungs-Flow

## Phase 3 — Marktplatz Public (3-4 Wochen)

- [ ] Public-Routes `/marktplatz/**`
- [ ] Filter (Branche, Region, Datum, Budget)
- [ ] Volltextsuche mit Postgres `tsvector`
- [ ] SEO: Sitemap, Schema.org, Open Graph
- [ ] Public-Vereinsprofile

## Phase 4 — Anfragen & Konversation (3 Wochen)

- [ ] `SponsoringAnfrage`-Entity + Status-Workflow
- [ ] Sponsor-Org-Self-Reg
- [ ] Anfrage-Form auf Paket-Seite
- [ ] Verein-Inbox + Threaded Messages
- [ ] E-Mail-Notifications
- [ ] Bei Annahme: SponsorBeteiligung erzeugen

## Phase 5+ — Wachstum

- Watchlist
- Matching-Empfehlungen
- Mehrsprachigkeit FR/IT
- Vertrags-Generator
- Zahlungs-Integration

## Definition: MVP fertig

- 5+ verifizierte Vereine
- 10+ veröffentlichte Projekte
- 5+ Sponsor-Orgs registriert
- 10+ Anfragen, davon 3+ angenommen
- Test-Suite ≥ 60 Test-Klassen
- DSG-Datenexport funktioniert
- Marktplatz < 300 ms p95
