# Technische Spezifikation

## Stack

| Schicht | Technologie | Version |
|---|---|---|
| Sprache | Java | 21 (LTS) |
| Framework | Spring Boot | 3.4.x |
| Frontend | Thymeleaf 3 + light CSS | — |
| DB (dev) | H2 (file) | aktuell |
| DB (prod) | PostgreSQL | 17 |
| Migrationen | Flyway | aus Boot-Parent |
| Build | Maven | 3.9+ |
| Container | Docker (multi-stage) | — |
| CI | GitHub Actions | — |

## Profile

### `dev` (Default für lokale Entwicklung)

- H2 file-based unter `./data/sponsorplatz`
- **Flyway aktiv** (gleiche Migrationen wie prod)
- Hibernate `ddl-auto=validate` — Schema kommt aus Migrationen
- Security: `permitAll()` — kein Login
- H2-Konsole aktiv

### `prod` (Produktion)

- PostgreSQL über `DB_URL`/`DB_USER`/`DB_PASSWORD`
- Flyway aktiv
- Hibernate `ddl-auto=validate`
- Security: Form-Login + OIDC-Vorbereitung
- H2-Konsole **deaktiviert**

## Routen-Tabelle

### Phase 0 (Skelett)

| Methode | Pfad | Zugriff | Beschreibung |
|---|---|---|---|
| GET | `/` | public | Startseite |
| GET | `/actuator/health` | public | Healthcheck |
| GET | `/actuator/info` | public | Build-Info |

### Phase 0.1 (Organisation-CRUD)

| Methode | Pfad | Zugriff | Beschreibung |
|---|---|---|---|
| GET | `/organisationen` | public (dev) | Liste aller Organisationen |
| GET | `/organisationen/neu` | public (dev) | Formular für neue Org |
| POST | `/organisationen/speichern` | public (dev) | Org speichern (anlegen oder aktualisieren) |
| GET | `/organisationen/{slug}` | public (dev) | Detail-Ansicht einer Org |
| GET | `/organisationen/{slug}/bearbeiten` | public (dev) | Edit-Formular |
| POST | `/organisationen/{slug}/loeschen` | public (dev) | Org löschen |

> Hinweis: In dev sind alle Routen offen. Sobald Phase 1 (Auth + Mitgliedschaft) durch ist, gilt: Edit-Routen verlangen `ORG_OWNER` oder `ORG_EDITOR` der jeweiligen Org.

### Phase 0.2 (Mitglieder)

| Methode | Pfad | Zugriff | Beschreibung |
|---|---|---|---|
| GET | `/organisationen/{slug}/mitglieder` | authenticated | Mitglieder-Liste |
| POST | `/organisationen/{slug}/mitglieder/hinzufuegen` | ORG_OWNER | Mitglied hinzufügen |
| POST | `/organisationen/{slug}/mitglieder/{id}/entfernen` | ORG_OWNER | Mitglied entfernen |

### Phase 1.1 (Auth)

| Methode | Pfad | Zugriff | Beschreibung |
|---|---|---|---|
| GET | `/login` | public | Login-Formular |
| POST | `/login` | public | Login verarbeiten (Spring Security) |
| POST | `/logout` | authenticated | Logout |
| GET | `/registrieren` | public | Registrierungs-Formular |
| POST | `/registrieren` | public | Neuen User anlegen |

#### Zugriffs-Regeln ab Phase 1.1

| Pfad-Pattern | Zugriff |
|---|---|
| `/`, `/login`, `/registrieren`, `/css/**`, `/images/**` | `permitAll` |
| `/actuator/health`, `/actuator/info` | `permitAll` |
| `/h2-console/**` (nur dev) | `permitAll` |
| `/organisationen` (GET), `/organisationen/{slug}` (GET) | `permitAll` |
| `/organisationen/neu`, `/organisationen/speichern` | `authenticated` |
| `/organisationen/{slug}/bearbeiten`, `/organisationen/{slug}/loeschen` | `@accessControl.kannOrgEditieren` |
| `/organisationen/{slug}/mitglieder/**` | `@accessControl.kannOrgVerwalten` |
| alle anderen | `authenticated` |

## Verzeichnisstruktur

```
sponsorplatz/
├── src/main/java/ch/sponsorplatz/
│   ├── PlatformApplication.java
│   ├── config/        # SecurityConfig, ModelAttributeNames
│   ├── controller/    # HomeController, OrganisationController, GlobalExceptionHandler
│   ├── dto/           # OrganisationFormDto
│   ├── model/         # Organisation, OrgTyp, OrgStatus
│   ├── repository/    # OrganisationRepository
│   ├── service/       # OrganisationService, SlugGenerator
│   └── startup/
├── src/main/resources/
│   ├── application.properties
│   ├── application-dev.properties
│   ├── application-prod.properties
│   ├── db/migration/V*.sql
│   ├── templates/     # layout, index, error, organisationen, organisation-form, organisation-detail
│   ├── static/        # CSS, Bilder
│   └── messages_de_CH.properties
├── src/test/...
└── specs/
```

## Build & Run

```bash
mvn spring-boot:run
mvn test
mvn clean verify
docker compose up -d postgres mailhog
docker compose --profile app up --build
```

## Sicherheit

- Form-Login mit BCrypt (Phase 1)
- OIDC-Vorbereitung (Phase 1+)
- CSRF an für alle Mutationen
- Spring-Security Default-Headers
- Secrets nur via ENV/Vault, nie in Properties

## Lokalisierung

- Default-Locale `de_CH`
- `messages_de_CH.properties` als Quelle
- CHF-Format `1'234.50`
- Datum `dd.MM.yyyy`
- Zeitzone `Europe/Zurich`
- FR/IT vorbereitet (Phase 5)

## Cloud-Deployment

Ziel: **Oracle Cloud Infrastructure (OCI)** — Container Instances + Managed PostgreSQL + IAM (OIDC) + Object Storage + Vault.
Detail-Spec folgt in Phase 1: `specs/CLOUD_DEPLOYMENT.md`.
