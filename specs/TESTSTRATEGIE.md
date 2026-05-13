# Teststrategie

## Test-Ebenen

| Ebene | Werkzeug | Zweck |
|---|---|---|
| Unit | JUnit 5 + Mockito + AssertJ | Service-Logik, Konverter, Validierung |
| Repository | `@DataJpaTest` (H2) | JPA-Queries, Constraints |
| Web | `@WebMvcTest` + MockMvc | HTTP-Routen, Redirects |
| Security | `@SpringBootTest` mit Profilen | Form-Login, AccessControl, OIDC |
| Container | Testcontainers (PostgreSQL) | echte DB für Migrations-Tests |
| **E2E** | **Cucumber + Playwright + Testcontainers** | **Browser-getriebene Cross-Page-Flows (Login → Onboarding → Anfrage → Vertrag)** |
| Smoke | manuell + CI Healthcheck | Startup, Browser-Happy-Path |

### E2E-Suite (Pilot)

Die E2E-Suite läuft **nicht** in `mvn test` — sondern via Failsafe-Profil:

```bash
mvn verify -P e2e            # alle E2E-Szenarien (benötigt Docker für Postgres-Testcontainer)
mvn verify -P e2e -De2e.headless=false   # Browser sichtbar (Debug)
mvn verify -P e2e -De2e.browser=firefox  # alternative Browser-Engine
```

Surefire excludet das Package `src/test/java/ch/sponsorplatz/e2e/**` im
Default-Lauf, damit `mvn test` schnell + Docker-frei bleibt.

**Konventionen für E2E-Tests:**

- Feature-Dateien (`.feature`) unter `src/test/resources/features/`, Sprache `de`
  (Gherkin-Keywords `Funktionalität`, `Szenario`, `Angenommen`, `Wenn`, `Und`, `Dann`).
- Step-Definitions im Package `ch.sponsorplatz.e2e`, eine Klasse pro Flow.
- Test-Daten via `E2EFixtures` seeden (z.B. CSS-Sponsor); zwischen Szenarien
  TRUNCATE der mutierbaren Tabellen — Email-Unique-Constraints kollidieren
  sonst bei wiederholten Läufen.
- Bei fehlgeschlagenen Szenarien schreibt {@link E2EHooks} Screenshot + HTML
  ins `target/`-Verzeichnis (Post-Mortem-Debug).
- Pre-Filled Test-Profil `application-e2e.properties`: Mail off, Storage
  lokal, kein OCI — alles, was Test-Reibung erzeugt, wird ausgehängt.

| ID | Feature | Beschreibung |
|---|---|---|
| **E2E-01** | `sponsor-anfrage-zu-vertrag.feature` | Verein registriert → Verein anlegen → Projekt + Paket → CSS-Sponsor stellt Anfrage → Verein nimmt an → Vertrag entsteht in DB |

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

> **End-to-End-Spec:** [`SPONSORING_ZAHLUNGSFLUSS.md`](SPONSORING_ZAHLUNGSFLUSS.md)
> — Lifecycle, Statusmaschinen, Swiss-QR-Bill-Compliance, Nummerierung, MwSt,
> Mahnwesen, Storno, DSG-Permissions, Audit-Log-Pflicht-Events.

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

**Aus SPONSORING_ZAHLUNGSFLUSS.md (TBD):**

| ID | Test-Klasse | Beschreibung |
|---|---|---|
| **RECH-07** | `RechnungsnummerGeneratorTest` | Format `R-YYYY-NNNNN`, 5-stellige LfdNr pro Org-Jahr fortlaufend |
| **RECH-07b** | `RechnungsnummerGeneratorTest` | Bei vorhandenen Rechnungen zählt der Generator um 1 hoch |
| **RECH-08** | `RechnungsnummerGeneratorTest` | Jahres-Rollover startet bei 1, kein Reset bei laufendem Jahr |
| **RECH-09** | `RechnungsnummerGeneratorTest` | Lückenlosigkeit — stornierte Rechnung behält Nummer, nächste = max+1 |
| **RECH-10** *(Backlog)* | `IbanValidatorTest` | Mod-97-Prüfsumme — heute via qrbill-generator-Lib (`Payments.isQRIBAN`) abgedeckt |
| **RECH-11** *(Backlog)* | `IbanValidatorTest` | `istQrIban` true für 30000-31999, false sonst |
| **RECH-12** *(Backlog)* | `QrReferenzGeneratorTest` | 27-stellige Referenz mit Mod-10-Prüfziffer — heute via `Payments.createQRReference` |
| **RECH-13** *(TBD)* | `RechnungControllerTest` | GET `/rechnungen/{id}` ohne ORG_VIEWER-Recht → 403 |
| **RECH-14** *(TBD)* | `RechnungControllerTest` | GET `/rechnungen/{id}/pdf` mit Sponsor-Org-Mitgliedschaft → 200, eigene Rechnung |
| **RECH-15** | `RechnungServiceTest` | `markiereBezahlt` schreibt `RECHNUNG_BEZAHLT` ins Audit-Log (Quelle MANUELL/WEBHOOK) |
| **RECH-16** | `RechnungServiceTest` | `stornieren` schreibt `RECHNUNG_STORNIERT` ins Audit-Log mit Grund + vorherigem Status |
| **MAHN-01** | `MahnungsCronJobTest` | 7 Tage vor Fälligkeit → Reminder versendet, `mahnstufe=0` bleibt |
| **MAHN-02** | `MahnungsCronJobTest` | 7 Tage nach Fälligkeit → 1. Mahnung, `mahnstufe=1` gesetzt |
| **MAHN-03** | `MahnungsCronJobTest` | Mehrfacher Lauf am gleichen Tag versendet nur eine Mahnung (Idempotenz) |
| **MAHN-04** | `MahnungsCronJobTest` | BEZAHLT-Rechnung wird nie gemahnt |

### Phase Wachstum — Vertrags-Generator (VTR)

> **End-to-End-Spec:** [`SPONSORING_ZAHLUNGSFLUSS.md`](SPONSORING_ZAHLUNGSFLUSS.md)

| ID | Test-Klasse | Beschreibung |
|---|---|---|
| **VTR-01** | `VertragServiceTest` | `erstelle` aus angenommener Anfrage kopiert Snapshot-Felder (Org, Sponsor, Paket, Preis) |
| **VTR-02** | `VertragServiceTest` | `erstelle` bei Status NEU wirft `IllegalStateException` |
| **VTR-03** | `VertragServiceTest` | `erstelle` bei vorhandenem Vertrag wirft `IllegalStateException` |
| **VTR-04** | `VertragServiceTest` | `erstelle` bei unbekannter Anfrage wirft `NotFoundException` |
| **VTR-05** | `VertragServiceTest` | `markiereUnterzeichnet` setzt Status, Zeitstempel, User |
| **VTR-06** | `VertragServiceTest` | `markiereUnterzeichnet` auf bereits unterzeichnetem Vertrag wirft |
| **VTR-07** *(TBD)* | `VertragServiceTest` | `kuendige` mit bezahlter Rechnung wirft `IllegalStateException` |
| **VTR-08** *(TBD)* | `VertragServiceTest` | `kuendige` mit offener Rechnung erlaubt — Rechnung wird mit storniert |
| **VTR-09** | `VertragServiceTest` | `erstelle` bei Kontakt-Anfrage (paket=null): Verein-Org wird via `OrgTyp.VEREIN`-Check als `v.org` gemappt, Sponsor als `v.sponsorOrg` — unabhängig von Anfrage-Richtung. Siehe [`KONTAKT_ANFRAGE_VERTRAG.md`](KONTAKT_ANFRAGE_VERTRAG.md) |
| **VTR-10** | `VertragServiceTest` | `erstelle` bei Kontakt-Anfrage ohne Wunsch-Betrag: `betreff` wird zu `paketName`, `nachricht` zu `paketBeschreibung`, `preisChf = 0` |
| **VTR-10b** | `VertragServiceTest` | `erstelle` bei Kontakt-Anfrage mit `wunschBetragChf=5000`: Vertrag startet mit `preisChf=5000` (Initial-Preis aus Anfrage-Wunsch). Siehe V33 + [`KONTAKT_ANFRAGE_VERTRAG.md`](KONTAKT_ANFRAGE_VERTRAG.md) |
| **ANF-08** | `SponsoringAnfrageServiceTest` | `erstelleKontaktAnfrage` mit negativem Wunsch-Betrag wirft `IllegalArgumentException` (Defense-in-Depth zum DB-CHECK in V33) |

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

### Phase Operational — Feature-Backlog (BL)

| ID | Test-Klasse | Beschreibung |
|---|---|---|
| **BL-01** | `BacklogServiceTest` | `erstelle` setzt Status OFFEN, Priorität, erstelltVon |
| **BL-02** | `BacklogServiceTest` | `findeAlleSortiert` — offen vor erledigt, HOCH vor MITTEL vor NIEDRIG |
| **BL-03** | `BacklogServiceTest` | `aendereStatus` setzt erledigtAm bei ERLEDIGT, nullt bei OFFEN |
| **BL-04** | `BacklogServiceTest` | `aendereStatus` auf unbekannte ID wirft `NotFoundException` |
| **BL-05** | `BacklogServiceTest` | `zaehleOffen` summiert OFFEN + IN_ARBEIT |
| **BL-06** | `BacklogServiceTest` | `loesche` bei unbekannter ID wirft `NotFoundException` |

### Security-Härtung — Dev-Seed-Properties (DEV-SEED)

| ID | Test-Klasse | Beschreibung |
|---|---|---|
| **DEV-SEED-01** | `DevSeedRunnerTest` | Property-Override für E-Mail + Passwort (`sponsorplatz.dev.email/passwort`) wird angewandt |
| **DEV-SEED-02** | `DevSeedRunnerTest` | Idempotent — bereits existierender User wird nicht überschrieben |
| **DEV-SEED-03** | `DevSeedRunnerTest` | Default-Passwort `dev` wird verwendet, wenn keine Property gesetzt |

### Phase 7.1 — Marktplatz-Branche-Filter (MKT)

> **Ziel:** Sponsorplatz Health-Fokus (V12) wird im Marktplatz erlebbar. Versicherte
> und Marken filtern Projekte nach Health-Branche; das Frontend bietet alle elf
> `Branche`-Werte als Chip-Cloud zur Multi-Selection an.
>
> **Controller-Vertrag:**
> - `@RequestParam(required = false) Set<Branche> branche` — Spring konvertiert via
>   `Branche.valueOf(...)`. Default = `null` (keine Filterung, alle Projekte).
> - Filter-Logik: leerer/`null`-Wert lässt Liste unverändert; sonst werden
>   Projekte behalten, deren `org.branche` im Set ist.
> - Model-Attribute:
>   - `alleBranchen` — `Branche.values()` (für Chip-Cloud-Render in `marktplatz.html`)
>   - `filterBranchen` — aktuelle Auswahl (leeres Set, wenn nicht gesetzt) für aktive Chips
>
> **Tests:**

| ID | Test-Klasse | Beschreibung |
|---|---|---|
| **MKT-08** | `MarktplatzControllerTest` | `?branche=SPORT` reduziert die Liste auf Projekte, deren `org.branche == SPORT` ist |
| **MKT-09** | `MarktplatzControllerTest` | Ohne `branche`-Param → Liste unverändert; Model exposed `alleBranchen` (alle elf Werte) und `filterBranchen` (leeres Set) für die Chip-Cloud |
| **MKT-10** | `MarktplatzControllerTest` | Multi-Select `?branche=SPORT&branche=REHA` — beide Branchen aktiv, Projekte beider Branchen erscheinen, `filterBranchen`-Model enthält `{SPORT, REHA}` für aktive Chip-Anzeige |

### Phase 7.2 — Vereins-Profil Health-Hero (VP)

| ID | Test-Klasse | Beschreibung |
|---|---|---|
| **VP-03** | `VereinProfilControllerTest` | Branche-Chip wird als Teil der `OrganisationView` im Model bereitgestellt |
| **VP-04** | `VereinProfilControllerTest` | Branche-Beschreibung ist als Subhead-Text verfügbar (enthält Keyword) |

### Phase 7.3 — Marken-Landing-Page (MARK)

| ID | Test-Klasse | Beschreibung |
|---|---|---|
| **MARK-01** | `MarkenLandingControllerTest` | Marken-Landing `/fuer-marken` rendert ohne Login (200 + View `marken-landing`) |
| **MARK-02** | `MarkenLandingControllerTest` | Model enthält `vereineProBranche` und `anzahlProjekte` mit korrekten Werten |
| **MARK-03** | `MarkenLandingControllerTest` | Response enthält CTA-Link zu `/sponsor/registrieren` |
| **MARK-02a** | `StatistikServiceTest` | `vereineProBranche` zählt korrekt pro Branche (nur VERIFIED/ACTIVE) |
| **MARK-02b** | `StatistikServiceTest` | `anzahlAktiveProjekte` gibt Gesamtzahl öffentlicher Projekte zurück |
| **MARK-02c** | `StatistikServiceTest` | PENDING-Orgs werden in `vereineProBranche` nicht mitgezählt |

**Spätere Tests (Phase 7.1 fortgesetzt):**
- **MKT-11** (TBD): Ungültiger Branche-Wert in URL → Spring `MethodArgumentTypeMismatchException` → 400 via `GlobalExceptionHandler`
- **MKT-12** (TBD): Branche-Filter kombiniert mit `kategorie`/`ort`/`q` (Kompositions-Test, AND-Verknüpfung)

### Phase 7.3 — Marken-Landing-Page (MARK)

> **Ziel:** Öffentliche B2B-Landing für Health-Marken (Krankenkassen, Apotheken,
> Lebensmittel, Fitness, Stiftungen). Spiegelt die Vereins-Erzählung mit
> Marken-Use-Cases und Trust-Indikatoren ("kuratiert", "lokal", "messbar").
>
> **Controller-Vertrag (`MarkenLandingController`):**
> - Route: `GET /fuer-marken` (permitAll in dev- und prod-Profil)
> - View: `marken-landing`
> - Model-Attribute:
>   - `vereineProBranche` — `Map<Branche, Long>` aus `StatistikService.vereineProBranche()`
>   - `anzahlProjekte` — `long` aus `StatistikService.anzahlAktiveProjekte()`
>   - `alleBranchen` — `Branche.values()` für Branche-Übersicht im Template
>   - `aktiveSeite` — `"fuer-marken"` (für Navigations-Hervorhebung)
>
> **Service-Vertrag (`StatistikService`, Paket `organisation`):**
> - `vereineProBranche()` — zählt VEREIN-Orgs mit Status `VERIFIED` oder `ACTIVE`
>   gruppiert nach `branche`. Liest aktuell alle Orgs in den Speicher und
>   filtert in Java; Refactoring auf Aggregat-Query siehe MARK-04.
> - `anzahlAktiveProjekte()` — Anzahl Projekte mit `Sichtbarkeit.OEFFENTLICH`.
> - **Backlog:** `@Cacheable` mit TTL 5 Minuten (Marketing-Traffic), DB-seitige
>   Aggregat-Queries für Skalierbarkeit, optional zentrales `StatistikDaten`-DTO.

| ID | Test-Klasse | Beschreibung |
|---|---|---|
| **MARK-01** | `MarkenLandingControllerTest` | `GET /fuer-marken` → 200, View `marken-landing`, Model enthält `vereineProBranche` und `anzahlProjekte` |
| **MARK-02** | `MarkenLandingControllerTest` | Model-Werte stammen exakt aus `StatistikService` (gemockt: `vereineProBranche()` und `anzahlAktiveProjekte()` mit konkreten Werten) |
| **MARK-03** | `MarkenLandingControllerTest` | Gerendertes HTML enthält den CTA-Link `/sponsor/registrieren` (Conversion-Pfad zur Sponsor-Org-Anmeldung) |

**Phase 7.3 Performance-Iteration — Aggregat-Queries + Cache:**

| ID | Test-Klasse | Beschreibung |
|---|---|---|
| **MARK-04** | `OrganisationStatistikIT` | `@DataJpaTest`: `OrganisationRepository.zaehleVereineNachBranche(stati)` filtert auf `typ = VEREIN` + Status in `(VERIFIED, ACTIVE)` + `branche IS NOT NULL` und gruppiert korrekt; UNTERNEHMEN/STIFTUNG werden ausgeschlossen |
| **MARK-05** | `OrganisationStatistikIT` | `@DataJpaTest`: `ProjektRepository.countBySichtbarkeit(OEFFENTLICH)` ignoriert ENTWURF und ARCHIVIERT |
| **MARK-06** | `MarkenLandingControllerTest` | Render-Assertion: HTML enthält Trust-Indikatoren `Kuratiert`, `Lokal`, `Messbar` (regression-safe gegen Template-Kürzungen) |
| **MARK-07a** | `StatistikServiceCacheIT` | `vereineProBranche` mit `@Cacheable` — zweiter Aufruf trifft Repository nicht (Verify-Counter `times(1)`) |
| **MARK-07b** | `StatistikServiceCacheIT` | `anzahlAktiveProjekte` mit `@Cacheable` — analog Verify-Counter |
| **MARK-07c** | `StatistikServiceCacheIT` | Cache-Regions `statistik-vereineProBranche` und `statistik-anzahlProjekte` sind explizit deklariert (fail-fast bei Tippfehler im `@Cacheable("...")`) |

### Phase 8.1 — Demo-Seed (SEED)

| ID | Test-Klasse | Beschreibung |
|---|---|---|
| **SEED-01** | `DemoSeedRunnerTest` | DemoSeed erstellt Vereine, Projekte und Anfragen mit konsistenten FKs |
| **SEED-01b** | `DemoSeedRunnerTest` | Idempotent — überspringt wenn Org-Slug bereits existiert |
| **SEED-02** | `DemoModusAdviceTest` | Demo-Disclaimer rendert bei `sponsorplatz.demo-modus=true` |

### Phase 8.2 — Engagement-Schaufenster (ENG)

| ID | Test-Klasse | Beschreibung |
|---|---|---|
| **ENG-01** | `EngagementServiceTest` | `findeNachSponsorSlug` liefert nur ANGENOMMEN-Anfragen |
| **ENG-02** | `EngagementServiceTest` | `findeNachSponsorSlug` mit unbekanntem Slug → NotFoundException |
| **ENG-03** | `EngagementServiceTest` | `findeNachSponsorSlugUndRegion` filtert nach Region |
| **ENG-01-CTRL** | `EngagementControllerTest` | GET `/marken/{slug}/engagements` → 200 + engagement-schaufenster View |
| **ENG-02-CTRL** | `EngagementControllerTest` | Region-Filter wird an Service delegiert |
| **ENG-03-CTRL** | `EngagementControllerTest` | Branche-Filter wird an Service delegiert |

### Phase 8.3 — OG-Card-Generator (OG) — Backlog

| ID | Test-Klasse | Beschreibung |
|---|---|---|
| **OG-01** | `OgImageControllerTest` | GET `/og/verein/{slug}.png` → 200 + content-type image/png |
| **OG-02** | `OgImageControllerTest` | GET `/og/projekt/{slug}.png` → 200 + content-type image/png |
| **OG-03** | `OgImageControllerTest` | Response enthält `Cache-Control: max-age=3600` |

### Phase 9.1 — Mehrsprachigkeit FR/IT (I18N)

| ID | Test-Klasse | Beschreibung |
|---|---|---|
| **I18N-01** | `LocaleConfigTest` | Cookie-basierter `LocaleResolver` — Sprache wird in Cookie `lang` persistiert; nächster Request liest korrekte Locale |
| **I18N-02** | `LocaleConfigTest` | `?lang=fr` URL-Override setzt Locale auf `fr_CH`, Cookie wird aktualisiert |
| **I18N-03** | `LocaleConfigTest` | `?lang=de` wechselt zurück zu `de_CH` |
| **I18N-04** | `LocaleConfigTest` | Ungültiger `?lang=xx` → Fallback auf Default `de_CH` |
| **I18N-05** | `BrancheI18nTest` | `Branche.SPORT` Anzeige-Name FR = `Sport` (aus `messages_fr_CH.properties`) |
| **I18N-06** | `BrancheI18nTest` | `Branche.MENTAL_HEALTH` Anzeige-Name IT = `Salute mentale` (aus `messages_it_CH.properties`) |

### Phase 9.2 — Zahlungs-Provider-Anbindung (PAY)

| ID | Test-Klasse | Beschreibung |
|---|---|---|
| **PAY-01** | `LokalerStubProviderTest` | `erstelleZahlung` gibt sofort Status BEZAHLT zurück |
| **PAY-02** | `LokalerStubProviderTest` | `bestaetigeZahlung` ist idempotent |
| **PAY-03** | `PaymentWebhookControllerTest` | POST `/payment/webhook/stub` → 200 + Rechnung markiert als bezahlt |
| **PAY-04** | `PaymentWebhookControllerTest` | POST `/payment/webhook/stub` mit unbekannter Referenz → 404 |
| **PAY-05** | `PaymentWebhookControllerTest` | Doppelter Webhook (Idempotenz) → 200, keine doppelte Buchung |
| **PAY-06** | `PaymentServiceTest` | `erstelleZahlung` delegiert korrekt an aktiven Provider |

### Phase 9.3 — Event-Entity (EVT)

| ID | Test-Klasse | Beschreibung |
|---|---|---|
| **EVT-01** | `EventServiceTest` | CRUD: `erstelle`, `aktualisiere`, `loesche` — Basis-Lifecycle |
| **EVT-02** | `EventControllerTest` | POST `…/events/speichern` ohne Edit-Recht → 403 |
| **EVT-03** | `DashboardServiceTest` | `naechsteEvents` gibt max. 3 kommende Events der eigenen Orgs zurück |
| **EVT-04** | `EventServiceTest` | `findeKommendeNachOrgIds` sortiert nach Datum aufsteigend |
| **EVT-05** | `EventViewTest` | View-Mapping korrekt (kein Org-Entity im View) |

### Phase 1.4 — SSO via OIDC / Entra ID (SSO)

> **Ziel:** Single-Sign-On für Corporate-User via Microsoft Entra ID (vormals
> Azure AD). Coexistence mit Form-Login. Vollständige Spec siehe
> [`AUTH_SSO_OIDC.md`](AUTH_SSO_OIDC.md).
>
> **Service-Vertrag (`SponsorplatzOidcUserService`):**
> - `loadUser(OidcUserRequest)` mappt OIDC-Subject auf `AppUser` mit drei Stufen:
>   1. Lookup `(provider=ENTRA_ID, subject)` in `federierte_identitaet`
>   2. Email-Match auf bestehenden `AppUser` → Auto-Verknüpfung
>   3. Just-in-Time-Provisionierung neuer `AppUser` mit `email_verifiziert=true`
> - Group-Mapping: `groups`-Claim → `PlatformRolle` per Properties-Konfiguration
>
> **Datenmodell:** Neue Tabelle `federierte_identitaet` (Migration V25) mit
> `(provider, subject)` UNIQUE, FK auf `app_user(id)` mit ON DELETE CASCADE.

| ID | Test-Klasse | Beschreibung |
|---|---|---|
| **SSO-01** | _Pilot-Smoke-Test_ | Authorization-Code-Flow End-to-End. Bewusst NICHT als IT verdrahtet (verlangt Wiremock/Testcontainers-Keycloak, übermächtig für Pilot-Phase). Mapping-Logik ist via SSO-02..07 voll abgedeckt; Token-Verifikation ist Spring-Security-upstream-getestet. Manuell gegen Entra-Test-Tenant in der Pilot-Phase verifizieren. |
| **SSO-02** | `SponsorplatzOidcUserServiceTest` | Bestehender User mit gleicher E-Mail wird via Email-Match verknüpft (Eintrag in `federierte_identitaet`) ✅ |
| **SSO-03** | `SponsorplatzOidcUserServiceTest` | Neuer User wird Just-in-Time erstellt mit `email_verifiziert=true` und `passwort_hash=OIDC-ONLY` ✅ |
| **SSO-04** | `SponsorplatzOidcUserServiceTest` | Subsequent Login findet User direkt via `(provider, subject)`, aktualisiert `letzter_login_am` ✅ |
| **SSO-05** | `OidcLoginPageRenderTest` | Beide Login-Pfade aktiv: Form-Login + OAuth2-Anbieter-Button auf `/login`-Page sichtbar ✅ |
| **SSO-06** | `SponsorplatzOidcUserServiceTest` | Group-Mapping setzt `PLATFORM_ADMIN`, wenn `groups`-Claim die konfigurierte Group enthält ✅ |
| **SSO-07** | `SponsorplatzOidcUserServiceTest` | Group-Mapping entzieht `PLATFORM_ADMIN`, wenn die Group nicht mehr im Claim ist (Re-Sync bei jedem Login) ✅ |
| **SSO-08** | _Spring-Security-upstream_ | Ungültige ID-Token-Signatur → 401. Spring Security validiert JWKS automatisch — keine eigene Logik, daher delegiert auf upstream-Tests. |
| **SSO-09** | _Spring-Security-upstream_ | Abgelaufenes ID-Token → 401. Analog SSO-08. |
| **SSO-10** | `LogoutControllerTest` | Logout entfernt Spring-Session lokal (Provider-Logout out of scope für initial) — TBD |
| **SSO-11** | `FederierteIdentitaetRepositoryTest` | `findByProviderAndSubject` findet bei Treffer, gibt `Optional.empty()` bei Miss zurück ✅ |
| **SSO-12** | `FederierteIdentitaetRepositoryTest` | UNIQUE-Constraint auf `(provider, subject)` — zweiter Eintrag wirft `ConstraintViolationException` ✅ |

**Spätere Tests (Phase 1.4 fortgesetzt):**
- **SSO-13** (TBD): Single Logout — Provider-initiated `end_session_endpoint`-Aufruf entfernt Session lokal und beim Provider
- **SSO-14** (TBD): User-UI für manuelle Verknüpfung/Trennung der Entra-Identität in `/einstellungen`
- **SSO-15** (TBD): Multi-Provider — Erweiterung um Google/Apple, Provider-Auswahl auf `/login`

### Persönliche Anfragen-Übersicht + Anfrage-Erstellung (MANF)

> Tests für `MeineAnfragenController` — sowohl die User-zentrierte
> Anfragen-Übersicht (`/anfragen`) als auch der Erstellungs-Flow vom
> Marktplatz aus (`/anfragen/neu` + `/anfragen/erstellen`).

| ID | Test-Klasse | Beschreibung |
|---|---|---|
| **MANF-01** | `MeineAnfragenControllerTest` | `/anfragen` ohne Auth → Redirect auf Login |
| **MANF-02** | `MeineAnfragenControllerTest` | `/anfragen` mit Auth → 200 + Template `meine-anfragen` |
| **MANF-03** | `MeineAnfragenControllerTest` | `/anfragen` zeigt offene Zählung im Modell |
| **MANF-04** | `MeineAnfragenControllerTest` | `GET /anfragen/neu?paketId=…` zeigt Form mit Paket-Kontext und User-Orgs (Edit-Recht) |
| **MANF-05** | `MeineAnfragenControllerTest` | `POST /anfragen/erstellen` Happy Path → ruft `service.erstelle` mit Empfänger (vom Paket abgeleitet, kein Client-Trust) und redirected auf `/anfragen` |
| **MANF-06** | `MeineAnfragenControllerTest` | `POST /anfragen/erstellen` mit `anfragenderOrgId == empfaengerOrg.id` → Form mit Binding-Error, kein Service-Call (Self-Anfrage-Schutz) |
| **MANF-07** | `MeineAnfragenControllerTest` | `POST /anfragen/{id}/annehmen` ohne Edit-Recht auf Empfänger-Org → 403 (IDOR-Schutz; analoge Deckung für `/ablehnen`) |

### Onboarding-Wizard (ONB) — Phase 11.1

| ID | Test-Klasse | Beschreibung |
|---|---|---|
| **ONB-01** | `OnboardingControllerTest` | `/onboarding` ohne Auth → Redirect auf Login |
| **ONB-02** | `OnboardingControllerTest` | `/onboarding` ohne Mitgliedschaften → zeigt Onboarding-Seite |
| **ONB-03** | `OnboardingControllerTest` | `/onboarding` mit Mitgliedschaft → Redirect auf Dashboard (Re-Entry-Schutz) |
| **ONB-04** | `OnboardingControllerTest` | `POST /onboarding/verein-erstellen` ruft `OrganisationService.erstelleMitEigentuemer` und redirected auf Dashboard |
| **ONB-05** | `OnboardingControllerTest` | `POST /onboarding/verein-erstellen` ohne Name → Validierungs-Fehler |

### Support-Formular (SUP) — Phase 11.2

| ID | Test-Klasse | Beschreibung |
|---|---|---|
| **SUP-01** | `SupportControllerTest` | `/support` ohne Auth → Redirect auf Login |
| **SUP-02** | `SupportControllerTest` | `/support` mit Auth → 200 + Form |
| **SUP-03** | `SupportControllerTest` | `POST /support` ruft `MailService.sendePlain` und redirected mit Erfolgs-Meldung |
| **SUP-04** | `SupportControllerTest` | `POST /support` mit leerem Betreff → Validierungs-Fehler, Form bleibt offen |

### Datei-Anhänge — MedienAssetView (Phase 11.3)

| ID | Test-Klasse | Beschreibung |
|---|---|---|
| **VIEW-10** | `MedienAssetViewTest` | `MedienAssetView.von` mappt alle Felder inkl. `groesseBytes` |
| **VIEW-11** | `MedienAssetViewTest` | `istBild()` → true für Bilder, false für Dokumente |
| **VIEW-12** | `MedienAssetViewTest` | `groesseFormatiert()` zeigt B/KB/MB korrekt |
| **VIEW-12b** | `MedienAssetViewTest` | `endung()` extrahiert Datei-Endung lowercase, leer wenn kein Punkt im Namen |
| **VIEW-13** | `AnfrageViewTest` | `vereinSlug()` liefert Anfragender-Slug bei Kontakt-Anfrage (Verein → Sponsor), Empfänger-Slug bei Paket-Anfrage. Siehe [`KONTAKT_ANFRAGE_VERTRAG.md`](KONTAKT_ANFRAGE_VERTRAG.md) |

### Monitoring & Observability (MON) — Phase 10.1

| ID | Test-Klasse | Beschreibung |
|---|---|---|
| **MON-01** | `MonitoringTest` | `GET /actuator/health` → 200 mit `status: UP` |
| **MON-01b** | `MonitoringTest` | `GET /actuator/health/liveness` → 200 (Kubernetes Liveness-Probe) |
| **MON-01c** | `MonitoringTest` | `GET /actuator/health/readiness` → 200 (Kubernetes Readiness-Probe) |
| **MON-02** | `MonitoringTest` | `GET /actuator/prometheus` → 200, enthält JVM-Metriken im Prometheus-Format |
| **MON-03** | `MonitoringTest` | Response enthält `X-Trace-ID`-Header (generiert wenn nicht vorhanden) |
| **MON-03b** | `MonitoringTest` | Vorhandener `X-Trace-ID`-Request-Header wird übernommen (Load-Balancer-Propagation) |
| **MON-03c** | `MonitoringTest` | Sonderzeichen in `X-Trace-ID` (Quote, Space, …) werden verworfen → UUID-Fallback (Log-Injection-Schutz). CR/LF fängt Tomcat selbst mit 400 ab. |
| **MON-03d** | `MonitoringTest` | Overlong `X-Trace-ID` (>64 Zeichen) wird verworfen — kein unbegrenzter MDC-Memory-Footprint |
| **MON-04** | `MonitoringTest` | MDC wird nach jedem Request aufgeräumt — keine Trace-ID-Leaks im Spring-Thread-Pool |

**Architektur-Hinweise zum Monitoring-Setup:**

- **Management-Port-Split in prod**: Actuator-Endpoints laufen auf `${MANAGEMENT_SERVER_PORT:9090}` mit Loopback-Bind
  (`${MANAGEMENT_SERVER_ADDRESS:127.0.0.1}`). Damit ist `/actuator/prometheus` für externe Scraper nicht direkt erreichbar
  — Operations setzt einen SSH-Tunnel, Sidecar oder Same-Host-Prometheus-Container. K8s-Probes hitten Port 9090 direkt
  am Pod; Docker-Healthcheck probiert 9090 → 8080 als Fallback.
- **Public-Endpoints am Application-Port** (8080): nur `/actuator/health`, `/actuator/health/liveness`,
  `/actuator/health/readiness`, `/actuator/info` — explizite Allowlist statt `/actuator/**`-Wildcard, damit
  `diskSpace`/`db`/`ping`-Sub-Indikatoren nicht versehentlich freigeschaltet werden.
- **MON-W3C (Roadmap)**: Migration von proprietärem `X-Trace-ID` zu W3C-`traceparent` (OpenTelemetry-kompatibel),
  sobald Distributed-Tracing-Backend (Tempo/Jaeger) eingeführt wird.

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
