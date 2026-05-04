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
- Hibernate `ddl-auto=update` (schnelles Iterieren in Phase 0)
- Flyway deaktiviert
- Security: `permitAll()` — kein Login
- H2-Konsole aktiv

### `prod` (Produktion)

- PostgreSQL über `DB_URL`/`DB_USER`/`DB_PASSWORD`
- Hibernate `ddl-auto=validate`
- Flyway aktiv, Schema kommt aus Migrationen
- Security: Form-Login + OIDC-Vorbereitung
- H2-Konsole **deaktiviert**

## Routen-Tabelle (Phase 0)

| Methode | Pfad | Zugriff | Beschreibung |
|---|---|---|---|
| GET | `/` | public | Startseite (Phase 0) |
| GET | `/actuator/health` | public | Healthcheck |
| GET | `/actuator/info` | public | Build-Info |

Wird mit jeder Iteration erweitert.

## Verzeichnisstruktur

```
sponsorplatz/
├── src/main/java/ch/sponsorplatz/
│   ├── PlatformApplication.java
│   ├── config/        # SecurityConfig, ModelAttributeNames
│   ├── controller/    # HomeController, GlobalExceptionHandler
│   ├── dto/
│   ├── model/
│   ├── repository/
│   ├── service/
│   └── startup/
├── src/main/resources/
│   ├── application.properties
│   ├── application-dev.properties
│   ├── application-prod.properties
│   ├── db/migration/V*.sql
│   ├── templates/
│   ├── static/
│   └── messages_de_CH.properties
├── src/test/...
└── specs/
```

## Build & Run

```bash
# Lokal
mvn spring-boot:run

# Tests
mvn test

# JAR bauen
mvn clean package

# Docker
docker compose up -d postgres mailhog
docker compose --profile app up --build
```

## Sicherheit

- Form-Login mit BCrypt-Passwörtern (Phase 1)
- OIDC-Vorbereitung (Phase 1+, optional)
- CSRF an für alle Mutationen
- Security-Headers Standard von Spring
- Secrets nur über ENV / Vault, nie in Properties

## Lokalisierung

- Default-Locale: `de_CH`
- `messages_de_CH.properties` als Quelle
- CHF-Format `1'234.50`
- Datum `dd.MM.yyyy`
- Zeitzone `Europe/Zurich`
- Vorbereitet für FR/IT (Phase 5)

## Cloud-Deployment

Ziel-Plattform: **Oracle Cloud Infrastructure (OCI)** — Container Instances + Managed PostgreSQL + IAM (OIDC) + Object Storage + Vault.

Details folgen in `specs/CLOUD_DEPLOYMENT.md` (Phase 1).
