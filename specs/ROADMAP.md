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
- [ ] **Mahnwesen** (Spec §7, V36) — nächste Iteration: `mahnstufe`/`letzte_mahnung_am`-Spalten, `MahnungsCronJob` täglich 06:00, Tests MAHN-01..04.

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

## Phase 10 — Production-Readiness & Pilot-Launch ⏳

> **Paket 4.** Plattform ist produktiv betreibbar in OCI Cloud mit Monitoring, DSG-Compliance und Error-Tracking.
> Iteration: ~4 Tage vor dem Pilot-Launch.

### 10.1 — Monitoring & Observability ✓

- [x] Spring Actuator: `/actuator/health/{liveness,readiness}` mit DB-Check
- [x] Spring Actuator: `/actuator/prometheus` für Prometheus-Scrape (in prod auf separatem Management-Port 9090, Loopback-Bind)
- [x] `logback-spring.xml` mit JSON-Encoder (logstash-encoder, prod/cloud-free)
- [x] Strukturierte Logs mit Trace-ID via MDC (`X-Trace-ID`-Header) — `TraceIdFilter` mit Format-Validierung (Log-Injection-Schutz)
- [ ] **MON-W3C**: Migration von `X-Trace-ID` (UUID) zu W3C-`traceparent` (`00-<32hex>-<16hex>-01`) sobald Distributed-Tracing-Backend (Tempo/Jaeger) eingeführt wird — OpenTelemetry-kompatibel
- [ ] OCI Cloud Logging-Forwarding via Sidecar oder Direct-Push
- [x] Tests: MON-01..04 + MON-03c/d (9 Tests, alle grün)

### 10.2 — Error-Tracking

- [ ] Glitchtip self-hosted (DSG-konform) oder Sentry-EU-Cloud
- [ ] Sentry-Java-SDK ins Backend, Release-Tagging via CI
- [ ] Sentry-Browser-SDK ins Layout (nur Errors, kein Replay → DSG)
- [ ] Konfiguration als ENV (`SENTRY_DSN`, `SENTRY_ENVIRONMENT`)

### 10.3 — DSG-Compliance & Public-Pages

- [ ] Cookie-Banner (nur technisch notwendige Cookies, kein Tracking)
- [ ] `impressum.html` mit Adresse, Kontakt, MwSt-Nummer
- [ ] `datenschutz.html` mit DSG-Text (Daten-Erhebung, Speicherdauer, Auskunfts-/Lösch-Rechte)
- [ ] `agb.html` mit Sponsoring-Plattform-spezifischen Bedingungen
- [ ] Footer-Links validiert, alle drei Pages aus jedem Layout erreichbar
- [ ] Tests: PUB-01 Impressum, PUB-02 Datenschutz, PUB-03 AGB, PUB-04 Cookie-Banner-Default

### 10.4 — Pilot-Launch-Checkliste

- [ ] HTTPS via OCI Load Balancer + Zertifikat (Let's Encrypt oder OCI-Cert)
- [ ] SMTP-Konfiguration prod (statt MailHog) — getrennter Mail-User
- [ ] SPF / DKIM / DMARC für Mail-Domain einrichten und testen
- [ ] Backups in OCI Object Storage spiegeln, Restore-Test einmal pro Quartal
- [ ] DNS `sponsorplatz.ch` + `www`-Redirect, IPv6 enabled
- [ ] Smoke-Test-Suite gegen prod-URL (Login, Marktplatz, Anfrage, Logout)
- [ ] Onboarding 5 echter CH-Sport-/Health-Vereine (Pilot-Welle)
- [ ] Public-Launch-Kommunikation: Blog-Post, LinkedIn, lokale Sport-Verbände

---

## Definition: MVP fertig

- 5+ verifizierte Vereine
- 10+ veröffentlichte Projekte
- 5+ Sponsor-Orgs registriert
- 10+ Anfragen, davon 3+ angenommen
- Test-Suite ≥ 60 Test-Klassen
- DSG-Datenexport funktioniert
- Marktplatz < 300 ms p95
