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

UI-Skelett für angemeldete Benutzer unter `/dashboard`. Werte werden in folgenden Iterationen verkabelt — die Tests prüfen aktuell nur das Routing, Auth und das Vorhandensein der Model-Attribute.

| ID | Test-Klasse | Beschreibung |
|---|---|---|
| **DASH-01** | `DashboardControllerTest` | GET `/dashboard` anonym → Redirect zu `/login` |
| **DASH-02** | `DashboardControllerTest` | GET `/dashboard` eingeloggt → 200 + View `dashboard` |
| **DASH-03** | `DashboardControllerTest` | Model enthält `aktuellerMonat`, `aktuelleKw`, `anzahlOrganisationen`, `anzahlProjekte`, `anzahlAnfragen`, `anzahlOffeneAnfragen` |

(Wird mit Phase 1 — Self-Registrierung, Spring Security Form-Login — weiter ergänzt.)

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
