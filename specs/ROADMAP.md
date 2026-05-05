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

## Phase 0.3 — Dashboard-UI-Skelett ✓

- [x] `DashboardController` unter `/dashboard` mit `@PreAuthorize("isAuthenticated()")`
- [x] `dashboard.html` mit 3-Spalten-Layout (Sidebar / Main / Rail) — eigenes CSS
- [x] Statische Platzhalter-Werte (Anzahl Orgs/Projekte/Anfragen, Events) — Service-Verkabelung folgt iterativ
- [x] Tests: DASH-01..03 (Auth, Routing, Model-Attribute)
- [x] Service-Aufrufe verkabelt: `DashboardService.ladeDashboardDaten(email)` aggregiert Orgs/Projekte/Anfragen/Offene via `DashboardDaten`-Record
- [ ] `naechsteEvents`-Verkabelung (Backlog — Event-Entity fehlt noch)

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

### Phase 1.3 — Plattform-Admin, Zefix & Einladungen ✓

- [x] `AdminVerifizierungController` unter `/admin/verifizierungen` (PLATFORM_ADMIN-only)
- [x] `OrganisationService.verifiziere(id)`, `suspendiere(id)`, `findePending()`
- [x] `ZefixService`-Interface + `ZefixServiceStub` (gibt immer `Optional.empty()`)
- [x] Migration `V8__einladung.sql` (Einladungs-Tabelle + Index auf `organisation.status`)
- [x] `Einladung`-Entity + `EinladungRepository`
- [x] `EinladungsService` (erstelleEinladung, nimmAn — Token-basiert, 7 Tage gültig)
- [x] `EinladungsController` (`/einladung/annehmen?token=...`)
- [x] `AdminOrgView`-DTO für Verifizierungs-Queue
- [x] Templates: `admin/verifizierungen.html`, `einladung-erfolg.html`
- [x] Tests: ADM-01..08, ZFX-01, EINL-01..06 (157 Tests gesamt)

## Phase 2 — Pakete, Sichtbarkeit & Medien ✓

- [x] Migration `V5__projekt_und_sponsoring_paket.sql`
- [x] `Projekt`-Entity mit `sichtbarkeit`, `slug`, Kategorie, Ort, Datum
- [x] `SponsoringPaket`-Entity (Name, Beschreibung, Preis CHF, Sortierung, aktiv)
- [x] `ProjektRepository`, `SponsoringPaketRepository`
- [x] `ProjektService` (erstelle, veroeffentliche, archiviere, findeOeffentliche)
- [x] `SponsoringPaketService` (erstelle, deaktiviere, findeNachProjekt)
- [x] `ProjektController` (CRUD + Veröffentlichung + Paket-Anlage)
- [x] DTOs: `ProjektFormDto`, `SponsoringPaketFormDto`
- [x] Templates: projekt-liste, projekt-form, projekt-detail
- [x] Tests: PRJ-01..04, SP-01..04, PCTRL-01..05 (69 Tests gesamt)
- [x] `MedienAsset` mit Storage-Abstraktion: Entity, `StorageService` Interface + `LokalerStorageService`, `MedienAssetService` (Upload/Lösch/Validierung: JPEG/PNG/WebP, max 5MB, max 10 pro Entity)
- [x] `MedienController` (GET `/medien/{id}` public, POST Upload für Projekt/Org, POST Löschen mit AccessControl)
- [x] `MedienAssetView`-DTO, Migration `V11__medien_asset.sql`
- [x] Tests: MA-01..08 (197 Tests gesamt)
- [x] Cover/Galerie/Pitch-Deck: Upload-Widget auf Projekt-Detail, Cover-Bild in Marktplatz-Karten, `ProjektView` mit `coverUrl`

## Phase 3 — Marktplatz Public ✓

- [x] `MarktplatzController` mit `/marktplatz` (Liste) und `/marktplatz/{slug}` (Detail)
- [x] Filter nach Kategorie und Ort
- [x] SecurityConfig: `/marktplatz/**` als permitAll
- [x] Templates: marktplatz.html, marktplatz-detail.html
- [x] Tests: MKT-01..05 (74 Tests gesamt)
- [x] SEO: Open Graph Meta-Tags auf Marktplatz-Detail + Vereinsprofil
- [x] SEO: `SitemapController` unter `/sitemap.xml` (Projekte + Vereine)
- [x] Public-Vereinsprofile: `VereinProfilController` unter `/vereine/{slug}`
- [x] Volltextsuche: `ProjektRepository.sucheOeffentliche()` (LIKE über Name/Beschreibung/Kategorie/Ort/Org), Suchfeld auf Marktplatz, V10 Indizes
- [x] Tests: MKT-06..07, VTS-01..02 (189 Tests gesamt)

## Phase 4 — Anfragen & Konversation ✓

- [x] Migration `V6__sponsoring_anfrage.sql` (Status-CHECK, FK auf Paket + Orgs)
- [x] `SponsoringAnfrage`-Entity + Enum `AnfrageStatus` (NEU/IN_PRUEFUNG/ANGENOMMEN/ABGELEHNT/ZURUECKGEZOGEN)
- [x] `SponsoringAnfrageRepository` (eingehende, ausgehende, Zählung nach Status)
- [x] `SponsoringAnfrageService` (erstelle, annehme, lehneAb, findeEingehende/Ausgehende)
- [x] `AnfragenController` (`/organisationen/{slug}/anfragen` — Liste, Annehmen, Ablehnen)
- [x] `AnfrageFormDto` (Validierung: Nachricht 10-2000 Zeichen, E-Mail)
- [x] Template: anfragen-liste.html (Dashboard-Sidebar, Karten mit Aktionen)
- [x] Tests: ANF-01..05, ANFCTRL-01..04 (107 Tests gesamt)
- [x] E-Mail-Notifications: `BenachrichtigungsService` verdrahtet in `SponsoringAnfrageService` (erstelle → neue Anfrage, annehme/lehneAb → Antwort)
- [x] Tests: ANF-06..07, BEN-01..03
- [ ] Sponsor-Org-Self-Reg (Backlog)
- [ ] Verein-Inbox + Threaded Messages (Backlog)

## Phase 5 — Watchlist ✓

- [x] Migration `V7__watchlist.sql` (UNIQUE user_id+projekt_id, FK CASCADE)
- [x] `WatchlistEintrag`-Entity
- [x] `WatchlistRepository` (findeNachUser, existsByUserIdAndProjektId, delete)
- [x] `WatchlistService` (hinzufuegen, entferne, findeNachUser, istGemerkt)
- [x] `WatchlistController` (`/watchlist`, `/watchlist/hinzufuegen/{slug}`, `/watchlist/entfernen/{slug}`)
- [x] Template: watchlist.html (Dashboard-Sidebar, Karten mit Entfernen-Aktion)
- [x] Tests: WL-01..05, WLCTRL-01..03 (118 Tests gesamt)

## Phase 5+ — Wachstum (nächste Iteration)

- Matching-Empfehlungen
- Mehrsprachigkeit FR/IT
- Vertrags-Generator
- Zahlungs-Integration
- Volltextsuche mit Postgres `tsvector`

## Phase 6 — Einstellungen & Production-Readiness ✓

- [x] `@EnableAsync` für nicht-blockierende E-Mail-Benachrichtigungen
- [x] `EinstellungenController` mit DSG-Datenexport (`/einstellungen/datenexport`)
- [x] Passwort-Änderung (`POST /einstellungen/passwort`)
- [x] Template `einstellungen.html`
- [x] `AppUserService.aenderePasswort()` mit Validierung (min 8 Zeichen, altes PW prüfen)
- [x] Tests: EINST-01..03, PW-01..03, SEO-01 (185 Tests gesamt)

## Definition: MVP fertig

- 5+ verifizierte Vereine
- 10+ veröffentlichte Projekte
- 5+ Sponsor-Orgs registriert
- 10+ Anfragen, davon 3+ angenommen
- Test-Suite ≥ 60 Test-Klassen
- DSG-Datenexport funktioniert
- Marktplatz < 300 ms p95
