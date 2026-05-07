# Teststrategie

## Test-Ebenen

| Ebene | Werkzeug | Zweck |
|---|---|---|
| Unit | JUnit 5 + Mockito + AssertJ | Service-Logik, Konverter, Validierung |
| Repository | `@DataJpaTest` (H2) | JPA-Queries, Constraints |
| Web | `@WebMvcTest` + MockMvc | HTTP-Routen, Redirects |
| Security | `@SpringBootTest` mit Profilen | Form-Login, AccessControl, OIDC |
| Container | Testcontainers (PostgreSQL) | echte DB für Migrations-Tests |
| Smoke | manuell + CI Healthcheck | Startup, Browser-Happy-Path |

## Konventionen

- Naming: `<Klasse>Test` für Unit-/Web-/Repository-Tests, `<Klasse>IT` für Integration
- Test-IDs nach Schema `<Bereich>-<Nummer>`
- Test-IDs in der Tabelle unten pflegen
- Spec-Anforderungen referenzieren ihre Test-ID

## Testfall-Katalog

### Phase 0 (Skelett)

| ID | Test-Klasse | Beschreibung |
|---|---|---|
| SP-01 | `PlatformApplicationTests` | Spring-Context startet sauber |
| SP-02 | `HomeControllerTest` | GET / → 200 + index-Template |

### Phase 0.1 (Organisation)

| ID | Test-Klasse | Beschreibung |
|---|---|---|
| **ORG-01** | `SlugGeneratorTest` | Umlaute → ASCII (`Zürich → zuerich`) |
| **ORG-02** | `SlugGeneratorTest` | Sonderzeichen entfernt, mehrfach-Bindestriche reduziert |
| **ORG-03** | `OrganisationRepositoryTest` | Persistieren + finden, Default-Werte korrekt |
| **ORG-04** | `OrganisationRepositoryTest` | `slug` UNIQUE — Duplikat wirft Exception |
| **ORG-05** | `OrganisationServiceTest` | Speichern mit Auto-Slug aus Name |
| **ORG-06** | `OrganisationServiceTest` | Speichern wirft bei Slug-Konflikt |
| **ORG-07** | `OrganisationServiceTest` | Validierung: name zu kurz → IllegalArgumentException |
| **ORG-08** | `OrganisationControllerTest` | GET /organisationen → 200 + Liste |
| **ORG-09** | `OrganisationControllerTest` | POST /organisationen/speichern → Redirect 302 |
| **ORG-10** | `OrganisationControllerTest` | GET /organisationen/{slug} → 200 + Detail |

| **ORG-11** | `OrganisationServiceTest` | loesche wirft IllegalStateException wenn Mitgliedschaften vorhanden |
| **ORG-12** | `OrganisationControllerTest` | GET /organisationen/{slug}/bearbeiten ohne ORG_EDITOR → 403 |

### Phase 0.2 (AppUser, Mitgliedschaft, AccessControl)

#### AppUser (AU)

| ID | Test-Klasse | Beschreibung |
|---|---|---|
| **AU-01** | `AppUserRepositoryTest` | E-Mail UNIQUE — Duplikat wirft DataIntegrityViolationException |
| **AU-02** | `AppUserRepositoryTest` | Persistieren + laden; `aktiv` Default = true |
| **AU-03** | `AppUserServiceTest` | `registriere` hasht Passwort via BCrypt (Klartext ≠ gespeicherter Hash) |
| **AU-04** | `AppUserServiceTest` | `registriere` bei doppelter E-Mail → IllegalArgumentException |
| **AU-05** | `AppUserServiceTest` | `registriere` mit leerem Anzeigename → IllegalArgumentException |

#### Mitgliedschaft (MG)

| ID | Test-Klasse | Beschreibung |
|---|---|---|
| **MG-01** | `MitgliedschaftRepositoryTest` | UNIQUE (user_id, org_id) — Duplikat wirft DataIntegrityViolationException |
| **MG-02** | `MitgliedschaftRepositoryTest` | `existsByUserAndOrgAndRolleIn` findet korrekte Treffer |
| **MG-03** | `MitgliedschaftServiceTest` | `fuegeHinzu` — zweite Mitgliedschaft gleiche org/user → IllegalStateException |
| **MG-04** | `MitgliedschaftServiceTest` | `findeNachOrg` gibt alle Mitglieder einer Org zurück |

#### AccessControl (AC)

| ID | Test-Klasse | Beschreibung |
|---|---|---|
| **AC-01** | `AccessControlTest` | nicht eingeloggt (null auth) → `kannOrgEditieren` false |
| **AC-02** | `AccessControlTest` | PLATFORM_ADMIN → `kannOrgEditieren` immer true, auch ohne Mitgliedschaft |
| **AC-03** | `AccessControlTest` | ORG_EDITOR → `kannOrgEditieren` true |
| **AC-04** | `AccessControlTest` | ORG_VIEWER → `kannOrgEditieren` false |
| **AC-05** | `AccessControlTest` | ORG_OWNER → `kannOrgVerwalten` true |
| **AC-06** | `AccessControlTest` | ORG_EDITOR → `kannOrgVerwalten` false |
| **AC-07** | `AccessControlTest` | PLATFORM_ADMIN → `kannOrgVerwalten` true (ohne Mitgliedschaft) |
| **AC-08** | `AccessControlTest` | kein Mitglied dieser Org → `kannOrgEditieren` false (andere Org-Mitgliedschaft zählt nicht) |

### Phase 0.2.4 — View-DTOs (H1-Fix)

JPA-Entities verlassen den Service-Layer nicht mehr — Controller mappen vor `model.addAttribute` zu Read-only-View-DTOs (Java records). Vermeidet LazyInitializationException-Risiko und entkoppelt Persistenz von Präsentation.

| ID | Test-Klasse | Beschreibung |
|---|---|---|
| **VIEW-01** | `OrganisationViewTest` | `OrganisationView.von(entity)` mappt alle relevanten Felder |
| **VIEW-02** | `OrganisationViewTest` | `OrganisationView.von(List<Entity>)` mappt Liste |
| **VIEW-03** | `ProjektViewTest` | `ProjektView.von(entity)` mappt inkl. `OrganisationKurzView` für `org` |
| **VIEW-04** | `MitgliedViewTest` | `MitgliedView.von(mitgliedschaft)` flacht `user.anzeigename`/`user.email` ein |
| **VIEW-05** | `SponsoringPaketViewTest` | `SponsoringPaketView.von(entity)` mappt korrekt |
| **VIEW-06** | `AnfrageViewTest` | `AnfrageView.von(anfrage)` flacht `paket.name` zu `paketName` ein |
| **VIEW-07** | `WatchlistEintragViewTest` | `WatchlistEintragView.von(entity)` mappt mit nested `ProjektView` |

### Phase 0.2.3 — Mass-Assignment-Defense (K3-Fix)

`OrganisationFormDto` hatte ein `id`-Feld, was Mass-Assignment ermöglichte: Ein POST mit hidden-id konnte beliebige fremde Datensätze überschreiben. **Fix:** `id` aus dem DTO entfernt; Routes gesplittet — `POST /organisationen` (Create) und `POST /organisationen/{slug}` (Update). Update-Route ist durch `kannOrgEditierenNachSlug` geschützt.

| ID | Test-Klasse | Beschreibung |
|---|---|---|
| **ORG-17** | `OrganisationControllerTest` | POST `/organisationen` (Create) → 302 Redirect auf Detail-Seite |
| **ORG-18** | `OrganisationControllerTest` | POST `/organisationen/{slug}` (Update) mit Edit-Recht → 302 |
| **ORG-19** | `OrganisationControllerTest` | POST `/organisationen/{slug}` ohne Edit-Recht → 403 |
| **ORG-20** | `OrganisationServiceTest` | `aktualisiere(slug, dto)` lädt via Slug, ignoriert evtl. id im DTO |
| **ORG-21** | `OrganisationServiceTest` | `aktualisiere` mit unbekanntem Slug → NotFoundException (404) |

### Phase 3 — Health-Fokus (Branche-Enum)

| ID | Test-Klasse | Beschreibung |
|---|---|---|
| **ORG-22** | `OrganisationServiceTest` | `erstelle` ohne Branche → IllegalArgumentException (Health-Fokus, Pflichtfeld) |
| **ORG-23** | `OrganisationServiceTest` | `erstelle` akzeptiert alle elf Health-Branchen (`Branche.values()`) |

> **Hintergrund:** Mit Konzept v3.1 (Schärfung 05.05.2026) ist Sponsorplatz strikt auf
> Sport und Gesundheit positioniert. Die `Branche` ist Pflichtfeld und auf elf Werte
> beschränkt — abgesichert durch Java-Enum (Compile-Time), DTO-`@NotNull` (Runtime-Form),
> Service-Validierung (Runtime-Service) und DB-CHECK-Constraint (V12, Persistence).

### Phase 0.2.2 — Exception-Handling (K2-Fix)

`GlobalExceptionHandler` gibt gerenderte Error-Page (`error.html`) statt Plain-Text zurück. Korrektes HTTP-Statuscode-Mapping:

| Exception | HTTP | Verwendung |
|---|---|---|
| `NotFoundException` | 404 | Slug nicht gefunden, Ressource existiert nicht |
| `IllegalArgumentException` | 400 | Ungültige Eingabe (z. B. Slug-Konflikt) |
| `IllegalStateException` | 409 | Inkonsistenter Zustand (z. B. Org löschen mit Mitgliedschaften) |
| `AccessDeniedException` | 403 | Berechtigung fehlt |

#### Exception-Handler (EXC)

| ID | Test-Klasse | Beschreibung |
|---|---|---|
| **EXC-01** | `GlobalExceptionHandlerTest` | `NotFoundException` → 404 + View `error` mit `status=404` |
| **EXC-02** | `GlobalExceptionHandlerTest` | `IllegalArgumentException` → 400 + View `error` |
| **EXC-03** | `GlobalExceptionHandlerTest` | `IllegalStateException` → 409 + View `error` |
| **EXC-04** | `GlobalExceptionHandlerTest` | `AccessDeniedException` → 403 + View `error` |
| **EXC-05** | `GlobalExceptionHandlerTest` | Error-View enthält Model-Attribute `status`, `error`, `message` |
| **ORG-16** | `OrganisationControllerTest` | GET `/organisationen/{slug}` mit unbekanntem Slug → 404 (nicht 400) |

### Dashboard (DASH)

UI-Skelett für angemeldete Benutzer unter `/dashboard`. Service-Aufrufe über `DashboardService` verkabelt — Controller enthält keine Repo-Zugriffe oder Entity-Referenzen.

| ID | Test-Klasse | Beschreibung |
|---|---|---|
| **DASH-01** | `DashboardControllerTest` | GET `/dashboard` anonym → Redirect zu `/login` |
| **DASH-02** | `DashboardControllerTest` | GET `/dashboard` eingeloggt → 200 + View `dashboard` |
| **DASH-03** | `DashboardControllerTest` | Model enthält `aktuellerMonat`, `aktuelleKw`, `anzahlOrganisationen`, `anzahlProjekte`, `anzahlAnfragen`, `anzahlOffeneAnfragen` |
| **DASH-04** | `DashboardControllerTest` | Model-Werte kommen aus `DashboardService.ladeDashboardDaten(email)` |
| **DASH-05** | `DashboardServiceTest` | `ladeDashboardDaten` für User ohne Mitgliedschaften → alle Zähler 0 |
| **DASH-06** | `DashboardServiceTest` | `ladeDashboardDaten` zählt Orgs korrekt (= Anzahl Mitgliedschaften) |
| **DASH-07** | `DashboardServiceTest` | `ladeDashboardDaten` zählt nur öffentliche Projekte der eigenen Orgs |
| **DASH-08** | `DashboardServiceTest` | `ladeDashboardDaten` zählt eingehende Anfragen und offene Anfragen separat |
| **DASH-09** | `DashboardServiceTest` | `ladeDashboardDaten` für unbekannte E-Mail → alle Zähler 0 |
| **DASH-10** | `DashboardServiceTest` | `ladeDashboardDaten` ruft die Aggregat-Methoden **genau einmal** auf (H3-Fix: keine N+1-Loops mehr) |

### H3-Fix: Repository-Aggregat-Methoden für Dashboard

`DashboardService` ruft jetzt für N Mitglied-Orgs **3 statt 3·N Queries** auf. Verwendete Repository-Methoden:

- `MitgliedschaftRepository.findOrgIdsByUserId(userId)` — direkte Projection auf `org_id` (kein Org-Lazy-Load)
- `ProjektRepository.countByOrgIdInAndSichtbarkeit(orgIds, OEFFENTLICH)`
- `SponsoringAnfrageRepository.countByEmpfaengerOrgIdIn(orgIds)`
- `SponsoringAnfrageRepository.countByEmpfaengerOrgIdInAndStatus(orgIds, NEU)`

### Phase 1.3 (Admin-Verifizierung, Zefix, Einladungen)

#### Admin-Verifizierung (ADM)

| ID | Test-Klasse | Beschreibung |
|---|---|---|
| **ADM-01** | `AdminVerifizierungControllerTest` | GET `/admin/verifizierungen` ohne PLATFORM_ADMIN → 403 |
| **ADM-02** | `AdminVerifizierungControllerTest` | GET `/admin/verifizierungen` als PLATFORM_ADMIN → 200 + Liste der PENDING-Orgs |
| **ADM-03** | `AdminVerifizierungControllerTest` | POST `/admin/verifizierungen/{id}/verifizieren` → Status VERIFIED, `verifiziert_am` gesetzt |
| **ADM-04** | `AdminVerifizierungControllerTest` | POST `/admin/verifizierungen/{id}/ablehnen` → Status SUSPENDED |
| **ADM-05** | `OrganisationServiceTest` | `verifiziere(id)` setzt Status + verifziertAm |
| **ADM-06** | `OrganisationServiceTest` | `verifiziere(id)` bei nicht-PENDING → IllegalStateException |
| **ADM-07** | `OrganisationServiceTest` | `suspendiere(id)` setzt Status SUSPENDED |
| **ADM-08** | `OrganisationServiceTest` | `findePending()` gibt nur PENDING-Orgs zurück |

#### Zefix-Stub (ZFX)

| ID | Test-Klasse | Beschreibung |
|---|---|---|
| **ZFX-01** | `ZefixServiceTest` | Stub-Implementierung gibt immer `Optional.empty()` zurück |
| **ZFX-02** | `OrganisationServiceTest` | `erstelleMitZefixPruefung` ruft Zefix auf; bei Treffer wird UID gesetzt + Status VERIFIED |

#### Einladungen (EINL)

| ID | Test-Klasse | Beschreibung |
|---|---|---|
| **EINL-01** | `EinladungsServiceTest` | `erstelleEinladung` generiert Token + sendet Mail |
| **EINL-02** | `EinladungsServiceTest` | `nimmAn(token)` erstellt Mitgliedschaft + löscht Einladung |
| **EINL-03** | `EinladungsServiceTest` | `nimmAn` mit abgelaufenem Token → IllegalStateException |
| **EINL-04** | `EinladungsServiceTest` | `nimmAn` mit unbekanntem Token → IllegalArgumentException |
| **EINL-05** | `EinladungsControllerTest` | POST `/einladung/annehmen` mit gültigem Token → Erfolgsseite, ruft `nimmAn` |
| **EINL-06** | `EinladungsControllerTest` | POST `/einladung/annehmen` mit ungültigem Token → 400 (error.html) |
| **EINL-07** | `EinladungsControllerTest` | **GET** `/einladung/annehmen?token=...` zeigt Vorschau-Page, ruft `nimmAn` **NICHT** auf (Outlook-/Slack-Crawler-Schutz) |
| **EINL-08** | `EinladungsControllerTest` | GET mit ungültigem Token → 400 (error.html) — keine State-Änderung |
| **EINL-09** | `EinladungsServiceTest` | `ladeVorschau(token)` validiert Token (Existenz + Ablauf) — wirft analog zu `nimmAn`, aber **mutiert nichts** |
| **EINL-10** | `EinladungsServiceTest` | `erstelleEinladung` mit ungültigem E-Mail-Format → `IllegalArgumentException` (H2-Fix: garbage wie „nicht-email" oder „a@b" wird abgelehnt) |
| **EINL-11** | `EinladungsServiceTest` | `erstelleEinladung` publishes `EinladungErstelltEvent` mit Token + Empfängerdaten (H4-Fix: kein direkter Mail-Aufruf mehr) |
| **EINL-12** | `EinladungsMailListenerTest` | `@TransactionalEventListener(AFTER_COMMIT)`-Listener sendet Mail beim Event; Mail-Failure führt nicht zur Service-Exception (DB-State bleibt konsistent) |
| **EINL-13** | `EinladungsServiceTest` | `erstelleEinladung` mit existierender, **abgelaufener** Einladung für dieselbe E-Mail → löscht die alte + legt neue an (M2-Fix: Re-Einladung erlaubt) |
| **EINL-14** | `EinladungsCleanupJobTest` | Scheduled-Cleanup ruft `deleteByGueltigBisBefore(now())` auf — entfernt abgelaufene Einladungen aus der DB |
| **TG-01** | `TokenGeneratorTest` | `generiere()` erzeugt 64-stelligen Hex-String (32 Bytes random) |
| **TG-02** | `TokenGeneratorTest` | Zwei Aufrufe liefern unterschiedliche Tokens (Eindeutigkeit) |
| **EINL-15** | `EinladungsServiceTest` | `nimmAn` ist idempotent — zweiter Aufruf desselben Tokens → kein doppelter `fuegeHinzu` (M4-Fix via `angenommen_am`-Marker statt `delete`) |
| **EINL-16** | `EinladungsControllerTest` | POST `/einladung/annehmen` bei „nicht registriert"-Fehler → Redirect zu `/registrieren?email=…` (M3-Fix: bessere UX statt 409) |
| **DASH-11** | `DashboardServiceTest` | `ladeDashboardDaten` setzt `aktuellerMonat` und `aktuelleKw` direkt im DTO (M5-Fix: View-Logik nicht im Controller) |

### M1-Fix: Token-Generierung extrahiert

`TokenGenerator` (Util) wird von `VerifikationsService` und `EinladungsService` gemeinsam genutzt — keine Code-Duplikation mehr.

### M2-Fix: Cleanup für abgelaufene Einladungen

- **On-demand**: `EinladungsService.erstelleEinladung` löscht eine vorhandene abgelaufene Einladung bevor sie eine neue anlegt — UX-Fix für "Re-Einladung blockiert".
- **Background**: `EinladungsCleanupJob` läuft täglich (`@Scheduled`), entfernt alle Einladungen mit `gueltig_bis < now()` — DB-Hygiene.

### Phase 1.1 (Security & Registrierung)

#### Security (SEC)

| ID | Test-Klasse | Beschreibung |
|---|---|---|
| **SEC-01** | `SecurityConfigTest` | GET `/login` → 200, Login-Seite wird angezeigt |
| **SEC-02** | `SecurityConfigTest` | GET `/organisationen` ohne Login → 200 (public) |
| **SEC-03** | `SecurityConfigTest` | GET `/organisationen/neu` ohne Login → Redirect zu `/login` |
| **SEC-04** | `SecurityConfigTest` | POST `/logout` → Redirect zu `/` |
| **SEC-05** | `SecurityConfigTest` | Login mit gültigen Credentials → Redirect zu `/` |
| **SEC-06** | `SecurityConfigTest` | Login mit falschen Credentials → zurück zu `/login?error` |

#### Registrierung (REG)

| ID | Test-Klasse | Beschreibung |
|---|---|---|
| **REG-01** | `RegistrierungControllerTest` | GET `/registrieren` → 200 + Formular |
| **REG-02** | `RegistrierungControllerTest` | POST `/registrieren` mit gültigen Daten → User angelegt, Redirect zu `/login` |
| **REG-03** | `RegistrierungControllerTest` | POST `/registrieren` mit doppelter E-Mail → Fehler im Formular |
| **REG-04** | `RegistrierungControllerTest` | POST `/registrieren` mit leerem Passwort → Validierungsfehler |

### Phase 0.2.1 — Endpoint-Schutz via @PreAuthorize (K1-Fix)

#### AccessControl Slug-Varianten (AC)

| ID | Test-Klasse | Beschreibung |
|---|---|---|
| **AC-09** | `AccessControlTest` | `kannOrgEditierenNachSlug` delegiert: bekannter Slug → identisches Ergebnis wie `kannOrgEditieren(orgId)` |
| **AC-10** | `AccessControlTest` | `kannOrgEditierenNachSlug` mit unbekanntem Slug → false (kein Throw) |
| **AC-11** | `AccessControlTest` | `kannOrgVerwaltenNachSlug` delegiert analog für bekannten Slug |
| **AC-12** | `AccessControlTest` | `kannOrgVerwaltenNachSlug` mit unbekanntem Slug → false |

#### Organisation-Controller (ORG)

| ID | Test-Klasse | Beschreibung |
|---|---|---|
| **ORG-12** | `OrganisationControllerTest` | GET `/organisationen/{slug}/bearbeiten` ohne Edit-Recht → 403 |
| **ORG-13** | `OrganisationControllerTest` | GET `/organisationen/{slug}/bearbeiten` mit Edit-Recht → 200 |
| **ORG-14** | `OrganisationControllerTest` | POST `/organisationen/{slug}/loeschen` ohne Verwalten-Recht → 403 |
| **ORG-15** | `OrganisationControllerTest` | POST `/organisationen/{slug}/loeschen` mit Verwalten-Recht → 302 Redirect |

#### Mitglieder-Controller (MGCTRL)

| ID | Test-Klasse | Beschreibung |
|---|---|---|
| **MGCTRL-01** | `MitgliederControllerTest` | GET `/organisationen/{slug}/mitglieder` anonym → Redirect zu `/login` |
| **MGCTRL-02** | `MitgliederControllerTest` | GET `/organisationen/{slug}/mitglieder` ohne Edit-Recht → 403 |
| **MGCTRL-03** | `MitgliederControllerTest` | POST `.../hinzufuegen` ohne Verwalten-Recht → 403 |
| **MGCTRL-04** | `MitgliederControllerTest` | POST `.../{id}/entfernen` ohne Verwalten-Recht → 403 |
| **MGCTRL-05** | `MitgliederControllerTest` | POST `.../hinzufuegen` mit Verwalten-Recht → 302 Redirect |

#### Projekt-Controller (PCTRL)

| ID | Test-Klasse | Beschreibung |
|---|---|---|
| **PCTRL-06** | `ProjektControllerTest` | POST `/organisationen/{orgSlug}/projekte/speichern` ohne Edit-Recht → 403 |
| **PCTRL-07** | `ProjektControllerTest` | POST `.../{projektSlug}/veroeffentlichen` ohne Edit-Recht → 403 |
| **PCTRL-08** | `ProjektControllerTest` | POST `.../{projektSlug}/pakete/speichern` ohne Edit-Recht → 403 |

### Inbox / Nachrichten (MSG)

| ID | Test-Klasse | Beschreibung |
|---|---|---|
| **MSG-01** | `NachrichtServiceTest` | `sendeNachricht` erstellt Nachricht zu angenommener Anfrage |
| **MSG-02** | `NachrichtServiceTest` | `sendeNachricht` wirft IllegalStateException wenn Anfrage nicht ANGENOMMEN |
| **MSG-03** | `NachrichtServiceTest` | `sendeNachricht` wirft AccessDeniedException wenn User nicht zu beteiligter Org gehört |
| **MSG-04** | `NachrichtServiceTest` | `findeNachAnfrage` gibt Nachrichten chronologisch sortiert zurück |
| **MSG-05** | `NachrichtControllerTest` | GET `.../nachrichten` mit Edit-Recht → 200 + Thread-Ansicht |
| **MSG-06** | `NachrichtControllerTest` | GET `.../nachrichten` ohne Recht → 403 |
| **MSG-07** | `NachrichtControllerTest` | POST `.../nachrichten` mit gültigem Text → 302 Redirect |
| **MSG-08** | `NachrichtControllerTest` | POST `.../nachrichten` mit leerem Text → Validierungsfehler |
| **MSG-09** | `NachrichtViewTest` | View-Mapping korrekt (kein passwortHash, flacht Absender-Name ein) |

### Sponsor-Registrierung (SR)

| ID | Test-Klasse | Beschreibung |
|---|---|---|
| **SR-01** | `SponsorRegistrierungServiceTest` | `registriereSponsor` erstellt AppUser, Organisation (UNTERNEHMEN/PENDING), Mitgliedschaft (ORG_OWNER) |
| **SR-02** | `SponsorRegistrierungServiceTest` | Doppelte E-Mail → IllegalArgumentException (delegiert an AppUserService) |
| **SR-03** | `SponsorRegistrierungServiceTest` | Slug-Konflikt bei Firmenname → IllegalArgumentException |
| **SR-04** | `SponsorRegistrierungControllerTest` | GET `/sponsor/registrieren` → 200 + sponsor-registrieren Template |
| **SR-05** | `SponsorRegistrierungControllerTest` | POST `/sponsor/registrieren` valid → 302 Redirect `/login?registriert` |
| **SR-06** | `SponsorRegistrierungControllerTest` | POST `/sponsor/registrieren` mit Validierungsfehler → bleibt auf Formular |
| **SR-07** | `SponsorRegistrierungControllerTest` | POST `/sponsor/registrieren` doppelte E-Mail → Fehlermeldung auf Formular |

### Phase Wachstum — Postgres-Volltextsuche (VTS-PG)

| ID | Test-Klasse | Beschreibung |
|---|---|---|
| **VTS-PG-01** | `VolltextSucheServiceTest` | Leerer Suchbegriff → alle öffentlichen Projekte (kein JdbcTemplate-Call) |
| **VTS-PG-02** | `VolltextSucheServiceTest` | H2-DataSource-URL → Fallback auf `repository.sucheOeffentliche` (LIKE) |
| **VTS-PG-03** | `VolltextSucheServiceTest` | Postgres-DataSource-URL → JdbcTemplate native tsvector-Query |
| **VTS-PG-04** | `VolltextSucheServiceTest` | Postgres-Query-Fehler → Fallback auf LIKE-Suche |

### Phase Wachstum — Zahlungs-Integration (RECH, QRB)

| ID | Test-Klasse | Beschreibung |
|---|---|---|
| **RECH-01** | `RechnungServiceTest` | `erstelle` aus Vertrag kopiert IBAN + Sponsor + Betrag, Status OFFEN |
| **RECH-02** | `RechnungServiceTest` | `erstelle` bei vorhandener Rechnung wirft `IllegalStateException` |
| **RECH-03** | `RechnungServiceTest` | `erstelle` ohne IBAN auf der Org wirft mit klarer Message |
| **RECH-04** | `RechnungServiceTest` | QR-Referenz nur bei QR-IBAN (Institut-ID 30000-31999) generiert |
| **RECH-05** | `RechnungServiceTest` | `markiereBezahlt` setzt Status, Zeitstempel, User |
| **RECH-06** | `RechnungServiceTest` | `stornieren` bezahlter Rechnung wirft `IllegalStateException` |
| **QRB-01** | `QrBillServiceTest` | erzeuge liefert PNG mit Magic Bytes (≥ 1 KB) |
| **QRB-02** | `QrBillServiceTest` | `erzeugeAlsDataUrl` gibt `data:image/png;base64,…` zurück |
| **QRB-03** | `QrBillServiceTest` | Ohne IBAN wirft `IllegalArgumentException` |

### Phase Wachstum — Vertrags-Generator (VTR)

| ID | Test-Klasse | Beschreibung |
|---|---|---|
| **VTR-01** | `VertragServiceTest` | `erstelle` aus angenommener Anfrage kopiert Snapshot-Felder (Org, Sponsor, Paket, Preis) |
| **VTR-02** | `VertragServiceTest` | `erstelle` bei Status NEU wirft `IllegalStateException` |
| **VTR-03** | `VertragServiceTest` | `erstelle` bei vorhandenem Vertrag wirft `IllegalStateException` |
| **VTR-04** | `VertragServiceTest` | `erstelle` bei unbekannter Anfrage wirft `NotFoundException` |
| **VTR-05** | `VertragServiceTest` | `markiereUnterzeichnet` setzt Status, Zeitstempel, User |
| **VTR-06** | `VertragServiceTest` | `markiereUnterzeichnet` auf bereits unterzeichnetem Vertrag wirft |

### Phase Operational — DSG-Pflichtseiten (INFO)

| ID | Test-Klasse | Beschreibung |
|---|---|---|
| **INFO-01** | `InfoControllerTest` | GET `/impressum` → 200 + impressum-Template, public erreichbar |
| **INFO-02** | `InfoControllerTest` | GET `/datenschutz` → 200 + datenschutz-Template, public erreichbar |

### Phase Operational — Ops-Dashboard + Alerts (OPS)

| ID | Test-Klasse | Beschreibung |
|---|---|---|
| **OPS-01** | `RecentErrorsServiceTest` | Logback-Appender filtert auf ERROR-Level |
| **OPS-02** | `RecentErrorsServiceTest` | Buffer-Cap (50), neueste behalten |
| **OPS-03** | `RecentErrorsServiceTest` | `letzteErrors(limit)` begrenzt Rückgabe |
| **OPS-04** | `OpsAlertJobTest` | Heap unter Grenze → keine Mail |
| **OPS-05** | `OpsAlertJobTest` | Heap über Grenze → Mail an Test-Empfänger |
| **OPS-06** | `OpsAlertJobTest` | Wiederholter Alert innerhalb min-pause → unterdrückt |
| **OPS-07** | `OpsAlertJobTest` | Neue Errors über Schwelle → Alert mit Error-Vorschau |

### Phase Operational — Rate-Limiting (RATE)

| ID | Test-Klasse | Beschreibung |
|---|---|---|
| **RATE-01** | `RateLimitFilterTest` | Filter ignoriert nicht-konfigurierte Pfade (z.B. GET /marktplatz) |
| **RATE-02** | `RateLimitFilterTest` | POST `/registrieren` — erste N Requests gehen durch |
| **RATE-03** | `RateLimitFilterTest` | (N+1)-ter POST in 1 min vom selben IP → HTTP 429 + Retry-After |
| **RATE-04** | `RateLimitFilterTest` | Zwei verschiedene IPs haben getrennte Buckets |
| **RATE-05** | `RateLimitFilterTest` | Filter respektiert `X-Forwarded-For` (Caddy-Reverse-Proxy) |

### Phase Operational — Mail-Service zentral (MAIL)

| ID | Test-Klasse | Beschreibung |
|---|---|---|
| **BEN-01** | `BenachrichtigungsServiceTest` | `benachrichtigeUeberNeueAnfrage` ruft `MailService.sendePlain(empfaenger, subject, body)` |
| **BEN-02** | `BenachrichtigungsServiceTest` | Ohne Empfänger keine Mail (defensives Skip) |
| **BEN-03** | `BenachrichtigungsServiceTest` | `benachrichtigeUeberAntwort` ruft `MailService.sendePlain` |
| **EV-01..04** | `VerifikationsServiceTest` | Token-Lifecycle + Mail-Versand via `MailService.sendeHtml` |
| **EINL-12a/b** | `EinladungsMailListenerTest` | TransactionalEventListener via `MailService.sendeHtml`; Failure wird geschluckt |

### Phase 2 — Cloud-Storage + Backup (CLOUD-STO, BACKUP)

#### OciStorageService (CLOUD-STO)

| ID | Test-Klasse | Beschreibung |
|---|---|---|
| **CLOUD-STO-01** | `OciStorageServiceTest` | Konstruktor wirft `IllegalStateException` bei leerem Namespace |
| **CLOUD-STO-02** | `OciStorageServiceTest` | `speichere` ruft `putObject` mit Namespace, Bucket, Key, ContentLength korrekt |
| **CLOUD-STO-03** | `OciStorageServiceTest` | `BmcException` aus SDK wird in `RuntimeException` mit Status-Code gewrappt |
| **CLOUD-STO-04** | `OciStorageServiceTest` | `loesche` idempotent — 404-Antwort vom Bucket wird geschluckt |
| **CLOUD-STO-05** | `OciStorageServiceTest` | `ladeAlsResource` liefert lesbare Resource mit dem Object-Inhalt |
| **CLOUD-STO-06** | `OciStorageServiceTest` | `getObject` 404 → `RuntimeException("nicht gefunden")` |

#### BackupService Cloud-Upload (BACKUP)

| ID | Test-Klasse | Beschreibung |
|---|---|---|
| **BACKUP-01** | `BackupServiceTest` | H2-Backup erstellt Datei im konfigurierten Verzeichnis |
| **BACKUP-02** | `BackupServiceTest` | `listeBackups` gibt leere Liste für leeres Verzeichnis |
| **BACKUP-03** | `BackupServiceTest` | `listeBackups` findet vorhandene Backup-Files |
| **BACKUP-04** | `BackupServiceTest` | Backup wird in Cloud hochgeladen, wenn `BackupCloudUploader` registriert |
| **BACKUP-05** | `BackupServiceTest` | Cloud-Upload-Fehler bricht Backup nicht ab — lokales File bleibt |
| **BACKUP-06** | `BackupServiceTest` | Ohne `BackupCloudUploader` wird kein Cloud-Call gemacht |
| **BACKUP-07** | `BackupServiceTest` | `leseBackup` gibt Inhalt zurück, lehnt Path-Traversal + ungültige Namen ab |
| **BACKUP-08** | `BackupServiceTest` | `loescheBackup` entfernt validierte Datei |
| **RESTORE-01** | `BackupRestoreServiceTest` | Leerer/null Input wird abgelehnt |
| **RESTORE-02** | `BackupRestoreServiceTest` | H2-Restore ruft `RUNSCRIPT FROM` + Audit-Eintrag |
| **RESTORE-03** | `BackupRestoreServiceTest` | SQL-Failure beim RUNSCRIPT als RuntimeException propagiert |

### Security-Härtung — Dev-Seed-Properties (DEV-SEED)

| ID | Test-Klasse | Beschreibung |
|---|---|---|
| **DEV-SEED-01** | `DevSeedRunnerTest` | Property-Override für E-Mail + Passwort (`sponsorplatz.dev.email/passwort`) wird angewandt |
| **DEV-SEED-02** | `DevSeedRunnerTest` | Idempotent — bereits existierender User wird nicht überschrieben |
| **DEV-SEED-03** | `DevSeedRunnerTest` | Default-Passwort `dev` wird verwendet, wenn keine Property gesetzt |

## CI

- Bei jedem Push und PR auf `main`: `mvn -B clean verify` + Docker-Build-Smoke
- JaCoCo-Coverage als Artifact pro Run
- Surefire-Reports als Artifact pro Run

## Smoke-Test-Checkliste (manuell vor Release)

- [ ] App startet (`mvn spring-boot:run`)
- [ ] http://localhost:8080 → Startseite lädt
- [ ] http://localhost:8080/actuator/health → `{"status":"UP"}`
- [ ] http://localhost:8080/h2-console → erreichbar (dev only)
- [ ] http://localhost:8080/organisationen → leere Liste
- [ ] Neue Org anlegen via UI → erscheint in Liste, Detail-Page lädt
- [ ] `mvn test` — alle grün
- [ ] `docker compose up --build app` → läuft im Container
