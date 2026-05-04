# Teststrategie

## Test-Ebenen

| Ebene | Werkzeug | Zweck |
|---|---|---|
| Unit | JUnit 5 + Mockito + AssertJ | Service-Logik, Konverter, Validierung |
| Repository | `@DataJpaTest` (H2 in-memory) | JPA-Queries, Constraints |
| Web | `@WebMvcTest` + MockMvc | HTTP-Routen, Redirects |
| Security | `@SpringBootTest` mit Profilen | Form-Login, AccessControl, OIDC |
| Container | Testcontainers (PostgreSQL) | echte DB für Migrations-Tests |
| Smoke | Manuell + CI Healthcheck | Startup, Browser-Happy-Path |

## Konventionen

- **Naming:** `<Klasse>Test` für Unit-/Web-/Repository-Tests, `<Klasse>IT` für Integration
- **Test-IDs:** Pro Test eine ID nach Schema `<Bereich>-<Nummer>` (z.B. `ORG-01`, `AC-05`, `SP-12`)
- Test-IDs werden in dieser Datei in einer Tabelle gepflegt
- Jede Spec-Anforderung referenziert ihre Test-ID

## Testfall-Katalog (wird mit jeder Iteration erweitert)

### Phase 0 (Skelett)

| ID | Test-Klasse | Beschreibung |
|---|---|---|
| SP-01 | `PlatformApplicationTests` | Spring-Context startet sauber |
| SP-02 | `HomeControllerTest` | GET / → 200 + index-Template |

### Phase 0.1 (Org + Mitgliedschaft)

| ID | Test-Klasse | Beschreibung |
|---|---|---|
| ORG-01 | `OrganisationRepositoryTest` | persistieren, slug UNIQUE, default-status PENDING |
| ORG-02 | `OrganisationServiceTest` | CRUD mit Validierung |
| MG-01 | `MitgliedschaftRepositoryTest` | UNIQUE (user, org, rolle) |
| AC-01 | `AccessControlTest` | kannOrgEditieren mit Mock-Mitgliedschaft |
| AC-02 | `AccessControlTest` | PLATFORM_ADMIN darf alles |
| AC-03 | `AccessControlTest` | nicht-eingeloggt → false |

(Wird mit Phase 1, 2, ... erweitert.)

## CI

- Bei jedem Push und PR auf `main`: `mvn -B clean verify` + Docker-Build-Smoke
- JaCoCo-Coverage als Artifact pro Run
- Surefire-Reports als Artifact pro Run

## Smoke-Test-Checkliste (manuell vor Release)

- [ ] App startet (`mvn spring-boot:run`)
- [ ] http://localhost:8080 → Startseite lädt
- [ ] http://localhost:8080/actuator/health → `{"status":"UP"}`
- [ ] http://localhost:8080/h2-console → erreichbar (dev only)
- [ ] `mvn test` — alle grün
- [ ] `docker compose up --build app` → läuft im Container
