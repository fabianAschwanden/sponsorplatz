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

#### AccessControl im Controller (ORG-12)

| ID | Test-Klasse | Beschreibung |
|---|---|---|
| **ORG-12** | `OrganisationControllerTest` | GET `/organisationen/{slug}/bearbeiten` ohne ORG_EDITOR → 403 |

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
