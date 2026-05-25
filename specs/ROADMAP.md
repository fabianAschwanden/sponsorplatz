# Roadmap

> Vollständige Variante mit Iterations-Details: siehe `Sponsoring Plattform/06_Umsetzungsplan.md`.
> Dieses Dokument ist der Spec-Auszug für aktive Entwicklung.

## Aktueller Stand (kurz)

- **Phasen 0 – 9, 11, 12**: ✅ abgeschlossen
- **Phase 10** (Production-Readiness, ohne 10.4): ✅ 10.1–10.3 + 10.5 fertig
- **Phase 13** (Pre-Pilot-Hardening): ✅ Kern fertig — 13.1 A11y-auth ✅ / 13.2 2FA-Reset ✅ (Pflicht parkt) / 13.3 OIDC ✅
- **Phase 14** (Produktivschaltung, **war 10.4**): 🔜 sobald Phase 13 durch
- **Phase 15** (Post-Pilot): teilweise vorgezogen
  - 15.1 echtes Zahlungs-Provider-Wiring + 15.2 Mahnwesen: 📋 geplant
  - 15.3 Multi-Cloud Azure: ⏳ Slices 1–4 (App + Terraform + CD) ✅, Slices 5–7 (DNS-Failover, Cross-Replication, Smoke) offen
  - 15.4 Datei-Backup + Restore: ✅ inkl. Admin-UI
  - Cross-Cloud-Sync-Schutz: ✅ `umgebung`-Marker in Audit-Log + Sentry-Tag

## Logische Reihenfolge

Die Phasen-Nummern sind historisch gewachsen (Phase 10 wurde vor 11/12
begonnen, 10.4 aber später als 12 abgeschlossen — Reihenfolge im Dokument
spiegelt das). Die **logische Lese-/Umsetzungs-Reihenfolge** ist:

```
Foundation (✅ alles erledigt)
    Phase 0   Skelett + Org-Entity + AppUser + Dashboard
    Phase 1   Self-Reg & Verifizierung
    Phase 2   Pakete + Medien
    Phase 3   Marktplatz Public
    Phase 4   Anfragen & Konversation
    Phase 5   Watchlist (+ Wachstum als 5+)
    Phase 6   Einstellungen & DSG-Export
    Phase 7   Health-Story
    Phase 8   MVP-Reife & Demo
    Phase 9   Roadmap-Lücken (i18n, Zahlungs-Provider-Skeleton, Events)
    Phase 11  Pilot-Hardening
    Phase 12  Customizable Task-Engine
    Phase 10  Production-Readiness (10.1 Monitoring, 10.2 Sentry,
              10.3 DSG-Pages, 10.5 Security-Headers)

Pre-Pilot (⏳ jetzt)
    Phase 13  Pre-Pilot-Hardening (NEU)
              13.1 A11y für authentifizierte Seiten
              13.2 Zwei-Faktor-Authentifizierung (TOTP)
              13.3 OIDC-Identity-Provider-Anbindung

Produktivschaltung (🔜)
    Phase 14  Cutover sponsorplatz.ch (war 10.4)
              14.1 Ops — HTTPS, prod-SMTP, SPF/DKIM/DMARC, OCI-Backups, DNS
              14.2 Pilot-Welle — 5 echte CH-Sport-/Health-Vereine

Post-Pilot (📋)
    Phase 15  Wachstum nach erstem Echt-Betrieb (NEU)
              15.1 Echte Zahlungs-Provider-Integration (Stripe/PostFinance/Datatrans)
              15.2 Mahnwesen (automatische Mahnstufen, CH-Inkasso-Anbindung)
              15.3 Multi-Cloud — Azure als zweite Zone (DR / Aktiv-Aktiv-Vorbereitung)
              15.4 Datei-Backup + Restore (Sponsoring-Files-Roundtrip)
              15.5 Weitere Post-Pilot-Features (Backlog-getrieben)
```

Die Verschiebung von 10.4 → 14 schafft Platz: Phase 13 deckt
Sicherheits- und A11y-Themen ab, die vor dem ersten Echtbenutzer
landen sollten, ohne den Cutover zu verzögern. Phase 15 sammelt
alles, was erst Sinn macht wenn echte Verträge + Zahlungsflüsse
existieren.

## Phase 0 — Skelett ✓

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
- [x] `naechsteEvents`-Verkabelung (Phase 9.3 — Event-Entity + Dashboard-Integration)

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
f- [x] Cover/Galerie/Pitch-Deck: Upload-Widget auf Projekt-Detail, Cover-Bild in Marktplatz-Karten, `ProjektView` mit `coverUrl`

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
- [x] Sponsor-Org-Self-Reg: `SponsorRegistrierungController` unter `/sponsor/registrieren`, kombinierter Flow (User + UNTERNEHMEN-Org + ORG_OWNER-Mitgliedschaft)
- [x] Verein-Inbox: `Nachricht`-Entity, `NachrichtService`, `NachrichtController` unter `/organisationen/{slug}/anfragen/{id}/nachrichten`
    - Migration V13, Thread-Ansicht, nur bei ANGENOMMEN-Anfragen, AccessControl, `NachrichtView`-DTO
    - Tests: MSG-01..09 (217 Tests gesamt)

## Phase 5 — Watchlist ✓

- [x] Migration `V7__watchlist.sql` (UNIQUE user_id+projekt_id, FK CASCADE)
- [x] `WatchlistEintrag`-Entity
- [x] `WatchlistRepository` (findeNachUser, existsByUserIdAndProjektId, delete)
- [x] `WatchlistService` (hinzufuegen, entferne, findeNachUser, istGemerkt)
- [x] `WatchlistController` (`/watchlist`, `/watchlist/hinzufuegen/{slug}`, `/watchlist/entfernen/{slug}`)
- [x] Template: watchlist.html (Dashboard-Sidebar, Karten mit Entfernen-Aktion)
- [x] Tests: WL-01..05, WLCTRL-01..03 (118 Tests gesamt)

## Phase 5+ — Wachstum (nächste Iteration)

- [x] Matching-Empfehlungen: `MatchingService` findet Projekte nach Branchen-Match, Dashboard zeigt max. 6 Empfehlungen
- [x] Prod-Admin-Seed: `ProdAdminSeedRunner` erstellt Admin aus ENV-Variablen (`SPONSORPLATZ_ADMIN_EMAIL/PASSWORD`)
- [x] Audit-Log: `AuditLog`-Entity, `AuditService` (async), Admin-UI unter `/admin/audit`, Verdrahtung in Admin-Aktionen
    - Migration V14, AuditAktion-Konstanten, chronologische Log-Ansicht (letzte 100)
- [x] Backup-Funktion: `BackupService` (H2 SCRIPT TO / pg_dump), Cron-Job täglich 02:00, Admin-UI unter `/admin/backups`
    - Manueller Backup-Button, Cleanup nach 30 Tagen, Cloud-fähig via Erweiterung
    - Tests: AUDIT-01..03, BACKUP-01..03 (230 Tests gesamt)
- [x] In-App-Notifications (Glocke): `Benachrichtigung`-Entity, `NotificationService`, `NotificationController`
    - Migration V19, Badge-Polling (30s), JSON-API `/benachrichtigungen/anzahl|liste|gelesen`
    - Sidebar: Glocke mit rotem Badge (ungelesene Anzahl)
    - Verdrahtet in `SponsoringAnfrageService` (erstelle/annehme/lehneAb → Notification an Org-Mitglieder)
    - Tests: NOTIF-01..05 (285 Tests gesamt)
- [x] Passwort-Reset: `PasswortResetService` (Token-basiert, 1h gültig), `PasswortResetController`
    - Migration V20, `/passwort-vergessen` + `/passwort-reset?token=...`
    - Kein Information-Leak (immer Erfolg), Passwort-Bestätigung, Link auf Login-Seite
    - Tests: PWRESET-01..07, PWRCTRL-01..06 (305 Tests gesamt)
- [x] Brute-Force-Schutz: `LoginBruteForceSchutz` (Account-basiert, 5 Versuche → 15 Min Sperre)
    - `LoginSperreFilter` (Pre-Auth, spart BCrypt), `LoginFailureHandler` + `LoginSuccessHandler`
    - RateLimitFilter erweitert um `/passwort-vergessen` + `/passwort-reset`
    - Login-Seite: Sperr-Meldung mit Link zu Passwort-Reset
    - Tests: BF-01..06 (311 Tests gesamt)
- [x] Hierarchische Firmenstruktur: `uebergeordnete_org_id` Self-Referenz auf Organisation
    - Migration V24, max 3 Stufen (Konzern → Tochter → Abteilung)
    - `OrgHierarchieService` (erstelleUnterorganisation, findeElternkette, berechneTiefe)
    - `AccessControl` erweitert: vererbte Berechtigung über Elternkette (Owner auf Eltern → kann Kind editieren)
    - `OrganisationView` mit `uebergeordneteOrgId/Name/Slug`, `istUnterorganisation()`, Breadcrumbs
    - Lösch-Schutz: Eltern mit Kindern kann nicht gelöscht werden
    - Nur UNTERNEHMEN-Orgs dürfen Sub-Orgs haben
    - Tests: HIER-01..05, AC-HIER-01..02, VIEW-03 (325 Tests gesamt)
- [x] Vertrags-Generator: `Vertrag`-Entity, `VertragService`, `VertragController` mit PDF-Export
    - Migration V16, `VertragsStatus`-Enum, Templates `vertrag-detail.html` + `vertrag-pdf.html`
    - `Rechnung`-Entity + IBAN (V17), `RechnungService`, `RechnungController` mit PDF-Export
    - Tests: `VertragServiceTest`, `RechnungServiceTest`
- [x] Volltextsuche Postgres `tsvector`: Migration V22 (Postgres-only), GIN-Index, dialektabhängiger Branch im `VolltextSucheService` (Fallback auf LIKE in H2)
- [ ] **Mehrsprachigkeit FR/IT** → siehe Phase 9.1
- [ ] **Zahlungs-Provider-Anbindung** → siehe Phase 9.2

## Phase 6 — Einstellungen & Production-Readiness ✓

- [x] `@EnableAsync` für nicht-blockierende E-Mail-Benachrichtigungen
- [x] `EinstellungenController` mit DSG-Datenexport (`/einstellungen/datenexport`)
- [x] Passwort-Änderung (`POST /einstellungen/passwort`)
- [x] Template `einstellungen.html`
- [x] `AppUserService.aenderePasswort()` mit Validierung (min 8 Zeichen, altes PW prüfen)
- [x] Tests: EINST-01..03, PW-01..03, SEO-01 (185 Tests gesamt)

## Phase 7 — Health-Story sichtbar machen ✓

> **Paket 1.** Die Branche-Enum-Schärfung aus V12 wird im Frontend erlebbar — Marken-, Vereins- und Versicherten-Sicht spüren den Health-Fokus an jeder Stelle. Direkter Hebel für die Kickbox-RedBox-Validierung.

### 7.1 — Marktplatz-Branche-Filter

- [x] `MarktplatzController` erweitern um Multi-Select-Parameter `branche=...` (URL-persistent für Bookmarks)
- [x] Filter-Logik im Controller: `Set<Branche>` Parameter, Projekte nach `org.branche` filtern
- [x] `marktplatz.html` Chip-Cloud aller elf Health-Branchen oben, Multi-Toggle, Default = alle aktiv
- [x] Aktive Filter im Hero des Marktplatzes als entfernbare Chips anzeigen
- [x] Tests: MKT-08 Filter wirkt, MKT-09 Default = alle aktiv, MKT-10 URL-Persistenz

### 7.2 — Vereins-Profil mit Health-Hero

- [x] `verein-profil.html` Branche-Chip im Hero (Coral-Background, prominent)
- [x] `Branche.beschreibung()` als neue Methode am Enum für Subhead-Text
- [x] Verlinkung des Chips auf `/marktplatz?branche=...` (führt direkt in den Branchen-Filter)
- [x] OG-Image-Slot mit Branche-Tag (siehe 8.3) — Branche.anzeige (lesbar statt Enum), als gerundeter Pill oben rechts gerendert; Projekt-OG zieht die Branche aus der Org. Plus Twitter-Card-Meta-Tags auf Verein-Profil + Marktplatz-Detail.
- [x] Tests: VP-03 Chip rendert, VP-04 Subhead enthält Beschreibung

### 7.3 — Marken-Landing-Page

- [x] `MarkenLandingController` unter `/fuer-marken` (permitAll)
- [x] `marken-landing.html` mit Health-Use-Cases (Krankenkassen, Apotheken, Lebensmittel-Marken, Sportartikel, Stiftungen)
- [x] Live-Statistik: Vereine pro Branche, aktive Projekte (`StatistikService`)
- [x] CTA "Sponsor-Konto erstellen" → `/sponsor/registrieren`
- [x] Sub-Claim "Sichtbarkeit ohne Streuverlust" + drei Trust-Indikatoren (kuratiert, lokal, messbar)
- [x] Tests: MARK-01 rendert, MARK-02 Stats korrekt, MARK-03 CTA-Link

---

## Phase 8 — MVP-Reife & Demo-Tauglichkeit ✓

> **Paket 2.** Plattform ist live-demo-fähig für Stakeholder-Termine, Pilot-Verein-Gespräche und Kickbox-Validierungs-Interviews.
> Iteration: ~3 Tage.

### 8.1 — Pilot-Seed mit Beispiel-Daten

- [x] `DemoSeedRunner` (Profil `demo` neben dev/prod, idempotent)
- [x] 5 Beispiel-Vereine über alle elf Health-Branchen verteilt (Sport, Reha, Mental Health, Ernährung, Selbsthilfe…)
- [x] 10 veröffentlichte Beispiel-Projekte mit realistischen Texten + Cover-Bildern
- [x] 3 Sponsor-Orgs (Krankenkasse-Beispiel, Apotheke, Lebensmittel-Marke)
- [x] Beispiel-Anfragen quer durchs Status-Lifecycle, einige ANGENOMMEN-Engagements
- [x] „DEMO — Beispieldaten"-Header bei aktivem demo-Profil (nicht produktiv verwechselbar)
- [x] Tests: SEED-01 Konsistenz aller FK, SEED-02 Disclaimer rendert

### 8.2 — Engagement-Schaufenster (öffentlich)

- [x] `EngagementService` — Engagements abgeleitet aus `SponsoringAnfrage` mit Status ANGENOMMEN
- [x] `EngagementController` unter `/marken/{slug}/engagements` (permitAll)
- [x] Template `engagement-schaufenster.html` mit Verein-Karten, Region, Branche, Projekt-Snippet
- [x] Filter nach Region/Branche
- [x] Auf Marken-Detailseite verlinkt — schliesst die "CSS unterstützt diese Vereine"-Brücke
- [x] Tests: ENG-01 nur ANGENOMMEN, ENG-02 Slug-Filter, ENG-03 Region-Filter

### 8.3 — OG-Card-Generator

- [x] `OgImageController` für `/og/projekt/{slug}.png` und `/og/verein/{slug}.png` (1200×630)
- [x] Server-side Java Graphics2D → PNG (keine externe Lib nötig)
- [x] Branche-Tag + Slogan + Hero-Background im Sponsorplatz-Brand
- [x] HTTP-Cache-Header `max-age=3600, public`
- [x] In `<meta property="og:image">` von Vereinsprofil + Marktplatz-Detail eintragen
- [x] Tests: OG-01 Verein-Card, OG-02 Projekt-Card, OG-03 Cache-Header

---

## Phase 9 — Roadmap-Lücken schliessen ✓

> **Paket 3.** Die offenen Punkte aus Phase 5+ formal abschliessen.

### 9.1 — Mehrsprachigkeit FR/IT

- [x] `LocaleResolver` (Cookie-basiert + `Accept-Language`-Fallback)
- [x] `LocaleChangeInterceptor` mit `?lang=fr|it|de` URL-Override
- [x] `messages_fr_CH.properties` und `messages_it_CH.properties` (Erstübersetzung)
- [x] Sprach-Umschalter im Footer (DE/FR/IT)
- [x] V18-Feld `app_user.sprache` verkabeln (Feld existiert, LocaleConfig nutzt Cookie)
- [x] `Branche`-Anzeigenamen lokalisiert via messages_xx_CH.properties
- [x] Tests: I18N-01..04 Cookie-Persistenz/URL-Override, I18N-05..06 Branche FR/IT

### 9.2 — Zahlungs-Provider-Anbindung

> **End-to-End-Spec:** [`SPONSORING_ZAHLUNGSFLUSS.md`](SPONSORING_ZAHLUNGSFLUSS.md)
> — Lifecycle, Statusmaschinen, Swiss-QR-Bill-Compliance, Nummerierung, MwSt,
> Mahnwesen, Storno, DSG-Permissions, Audit-Log, Datatrans-Detail-Spec.

- [x] `PaymentProvider`-Interface (`erstelleZahlung`, `bestaetigeZahlung`, `widerrufe`)
- [x] `LokalerStubProvider` für dev/test (sofort BEZAHLT)
- [ ] `DatatransProvider` für prod (Backlog — Sandbox-Konfiguration, HMAC-Webhook, siehe ZAHLUNGSFLUSS §11)
- [x] `PaymentWebhookController` für Provider-Callbacks (`/payment/webhook/{provider}`)
- [x] `PaymentService` delegiert an aktiven Provider
- [x] `RechnungService.markiereAlsBezahltViaWebhook` für idempotente Webhook-Verarbeitung
- [x] SecurityConfig: `/payment/webhook/**` permitAll + CSRF-Ausnahme
- [x] Tests: PAY-01..02 Stub, PAY-06 Service-Delegation

### 9.3 — Event-Entity (Phase 0.3 Schluss)

- [x] Migration `V26__event.sql` (Event-Tabelle + Index)
- [x] `Event`-Entity, `EventRepository`, `EventService`
- [x] `EventView`-DTO (kein Org-Entity im View)
- [x] `EventController` unter `/organisationen/{slug}/events`
- [x] `DashboardService.naechsteEvents(...)` verkabelt (Backlog aus Phase 0.3 geschlossen)
- [x] `DashboardDaten`-Record erweitert um `naechsteEvents`
- [x] Template `event-liste.html`
- [x] Tests: EVT-01..05 (CRUD, AccessControl, View-Mapping, Dashboard-Integration)

---

## Phase 11 — Pilot-Hardening (Anfrage-Flow + UX-Verbesserungen) ✓

> **Paket 5.** Erweitert den Anfrage-Flow um die Verein→Sponsor-Richtung,
> schärft UX rund um Onboarding/Support/Datei-Anhänge, schliesst die
> rollenabhängige Sicht der Anfragen-Übersicht. Iteration: kontinuierlich
> seit Phase 9.

### 11.1 — Onboarding-Wizard nach erstem Login

- [x] `OnboardingController` unter `/onboarding` — User ohne Mitgliedschaft wird vom `DashboardController` hierhin redirected
- [x] Schnell-Verein-Form (Name + Branche + Ort) → `OrganisationService.erstelleMitEigentuemer` macht User automatisch ORG_OWNER
- [x] Einladungs-Token-Pfad mit Whitelist-Validierung (Regex `[A-Za-z0-9_-]{16,128}`) + `RedirectAttributes.addAttribute` (RFC-konformes URL-Encoding)
- [x] Re-Entry-Schutz (User mit Org wird zum Dashboard weitergeleitet)
- [x] Tests: ONB-01..05

### 11.2 — Support-Formular

- [x] `SupportController` unter `/support` — eingeloggte User schicken Mail an Plattform-Admin
- [x] Property `sponsorplatz.support.empfaenger` (ENV `SPONSORPLATZ_ADMIN_EMAIL`, Default `support@sponsorplatz.ch`); Fallback auf `MailService.effektiverAbsender()`
- [x] Bei Mail-Fehler: Form bleibt offen, Fehlermeldung mit Empfänger-Adresse — KEINE falsche Erfolgs-Meldung
- [x] Logging ohne User-PII (DSG-Hygiene)
- [x] Tests: SUP-01..04

### 11.3 — Datei-Anhänge an Projekten

- [x] `AssetTyp.ANHANG` neu (PDF/PPTX/DOCX/XLSX/PPT/DOC/XLS) — getrennt von Bild-Validation
- [x] `MedienAssetService` validiert separat: Bilder max 5 MB, Dokumente max 20 MB, 10 pro Entity
- [x] `MedienAssetView` mit `groesseBytes`, `istBild()`, `groesseFormatiert()` (B/KB/MB), `endung()` für Icon-Zuordnung
- [x] `MedienController` liefert Bilder inline, Dokumente als Attachment-Download (`ContentDisposition.builder()` mit RFC-5987-Encoding)
- [x] `loeschen`-Endpoint typabhängig (ORGANISATION/PROJEKT/USER) — IDOR-Fix für PROJEKT-Anhänge
- [x] `projekt-detail.html` zeigt Anhänge mit Icon + Grösse, Upload-Form für Dokumente
- [x] `marktplatz-detail.html` zeigt öffentliche „Projekt-Unterlagen"-Sektion mit Download-Links
- [x] Tests: VIEW-10..12 (Mapping, istBild, groesseFormatiert, endung)

### 11.4 — Marktplatz „Neueste Projekte"-Preview

- [x] `MarktplatzController.detail` zeigt 3-Karten-Preview oberhalb der regulären Liste — nur auf der ungefilterten Startansicht
- [x] `ProjektService.findeNeuesteOeffentliche(limit)` sortiert nach `veroeffentlichtAm DESC`
- [x] Cover aus `MedienAssetService.findeCover` oder SVG-Platzhalter

### 11.5 — Anfrage-Flow Erweiterungen

- [x] Status-Cleanup V29: `IN_PRUEFUNG`, `ZURUECKGEZOGEN` entfernt — Workflow strikt `NEU → ANGENOMMEN | ABGELEHNT`
- [x] Anfrage-Erstellungs-Form `/anfragen/neu?paketId=…` (Sponsor → Verein, Marktplatz-Detail-Klick)
- [x] IDOR-Schutz beim Annehmen/Ablehnen: `kannOrgEditieren(empfaengerOrg)`-Check
- [x] LEFT JOIN FETCH durchgängig in `SponsoringAnfrageRepository` (Lazy-Init-Fix)
- [x] **Verein → Sponsor — Kontakt-Anfrage (V30)**:
   - `paket_id` nullable, neue Spalte `betreff`
   - `SponsoringAnfrageService.erstelleKontaktAnfrage(anfrager, empfaenger, betreff, nachricht, …)`
   - `MeineAnfragenController` rollenabhängig: Vereins-Mitglieder sehen ausgehende + bekommen „+ Sponsor anfragen"-Button; Sponsoren-only-User sehen nur eingehende
   - Picker `/anfragen/neu-kontakt` listet aktive Sponsor-Orgs (`OrgTyp.UNTERNEHMEN`, `Status.VERIFIED|ACTIVE`)
   - `AnfrageView.istPaketAnfrage()` unterscheidet die beiden Typen
- [x] **Vertrag aus Kontakt-Anfrage**: nach Sponsor-Annahme erscheint der „Vertrag erstellen"-Button beim Verein-Owner (Meine-/Org-ausgehend); `VertragService.erstelle` mappt Verein-/Sponsor-Org via `OrgTyp`-Check — siehe [`KONTAKT_ANFRAGE_VERTRAG.md`](KONTAKT_ANFRAGE_VERTRAG.md). Tests: VTR-09, VTR-10, VIEW-13
- [x] Tests: MANF-01..07

### 11.6 — Sicht-Filter `/organisationen`

- [x] Liste zeigt für eingeloggte Nicht-Admins nur Orgs mit eigener Mitgliedschaft (jede Rolle inkl. `ORG_VIEWER`)
- [x] Anonyme: alle (öffentliche Übersicht); PLATFORM_ADMIN: alle (für Verifizierungs-Queue)

### 11.7 — Owner-on-Create

- [x] `OrganisationService.erstelleMitEigentuemer(dto, userId)` legt Org an + `Mitgliedschaft` mit `ORG_OWNER` automatisch
- [x] Onboarding-Pfad und reguläre `POST /organisationen` nutzen die Methode

### 11.8 — i18n vollständige Konvertierung (DE/FR/IT/EN)

- [x] `LocaleConfig` mit Country-Suffix-Mapping (`de_CH`/`fr_CH`/`it_CH`/`en`) — fixt fehlerhafte Sprach-Auflösung
- [x] ~50 Templates konvertiert auf `#{key}`-Resolution
- [x] ~600 Keys über alle vier Bundles
- [x] Marketing-Copy: erster Wurf, Native-Speaker-Review noch offen vor Pilot-Launch

### 11.9 — JOIN-FETCH-Konvention für Projekt-Queries

- [x] Alle Repository-Methoden, die `Projekt`-Entitäten zurückgeben, hängen `JOIN FETCH p.org` dran (`findBySlug`, `findByOrgIdOrderByCreatedAtDesc`, `findBySichtbarkeitOrderByVeroeffentlichtAmDesc`, `sucheOeffentliche`, `findePassende`)
- [x] Konvention im Repo-JavaDoc dokumentiert
- [x] Behebt LazyInit-500 in `ProjektView.von` → `OrganisationKurzView.von(p.getOrg())` unter `open-in-view=false`

### 11.10 — Findings-Fixes Sicherheits-Review

- [x] **F1** Filename-Header-Sanitization: `ContentDisposition.builder("attachment").filename(name).build()` (RFC 5987)
- [x] **F2** PROJEKT/USER-IDOR beim `/medien/{id}/loeschen` — Switch-Total über alle EntityTyps
- [x] **F3** `ProjektController.detail()` lädt Medien einmalig + Stream-split bilder/anhaenge
- [x] **MeineAnfragenController IDOR**: annehmen/ablehnen prüfen `kannOrgEditieren(empfaengerOrg)`

### 11.11 — Rechnungs-Lifecycle-Kern (5.F-Folge, Spec [SPONSORING_ZAHLUNGSFLUSS.md](SPONSORING_ZAHLUNGSFLUSS.md))

- [x] **Audit-Log-Verdrahtung** (Spec §10): neue `AuditAktion`-Konstanten `VERTRAG_ERSTELLT/UNTERZEICHNET/GEKUENDIGT`, `RECHNUNG_ERSTELLT/BEZAHLT/STORNIERT/MAHNUNG_VERSENDET/PDF_HERUNTERGELADEN`. `RechnungService` + `VertragService` rufen `auditService.protokolliere(...)` bei jedem Status-Übergang.
- [x] **RechnungsnummerGenerator** (Spec §5): separate Klasse, Format `R-YYYY-NNNNN` (5-stellig), pro Org-Jahr fortlaufend, lückenlos. Repository-Query `findeMaxLfdNr(orgId, praefix)` mit JPQL-`substring`. `Clock`-Bean injection für deterministische Tests.
- [x] **Storno-Grund** (Spec §8.1, Migration V34): `rechnung.storno_grund VARCHAR(500) NULL`. `stornieren(id, grund)`-Signatur, Audit-Log mit `vorheriger_status` + `grund`.
- [x] Tests: RECH-07/07b/08/09 (RechnungsnummerGenerator), RECH-15 (markiereBezahlt-Audit), RECH-16 (stornieren-Audit + Grund). 462 Tests gesamt.
- [x] **State-Machine-Sauber-Iteration**: `VertragService.kuendige(id, grund)` mit BEZAHLT-Check (wirft) + OFFEN-Auto-Storno + Audit-Event `VERTRAG_GEKUENDIGT`. `markiereUnterzeichnet` prüft `preisChf > 0 OR leistungVerein/Sponsor gepflegt` (verhindert versehentliche Standardwert-Unterzeichnung; Naturalien-Sponsoring explizit erlaubt). Migration V35 (`gekuendigt_am`, `kuendigungs_grund`). VertragService↔RechnungService-Cycle via `@Lazy`-Proxy gelöst. Tests VTR-05b/c, VTR-07/08/08b/08c — 468 Tests gesamt.
- [ ] **Mahnwesen** (Spec §7, V37) — Backlog, nach Pilot-Launch: `mahnstufe`/`letzte_mahnung_am`-Spalten, `MahnungsCronJob` täglich 06:00, Tests MAHN-01..04.

### 11.12 — Statistik-Dashboard für Sponsor + Verein (5.C)

- [x] `/statistiken`-Route für alle eingeloggten User — Template `statistik.html` rendert je nach Mitgliedschaft die passende Sektion.
- [x] `SponsorStatistikService` aggregiert über alle UNTERNEHMEN-Orgs des Users (4 Repo-Count-Queries + 1 Sum-Query + 1 Branche-Group-By).
- [x] `VereinStatistikService` aggregiert über alle VEREIN-Orgs des Users — eigene Sicht mit Projekt-/Paket-Counter, eingehenden + ausgehenden Anfragen, Einnahmen (`Vertrag.org`-Seite), Rechnungs-Liquidität.
- [x] Sponsor-Kennzahlen: aktive Engagements (UNTERZEICHNETe Verträge), Sponsoring-Volumen, Anfragen-Pipeline (NEU/ANGENOMMEN/ABGELEHNT) mit Conversion-Rate, Rechnungs-Status, Branchen-Verteilung als horizontale Balken, Vertrags-Status-Verteilung.
- [x] Verein-Kennzahlen: veröffentlichte Projekte + aktive Pakete, Sponsoring-Einnahmen (UNTERZEICHNETe Verträge), Eingehende Anfragen + Ausgehende Kontakt-Anfragen je mit Conversion-Rate, Rechnungs-Status (rechnungsstellende Org), Vertrags-Status.
- [x] Multi-Role-User (Mitglied von VEREIN UND UNTERNEHMEN) sieht beide Sektionen untereinander; weder noch → einheitlicher Empty-State.
- [x] **Bug-Fix:** Vereins-Mitglieder sahen vorher die Sponsor-Statistik — jetzt strikte Trennung über `hatVereinOrgs()` / `hatSponsorOrgs()` im Template.
- [x] **Bug-Fix:** Typo `Vertrags-Statūs` (Macron-u) → `Vertrags-Status` in DE-Locale.
- [x] Sidebar-Nav-Item "Statistiken" für eingeloggte User.
- [x] i18n DE/EN/FR/IT (39 Keys — 28 Sponsor + 11 Verein/Statistik-Container).
- [x] Tests STAT-01..07b (Sponsor-Service, 8) + STAT-VEREIN-01..07b (Verein-Service, 8) + STAT-CTRL-01..04 (Controller, 4) — 489 Tests gesamt.

### 11.13 — Admin-Benachrichtigung bei neuer Org-Registrierung

- [x] Jede Self-Service-Registrierung (Verein über `/organisationen` Owner-on-Create + Sponsor über `/sponsor/registrieren`) erzeugt In-App-Notifications und E-Mails an **alle** PLATFORM_ADMIN-User. Stand heute deckungsgleich mit „die Rollen, die Vereine approven können".
- [x] Neuer `AdminBenachrichtigungService` (Paket `admin`) kapselt den Versand: lädt alle Admins via `AppUserRepository.findByPlatformRolle`, ruft `NotificationService.benachrichtige` (Typ `NEUE_ORG_REGISTRIERT`, Link `/admin/verifizierungen`) und `MailService.sendePlain`. Mail-Fehler werden pro Admin als WARN-Log geschluckt — andere Admins werden nicht mitgerissen, Registrierung bleibt erfolgreich.
- [x] Titel/Body unterscheidet Verein vs. Sponsor-Organisation; Mail-Subject enthält Org-Name, Body verweist auf Verifizierungs-Queue.
- [x] Trigger in `OrganisationService.erstelleMitEigentuemer` (Verein/Stiftung via Owner-on-Create) und `SponsorRegistrierungService.registriereSponsor` — Admin-direkt-Anlage über `OrganisationService.erstelle` löst *keine* Notification aus (kein Verifizierungs-Bedarf).
- [x] Tests: ADMIN-NOTIF-01..04 (Service) + ORG-31/32 (Trigger im OrganisationService) + SR-01 erweitert — 495 Tests gesamt.

---

## Phase 12 — Customizable Task-Engine ✓

> **Paket 5.** Generische Aufgaben-Verwaltung mit Admin-Customizing — User sehen
> ihre offenen Aufgaben (Verein freigeben, Anfrage bearbeiten, Vertrag prüfen,
> Rechnung versenden) auf `/aufgaben`, und Admins definieren neue Tasktypen
> ohne Code-Änderung auf `/admin/aufgaben-definitionen`.

### 12.1 — Datenmodell + Engine

- [x] Migration V36 (`aufgaben_definition` + `aufgabe`) mit CHECK-Constraints auf `trigger_entity_typ`, `assignee_regel`, `status`. Fünf System-Seed-Definitionen für die Initial-Use-Cases (Org-Freigabe, Anfrage, Vertrag Verein+Sponsor, Rechnung).
- [x] Entities `Aufgabe` + `AufgabenDefinition` mit polymorpher Entity-Referenz (`entityTyp` + `entityId` — kein FK, weil typunabhängig). Enums `AufgabenStatus`, `TriggerEntityTyp`, `AssigneeRegel`. Bounded Context unter `ch.sponsorplatz.aufgabe`.
- [x] `AufgabenEngine.on<Entity>StatusWechsel(entity)` für ORG / ANFRAGE / VERTRAG / RECHNUNG. Kern: (1) offene Aufgaben für die Entity evaluieren — Definition.zielStatus erreicht → ERLEDIGT, Trigger-Status verlassen → ENTFALLEN; (2) aktive Definitions mit passendem Trigger laden, idempotent neue Aufgaben anlegen (existsByDefinitionIdAndEntityIdAndStatus-Guard verhindert Duplikate).
- [x] `AssigneeKontext` (Record) hält pro Entity die relevanten Org-Referenzen (Verein-Seite, Sponsor-Seite, Empfänger, Anfragender). Die Engine löst die `AssigneeRegel` damit auf — kein zusätzlicher Repo-Roundtrip.

### 12.2 — Trigger-Verkabelung

- [x] `OrganisationService.erstelle` / `verifiziere` / `suspendiere` → `onOrgStatusWechsel`
- [x] `SponsoringAnfrageService.erstelle` / `erstelleKontaktAnfrage` / `annehme` / `lehneAb` → `onAnfrageStatusWechsel`
- [x] `VertragService.erstelle` / `markiereUnterzeichnet` / `kuendige` → `onVertragStatusWechsel`
- [x] `RechnungService.erstelle` / `markiereBezahlt` / `markiereAlsBezahltViaWebhook` / `stornieren` → `onRechnungStatusWechsel`

### 12.3 — User-UI + Admin-UI

- [x] `GET /aufgaben` — „Meine Aufgaben" (Sidebar-Nav-Item für alle eingeloggten User). Zeigt offene Aufgaben des Users, abgeleitet aus Org-Mitgliedschaften (alle Rollen) + Platform-Admin-Status. Manuelles Abhaken via `POST /aufgaben/{id}/erledigen`.
- [x] `GET /admin/aufgaben-definitionen` — Liste aller Definitions, mit System-Badge + Aktiv-Toggle. Form auf `/admin/aufgaben-definitionen/{neu|/{id}/bearbeiten}` für CRUD. Trigger-Felder sind bei System-Definitionen gesperrt (fieldset disabled), damit die Engine-Verkabelung intakt bleibt.
- [x] i18n in DE/EN/FR/IT (40 Keys).

### 12.4 — Tests

- [x] AUFG-ENG-01..07: Engine-Lifecycle (Erzeugung, Auto-Erledigung, Idempotenz, mehrere Assignees pro Status-Wechsel, fehlende Org-Auflösung).
- [x] Bestehende Service-Tests (OrganisationServiceTest, SponsoringAnfrageServiceTest, VertragServiceTest, RechnungServiceTest) um `AufgabenEngine`-Mock erweitert — 502 Tests gesamt.

---

## Phase 10 — Production-Readiness & Pilot-Launch ⏳

> **Paket 4.** Plattform ist produktiv betreibbar in OCI Cloud mit Monitoring, DSG-Compliance und Error-Tracking.
> Iteration: ~4 Tage vor dem Pilot-Launch.

### 10.1 — Monitoring & Observability ✓

- [x] Spring Actuator: `/actuator/health/{liveness,readiness}` mit DB-Check
- [x] Spring Actuator: `/actuator/prometheus` für Prometheus-Scrape (in prod auf separatem Management-Port 9090, Loopback-Bind)
- [x] `logback-spring.xml` mit JSON-Encoder (logstash-encoder, prod/cloud-free)
- [x] Strukturierte Logs mit Trace-ID via MDC (`X-Trace-ID`-Header) — `TraceIdFilter` mit Format-Validierung (Log-Injection-Schutz)
- [x] **MON-W3C**: W3C-`traceparent` (OTel-Standard) hat Vorrang vor Legacy-`X-Trace-ID`, frische Span-ID pro Hop, ungültige/all-zero-IDs fallen auf fresh-Generation zurück. Response liefert beide Header. MDC trägt `traceId` (16 Byte hex) + `spanId` (8 Byte hex). Tests MON-W3C-01..04 in `MonitoringTest`.
- [ ] OCI Cloud Logging-Forwarding via Sidecar oder Direct-Push
- [x] Tests: MON-01..04 + MON-03c/d (9 Tests, alle grün)

### 10.2 — Error-Tracking ✓

- [x] Sentry Java-SDK (`sentry-spring-boot-starter-jakarta` 8.13.2) + Logback-Appender (`SentryAppender` mit `minimumEventLevel=ERROR` und `minimumBreadcrumbLevel=INFO`, in dev+prod registriert)
- [x] DSG-konform: `send-default-pii=false`, `traces-sample-rate=0`, BeforeSend-Callback entfernt User-IP
- [x] BeforeSend-Filter: NotFoundException (404) und IllegalArgumentException (400) werden nicht gesendet (Business-Errors, kein Noise)
- [x] Konfiguration als ENV: `SENTRY_DSN`, `SENTRY_ENVIRONMENT`, `SENTRY_RELEASE` — ohne DSN bleibt Sentry inaktiv (No-Op). Prod erzwingt `SENTRY_RELEASE` (kein Dev-Fallback), damit Release-Health-Tracking nicht unter dem SNAPSHOT-Tag landet.
- [x] Sentry Browser-SDK als Thymeleaf-Fragment `fragments/sentry-browser.html` (CDN-Einbindung mit SRI-`integrity`-Hash, nur JS-Errors, kein Replay) — eingebunden vor `</body>` in allen 57 nicht-Fragment-Templates
- [x] Kompatibel mit Glitchtip (self-hosted, DSG) und Sentry-EU-Cloud
- [x] Tests: SENTRY-01..05 (5 Tests, alle grün)

### 10.3 — DSG-Compliance & Public-Pages ✓

- [x] Cookie-Banner (nicht nötig: nur technisch notwendige Cookies, dokumentiert in Datenschutz §6)
- [x] `impressum.html` mit Adresse, Kontakt (bereits in Phase Operational umgesetzt)
- [x] `datenschutz.html` mit DSG-Text (bereits in Phase Operational umgesetzt)
- [x] `agb.html` mit Sponsoring-Plattform-spezifischen Bedingungen (12 Abschnitte, i18n DE/FR/IT/EN)
- [x] Footer-Links validiert, alle drei Pages (Impressum, Datenschutz, AGB) aus jedem Layout erreichbar
- [x] `/agb` in SecurityConfig als permitAll (dev + prod)
- [x] Tests: PUB-03 AGB-Route, PUB-04 Cookie-Banner-Verzicht-Dokumentation (4 Tests in InfoControllerTest)

### 10.5 — Security-Hardening (Response-Headers) ✓

- [x] `X-Content-Type-Options: nosniff` (verhindert MIME-Sniffing)
- [x] `Referrer-Policy: strict-origin-when-cross-origin` (DSG-konforme Referrer-Beschränkung)
- [x] `Permissions-Policy: camera=(), microphone=(), geolocation=(), payment=()` (Browser-Feature-Restriktion)
- [x] `Content-Security-Policy` mit striktem Default (`default-src 'self'`), Sentry-CDN-Ausnahme für script/connect
- [x] Prod: `frame-ancestors 'none'` (Clickjacking-Schutz), HSTS mit 1 Jahr max-age + includeSubDomains
- [x] Dev: `frame-src 'self'` (für H2-Console), `'unsafe-eval'` (Dev-Tools)
- [x] Tests: SEC-HDR-01..05 (5 Tests, alle grün)

### 10.4 — Pilot-Launch-Checkliste → **verschoben auf Phase 14**

Der ursprüngliche „Pilot-Launch"-Block lebt jetzt als **Phase 14 (Produktivschaltung)** weiter unten in diesem Dokument. Begründung: zwischen den abgeschlossenen Production-Readiness-Bausteinen (10.1–10.3, 10.5) und dem eigentlichen Cutover passen noch Phase-13-Features (A11y-auth-Smoke, 2FA, OIDC), die vor dem ersten Echt-User landen sollten. Die alten 10.4-Items (HTTPS, SMTP-prod, SPF/DKIM/DMARC, OCI-Backups, DNS, Pilot-Welle) sind 1:1 in Phase 14 übernommen.

---

## Phase 13 — Pre-Pilot-Hardening ⏳

> **Paket 5.** Sicherheits- und A11y-Themen, die vor dem ersten echten
> Pilot-Benutzer in Prod abgeschlossen sein sollten. Nicht-blockierend
> für die reine Produktivschaltung, aber stark empfohlen — Login,
> Identitäten und Zugänglichkeit sind die Themen, die ein Reviewer
> oder Sicherheitsbeauftragter zuerst anschaut.

### 13.1 — A11y-Smoke-Suite für authentifizierte Seiten ✅

> Backlog-Eintrag in V40 (`A11y-Smoke-Suite für authentifizierte Seiten`).

- [x] Login-Helper in der Test-Suite — Playwright form-login mit `dev@sponsorplatz.ch`/`dev` (DevSeedRunner) in dedicated `authContext`
- [x] `A11ySmokeIT` um eine zweite Schleife auf `/dashboard`, `/aufgaben`, `/meine-anfragen`, `/einstellungen` erweitert
- [x] Test-IDs A11Y-07..10 in TESTSTRATEGIE.md eingetragen
- [x] WCAG-AA-Fixes für Befunde aus dem ersten Lauf: `--dash-muted` heller, `.dash-chart-axis` explizit `#3F3A8C`, `.dash-avatar-plus`/`.dash-btn-pill` auf `--dash-coral-text`, Sidebar-Lang ohne Wrapper-Opacity, Dashboard-Selects mit `aria-label` (i18n in de_CH/en/fr_CH/it_CH)
- [ ] `/onboarding` bewusst raus — Dev-Seed-User ist `PLATFORM_ADMIN`, wird vom `OnboardingController` direkt auf `/dashboard` umgeleitet; dedizierter Non-Admin-Test-User ist Folge-Arbeit via `E2EFixtures`

### 13.2 — Zwei-Faktor-Authentifizierung (TOTP)

> Backlog-Eintrag in V39 (`Zwei-Faktor-Authentifizierung (2FA)`).

- [ ] DB-Spalten auf `app_user`: `totp_secret`, `totp_enabled`, `backup_codes_hashed`
- [ ] Setup-Flow `/einstellungen/2fa` mit QR-Code (Library: `dev.samstevens.totp`)
- [ ] Login-Flow mit zweitem Schritt (POST `/login/2fa`) und eigener SecurityFilter-Stage
- [ ] 10 Backup-Codes (BCrypt-gehasht, einmalig)
- [ ] Recovery-Reset durch Plattform-Admin im `/admin/benutzer`-UI
- [ ] Pflicht für PLATFORM_ADMIN, optional für Vereins-/Sponsor-Owner (Nudge nach erstem Login)
- [ ] Audit-Log für Enable/Disable/Recovery
- [ ] Tests AUTH-2FA-01..10 (Setup, Verify, Reuse-Protection, Replay-Window, Lockout, Recovery, Admin-Reset)
- [ ] Bei OIDC-Login: 2FA kommt vom IdP — kein eigener Schritt; Policy nur auf Form-Login-User anwenden

### 13.3 — OIDC-Identity-Provider-Anbindung ✅

> Backlog-Eintrag in V27 (`OIDC-Identity-Provider-Anbindung`).
> `spring-boot-starter-oauth2-client` ist im POM bereits vorhanden.

- [x] Provider-Registrierung pro Tenant: `IdentityProvider`-Enum mit ENTRA_ID, GOOGLE, SWISSID, EDU_ID; Property-Templates für alle 4 in `application-prod.properties`; V46 droppt `chk_provider`-Allowlist (Pattern V44/V45)
- [x] Mapping vom OIDC-`sub` auf `AppUser` mit Auto-Anlage: 3-stufige Lookup-Logik in `SponsorplatzOidcUserService.identifizierenOderAnlegen` (Subject-Lookup → Email-Match → JIT-Provisioning)
- [x] Domain-Whitelist (Slice A): `sponsorplatz.oidc.email-domain-whitelist=` schützt JIT + Email-Match gegen Account-Takeover; 5 Tests SSO-20..24
- [x] Logout-Flow RP-initiated (Slice B): `OidcClientInitiatedLogoutSuccessHandler` mit `{baseUrl}/` als `post_logout_redirect_uri`; Fallback auf lokales Logout wenn kein OAuth2-Client konfiguriert; in dev + prod SecurityFilterChain
- [x] Rollen-Mapping über `sponsorplatz.oidc.rollen-mapping` — implementiert in `OidcConfig`/`SponsorplatzOidcUserService.wendeGroupMappingAn`; Re-Sync bei jedem Login (SSO-06/07)
- [ ] Test-Stärkung deferred: `OidcLoginFlowIT` mit `mock-oauth2-server` für End-to-End (SSO-01/08/09) — Mapping-Logik ist via SSO-02..07 + SSO-20..24 voll abgedeckt, Token-Verifikation ist Spring-Security-upstream-getestet
- [ ] Tests SEC-OIDC-01..05

---

## Phase 14 — Produktivschaltung sponsorplatz.ch 🔜

> **Paket 6.** Der echte Cutover — übernommen aus dem alten 10.4-Block,
> erweitert um Punkte aus dem `Produktivschaltung`-Backlog-Item (API-erfasst).
> Reihenfolge ist bewusst: Infrastruktur (14.1) → Cutover-Validation (14.2)
> → Pilot-Welle (14.3).

### 14.1 — Infrastruktur

- [ ] HTTPS via OCI Load Balancer + Zertifikat (Let's Encrypt automatisch oder OCI-Cert)
- [ ] Renewal-Cron (certbot oder OCI-Vault-rotation)
- [ ] Prod-SMTP statt MailHog — separater Mail-User in OCI-Vault
- [ ] `sponsorplatz.mail.live=true` setzen sobald SMTP+SPF+DKIM live
- [ ] SPF / DKIM / DMARC für sponsorplatz.ch im DNS einrichten + testen (mail-tester.com ≥ 9/10)
- [ ] Backup-Spiegel in OCI Object Storage (`sponsorplatz-backups`), Lifecycle-Policy + Restore-Test quartalsweise
- [ ] DNS `sponsorplatz.ch` + `www`-Redirect aufschalten, IPv6 enabled
- [ ] CDN/Cache-Policy für `/css/`, `/images/`, `/medien/` (Caddy oder OCI LB)

### 14.2 — Cutover-Validation ✅

- [x] Smoke-Test-Suite lokal (`SmokeIT` via `mvn verify -P e2e`)
- [x] CD-Smoke gegen Prod-URL nach jedem Deploy — `/login`-200-Probe seit Pre-Pilot in beiden CD-Workflows (OCI + Azure)
- [x] ~~OCIR-Retention~~ — obsolet, Migration auf GHCR (2026-05-22); GHCR-Retention via GitHub-Packages-UI bei Bedarf
- [x] Sentry-Release-Tagging im CD-Workflow eingebaut — SENTRY_RELEASE-Sync in beide VM-`.env`, optionaler `getsentry/action-release`-Step (off-by-default ohne SENTRY_AUTH_TOKEN); cloud-free + cloud-azure profiles ergänzt
- [x] Rollback-Pfad dokumentiert in `infra/staging-free/README.md` + `infra/envs/azure-staging/README.md` (Image-Tag-Pin via `IMAGE_URL` in `.env`, kein DB-Rollback nötig wegen additiver Migrations-Policy)

### 14.3 — Pilot-Welle

- [ ] Onboarding 5 echter CH-Sport-/Health-Vereine (Pilot-Welle 1)
- [ ] Onboarding 3 Sponsoring-Marken (Krankenkasse/Pharma/Fitness — Pilot-Welle 2)
- [ ] Feedback-Kanal: `/support`-Formular + dediziertes Slack-Channel / Email-Alias
- [ ] Public-Launch-Kommunikation: Blog-Post, LinkedIn, lokale Sport-Verbände
- [ ] Wöchentlicher Retro-Review für die ersten 4 Pilot-Wochen

---

## Phase 15 — Post-Pilot — Wachstum 📋

> **Paket 7.** Features die erst Sinn machen, wenn echte Verträge,
> Mahnstufen und Geldflüsse existieren. Bewusst nicht vor dem Pilot —
> wir wollen die Use-Cases beobachten bevor wir große Integrationen
> bauen, deren Anforderungen sich aus echten Vereinen ergeben.

### 15.1 — Echte Zahlungs-Provider-Integration

> Heute: `PaymentService`-Interface + `StubPaymentProvider` (Phase 9.2)
> als Vorbereitung. Webhook-Endpoint `/payment/webhook/{provider}` ist
> signaturverifiziert (siehe `PaymentWebhookController`).

- [ ] Stripe Connect für CH-Vereine (Standard) evaluieren — Pro: Volumen, Browser-SDKs; Con: ausländischer Anbieter
- [ ] PostFinance Checkout / Datatrans als CH-native Alternative — Pro: Datenresidenz, Bankvertrauen
- [ ] Live-Webhook-Signaturen (HMAC pro Provider)
- [ ] PCI-DSS-SAQ-A-Compliance-Check (wenn Karten-Daten nur via Provider-iframe fließen, SAQ-A reicht)
- [ ] Refund-Flow + Storno-Audit-Log
- [ ] Tests: PAY-PROV-01..N (Real-Provider-Sandbox-Calls)

### 15.2 — Mahnwesen

- [ ] Mahnstufen-Konfiguration auf Plattform-Ebene (Default: Erinnerung +7d, 1. Mahnung +14d, 2. Mahnung +28d, Inkasso-Übergabe +60d)
- [ ] Pro Verein anpassbar (z.B. längere Fristen für etablierte Sponsoren-Beziehungen)
- [ ] Mahnungs-Vorlagen als Template (Verein-Branding, dynamische Felder)
- [ ] Schweizer Inkasso-Schnittstelle (z.B. Creditreform, Intrum) — als Pluggable-Service
- [ ] DSG-konformer Datenexport an Inkasso (Minimaldaten, dokumentiert in Datenschutz)
- [ ] Audit-Log für jeden Mahnschritt (was wurde wann von wem ausgelöst)
- [ ] Tests: MAHN-01..N

### 15.3 — Multi-Cloud — Azure als zweite Zone

> Heutiges OCI Always-Free ist Single-Point-of-Failure (eine VM, eine Region,
> ein Provider). Azure wird als zweite Zone vorbereitet — initial als
> **Warm-DR** (Promote per DNS-Switch), später als Aktiv-Aktiv-Split.
> Entscheidung dokumentiert in [`docs/adr/0009-multi-cloud-azure-als-dr-zone.md`](../docs/adr/0009-multi-cloud-azure-als-dr-zone.md).

**Slices in TDD-Reihenfolge** — Slice 1+2 laufen App-seitig, der Rest ist Ops.

- [x] **Slice 1 — `StorageService`-Abstraktion auf Azure ziehen.**
      `AzureBlobStorageService` (`@ConditionalOnProperty(...="azure")`) +
      `AzureStorageConfig` (Default-Auth via Managed Identity, optional
      Connection-String für lokale Tests). Tests: CLOUD-STO-AZ-01..06.
- [x] **Slice 2 — `BackupCloudUploader` für Azure.**
      `AzureBackupCloudUploader` analog OCI; Bucket `sponsorplatz-backups`.
      Tests: CLOUD-BKP-AZ-01..04.
- [x] **Slice 3 — Terraform-Modul `infra/envs/azure-staging/`.**
      Resource Group + VNet + NSG (22/80/443) + VM Standard_B2s +
      Azure Database for PostgreSQL Flexible Server (Burstable B1ms,
      VNet-privat) + Azure Container Registry (Basic, MSI-only) +
      Storage Account mit zwei Containern (`uploads`, `backups`) +
      User-Assigned Managed Identity mit `AcrPull` + `Storage Blob Data
      Contributor`-Rollen. cloud-init.yaml.tftpl + `cloud-azure` Spring-
      Profil.
- [x] **Slice 4 — CD-Workflow `cd-azure-staging.yml`.**
      Eigener Workflow parallel zu OCI (NICHT in `cd-staging-free.yml`
      einklemmen). Build einmal mit `docker buildx`, Trivy-Scan, Push
      nach ACR via Service-Principal mit `AcrPush`-Rolle, SSH-Deploy auf
      Azure-VM mit MSI-`az acr login`-Refresh, Smoke gegen
      `https://${vars.AZURE_STAGING_DOMAIN}/login`. Preflight-Job skipt
      sauber, wenn `AZURE_VM_IP`/`AZURE_ACR_LOGIN_SERVER` Vars fehlen.
- [ ] **Slice 5 — DNS-Failover via Cloudflare.**
      Zwei A-Records: OCI primary (weight 100), Azure failover (weight 0).
      Health-Check auf `/login` alle 60s, Threshold 3. Promote-Script
      `infra/scripts/promote-azure.sh` + Runbook
      `docs/runbooks/dr-failover.md` (Dry-Run-getestet, Ziel < 30 min).
- [ ] **Slice 6 — Backup-Cross-Replication OCI ↔ Azure Blob.**
      `BackupService` lädt in **beide** Provider-Buckets, wenn beide
      Uploader im Context sind. Refactor: `Optional<BackupCloudUploader>` →
      `List<BackupCloudUploader>`. Test: BKP-X-01..03.
- [ ] **Slice 7 — Beidseitiger Smoke + `X-Served-By`-Header.**
      Neuer CI-Job `smoke-multicloud` (manueller `workflow_dispatch`),
      verifiziert beide URLs + Header. Kleiner App-Change: `HostnameFilter`
      setzt `X-Served-By: <hostname>` damit der Smoke beweisen kann
      welche Cloud geantwortet hat.

**Kostenhinweis:** Azure ist **nicht** Free-Tier — Standard_B2s + Flex
Postgres B1ms + Blob ≈ CHF 50–80/Monat. Vor Slice 3 Budget freigeben.

**Tests:** CLOUD-STO-AZ-01..06, CLOUD-BKP-AZ-01..04, BKP-X-01..03,
SMOKE-MC-01..02.

### 15.4 — Datei-Backup + Restore (Sponsoring-Files-Roundtrip) ✓

> Parallel zum bestehenden DB-Backup: Medien-Uploads als portables ZIP
> sichern und restoren — provider-agnostisch via `StorageService`,
> arbeitet mit lokal, OCI und Azure.

- [x] `StorageService.speichereBytes(byte[], contentType, pfad)` als
      Restore-Schnittstelle in allen drei Impls
- [x] `DateiBackupService.erstelleDateiBackup()` — walkt `MedienAsset`-
      Repository, streamt jede Datei in ZIP, orphaned Records werden
      skipped + im Audit dokumentiert
- [x] `DateiBackupRestoreService.restore(byte[], ausgefuehrtVon)` —
      liest ZIP, Path-Traversal-Schutz, Content-Type aus Dateinamen-
      Heuristik, ruft `speichereBytes` pro Entry
- [x] Admin-UI: `/admin/datei-backups` mit Create/Download/Delete/
      Restore (RESTORE-Bestätigungstext wie DB-Restore)
- [x] Tests: DATEI-BACKUP-01..05, DATEI-RESTORE-01..04,
      CLOUD-STO-07, CLOUD-STO-AZ-07

### 15.5 — Weitere Post-Pilot-Themen

Diese landen erst nach Pilot-Feedback im Backlog (`/admin/backlog` oder via REST-API `POST /api/backlog`). Kandidaten heute schon im Blick:

- Mehrwertsteuer-Berechnung (8.1 % CH-Standardsatz, Mehrwertsteuer-Nummer am Sponsor)
- Wiederkehrende Sponsoring-Abos (monatlich/jährlich) als Pendant zur Einmal-Anfrage
- Public API für Vereine (`/api/v1/verein/{slug}/...`) zur Anbindung an Vereins-Websites
- Mobile-App / PWA-Optimierung (heute nur responsive Web)
- Multi-Tenant-Bereich für Verbände (Dachverband → Mitgliedsvereine, eigene Subdomain)

---

## Definition: MVP fertig

- 5+ verifizierte Vereine
- 10+ veröffentlichte Projekte
- 5+ Sponsor-Orgs registriert
- 10+ Anfragen, davon 3+ angenommen
- Test-Suite ≥ 60 Test-Klassen
- DSG-Datenexport funktioniert
- Marktplatz < 300 ms p95
