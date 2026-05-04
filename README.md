# Sponsorplatz

> **Vereine finden Sponsoren. Sponsoren finden Vereine. *Endlich.***

Schweizer Sponsoring-Plattform für Vereine und Sponsoren — kollaborativ, branchenoffen, DSG-konform.

[![CI](https://github.com/fabianaschwanden/sponsorplatz/actions/workflows/ci.yml/badge.svg)](https://github.com/fabianaschwanden/sponsorplatz/actions/workflows/ci.yml)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

---

## Was ist Sponsorplatz?

Sponsorplatz ist eine offene, schweizerische Plattform, auf der **Vereine** ihre Projekte und Events präsentieren — und **Unternehmen** als Sponsoren passende Engagements finden. Die Plattform vereint:

- **Kollaboratives CRM** für Sponsoren-Daten (geteilte Stammdaten, Edit-Rechte pro Verein)
- **Marktplatz** mit öffentlichen Projekt-Profilen, Sponsoring-Paketen und Anfrage-Workflow
- **Werkzeuge** für Vereine: Excel-Im/Export, Word-Serienbrief, Datenbereinigung gegen Zefix
- **Schweizer Fokus**: CHF, DE/FR/IT, Hosting in CH, DSG-konform

## Phase

Aktuell: **Phase 0 — Fundament**. Skelett-App lauffähig, Multi-Org-Modell und Marktplatz folgen.

## Tech-Stack

| Schicht | Technologie |
|---|---|
| Sprache | Java 21 LTS |
| Framework | Spring Boot 3.4 |
| Frontend | Thymeleaf + Bootstrap-light |
| DB (dev) | H2 (file-based) |
| DB (prod) | PostgreSQL 17 |
| Migrationen | Flyway |
| Build | Maven 3.9+ |
| Container | Docker Multi-Stage |
| CI | GitHub Actions |

## Schnellstart

### Voraussetzungen

- Java 21+ (`java -version`)
- Maven 3.9+ (`mvn -v`)
- Docker (optional, für PostgreSQL und MailHog lokal)
- **VS Code** (empfohlen) — siehe [VS Code Setup](#vs-code-setup) unten

### Lokale Entwicklung (mit H2)

```bash
git clone https://github.com/fabianaschwanden/sponsorplatz.git
cd sponsorplatz
mvn spring-boot:run
```

→ App läuft auf http://localhost:8080
→ H2-Konsole auf http://localhost:8080/h2-console (User `sa`, Passwort leer)

### Tests laufen lassen

```bash
mvn test
```

### Mit PostgreSQL + MailHog (Docker)

```bash
docker compose up -d postgres mailhog
mvn spring-boot:run -Dspring-boot.run.profiles=prod \
    -Dspring-boot.run.arguments="--spring.datasource.url=jdbc:postgresql://localhost:5432/sponsorplatz \
                                  --spring.datasource.username=sponsor \
                                  --spring.datasource.password=sponsor_dev"
```

→ MailHog UI: http://localhost:8025

### Komplette App im Docker-Container

```bash
docker compose --profile app up --build
```

## VS Code Setup

Das Projekt ist mit `.vscode/`-Konfiguration ausgestattet — beim ersten Öffnen schlägt VS Code alle nötigen Extensions automatisch vor.

```bash
code ~/git/sponsorplatz
```

**Beim ersten Start:**

1. **Extensions installieren** — VS Code zeigt eine Notification *„This workspace has extension recommendations"*. → **Install All** klicken.
   - Java Extension Pack (Microsoft)
   - Spring Boot Extension Pack (VMware)
   - Docker, XML, YAML, GitLens, EditorConfig, Code Spell Checker (DE+EN), Todo Tree, GitHub Actions

2. **Java SDK 21 wählen** — falls VS Code mehrere JDKs findet:
   `Cmd+Shift+P` → *„Java: Configure Java Runtime"* → JDK 21 als default.

3. **Maven importieren** — VS Code lädt automatisch `pom.xml` und holt Dependencies. Status unten in der Status-Leiste prüfen.

**App starten in VS Code:**

| Aktion | Wie |
|---|---|
| App starten (dev) | `F5` → *„Sponsorplatz (dev)"* |
| App starten (prod-lokal) | `F5` → *„Sponsorplatz (prod-lokal)"* (braucht Docker-Postgres) |
| Tests laufen lassen | Test-Explorer (Becher-Symbol links) |
| Tasks (Maven, Docker) | `Cmd+Shift+P` → *„Tasks: Run Task"* |
| Spring Boot Dashboard | linker Sidebar → Spring-Symbol → Start/Stop/Debug |

**Vordefinierte Tasks** (`Cmd+Shift+P` → *„Tasks: Run Task"*):

- `mvn: clean` / `compile` / `test` / `package` / `verify`
- `spring-boot:run (dev)`
- `docker: compose up (postgres + mailhog)` / `compose down`
- `docker: app starten (build + run)`

**Code-Style:** Format-on-save aktiv. Ruler bei 120 Zeichen. Java-Formatter-Profil unter `.vscode/java-formatter.xml`.

## Projekt-Struktur

```
sponsorplatz/
├── src/main/java/ch/sponsorplatz/
│   ├── PlatformApplication.java
│   ├── config/        # SecurityConfig, ModelAttributeNames
│   ├── controller/    # HomeController, GlobalExceptionHandler
│   ├── service/       # (wird gefüllt ab Phase 0.1)
│   └── repository/    # (wird gefüllt ab Phase 0.1)
├── src/main/resources/
│   ├── application*.properties
│   ├── db/migration/  # Flyway V1+
│   ├── templates/     # Thymeleaf
│   ├── static/        # CSS, Bilder
│   └── messages*.properties
├── src/test/...
├── specs/             # Architektur, Datenmodell, Tests, Roadmap
├── Dockerfile
├── docker-compose.yml
├── .github/workflows/
└── pom.xml
```

## Roadmap (kurz)

| Phase | Inhalt | Dauer |
|---|---|---|
| 0 | Org-Profil + Mitgliedschaft + AccessControl | 2 W |
| 1 | Self-Reg + Verifizierung | 2 W |
| 2 | Sponsoring-Pakete + Sichtbarkeit + Medien | 3 W |
| 3 | Marktplatz Public + SEO | 3-4 W |
| 4 | Anfragen + Konversation | 3 W |
| 5+ | Wachstum: Watchlist, Matching, Mehrsprachigkeit, Verträge, Payments |

Vollständig dokumentiert in [`specs/ROADMAP.md`](specs/ROADMAP.md).

## Dokumentation

| Datei | Inhalt |
|---|---|
| [`specs/PROJEKT_INFO.md`](specs/PROJEKT_INFO.md) | Produkt-Übersicht, Vision, Zielgruppen |
| [`specs/TECHNISCHE_SPEZIFIKATION.md`](specs/TECHNISCHE_SPEZIFIKATION.md) | Stack, Routen, Profile, Sicherheit |
| [`specs/DATENMODELL.md`](specs/DATENMODELL.md) | Entitäten, Felder, Beziehungen |
| [`specs/ROLLENKONZEPT.md`](specs/ROLLENKONZEPT.md) | Rollen, Permissions, AccessControl |
| [`specs/TESTSTRATEGIE.md`](specs/TESTSTRATEGIE.md) | Test-Ebenen, Testfälle, Smoke-Checkliste |
| [`specs/ROADMAP.md`](specs/ROADMAP.md) | Phasen, Iterationen, MVP-Definition |
| [`.instructions.md`](.instructions.md) | Clean Code, TDD-Workflow, Conventions |

## Mitmachen

Der Workflow ist strikt **Spec → Test → Implementation**.
Details in [`.instructions.md`](.instructions.md).

## Lizenz

[MIT](LICENSE) © 2026 Fabian Aschwanden
