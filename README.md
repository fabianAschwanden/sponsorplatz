# Sponsorplatz

> **Wo Vereine und Marken zueinander finden.**
> *Sponsoring für Sport und Gesundheit · Schweiz*

Schweizer Sponsoring-Plattform mit striktem Health-Fokus — kollaborativ, kuratiert, DSG-konform.
Sport, Bewegung, Reha, Behindertensport, Seniorensport, Prävention, Mental Health, Ernährung,
Wellness, Selbsthilfe, Patientenorganisationen.

[![CI](https://github.com/fabianaschwanden/sponsorplatz/actions/workflows/ci.yml/badge.svg)](https://github.com/fabianaschwanden/sponsorplatz/actions/workflows/ci.yml)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

---

## Was ist Sponsorplatz?

Sponsorplatz ist die kuratierte Schweizer Sponsoring-Plattform für **Sport- und Health-Vereine**
und für **Marken mit Health-Affinität** (Krankenkassen, Apotheken, Lebensmittel, Fitness, Stiftungen).
Vereine ohne klaren Sport- oder Gesundheitsbezug werden bei der Verifizierung abgelehnt — diese
Schärfe ist unser Vertrauens-Versprechen an die Sponsoren-Seite.

Die Plattform vereint:

- **Kuratierter Health-Marktplatz** — strikt auf Sport und Gesundheit fokussiert (elf Branchen)
- **Kollaboratives CRM** für Sponsoren-Daten (geteilte Stammdaten, Edit-Rechte pro Verein)
- **Marktplatz** mit öffentlichen Projekt-Profilen, Sponsoring-Paketen und Anfrage-Workflow
- **Engagement-Schaufenster** — öffentliche Darstellung aktiver Sponsoring-Partnerschaften
- **Mehrsprachigkeit** — DE, FR, IT und EN (Cookie-basiert, User-Profil-gesteuert)
- **Werkzeuge** für Vereine: Medien-Upload, Vertrags-/Rechnungs-PDF, QR-Bill, Event-Kalender
- **Schweizer Fokus**: CHF, DE/FR/IT/EN, Hosting in CH, DSG-konform

## Phase

Aktuell: **Phase 10 — Production-Readiness & Pilot-Launch** ⏳ — Code-seitig komplett (Monitoring, Sentry, DSG, Security-Hardening, Smoke-Tests, Kontakt-Funnel), 10.4-Ops (HTTPS/SMTP-prod/DNS/Backups) noch offen.
Architektur statisch durchgesetzt: **15 ArchUnit/Lint-Regeln (ARCH-01..15)**, **551 Tests**, Feature-Folder-Topologie ohne Cycles (Java + Templates parallel strukturiert).

### Umgesetzte Features

| Phase | Inhalt | Status |
|---|---|---|
| 0 | Skelett, Organisation-Entity, AppUser, Mitgliedschaft, AccessControl, Dashboard | ✓ |
| 1 | Self-Reg, Form-Login, E-Mail-Verifizierung, Admin-Verifizierung, Einladungen | ✓ |
| 2 | Projekte, Sponsoring-Pakete, Medien-Assets (Upload/Galerie/Cover) | ✓ |
| 3 | Öffentlicher Marktplatz, Volltextsuche, SEO/Sitemap, Vereinsprofile | ✓ |
| 4 | Sponsoring-Anfragen, Nachrichten-Thread, E-Mail-Notifications, Sponsor-Self-Reg | ✓ |
| 5 | Watchlist, Matching-Empfehlungen, Audit-Log, Backups, In-App-Notifications | ✓ |
| 5+ | Passwort-Reset, Brute-Force-Schutz, Hierarchische Firmenstruktur, Verträge/Rechnungen, Postgres-Volltext | ✓ |
| 6 | Einstellungen, DSG-Datenexport, Passwort-Änderung | ✓ |
| 7 | Health-Story: Branche-Filter, Vereins-Health-Hero, Marken-Landing-Page | ✓ |
| 8 | MVP-Reife: Demo-Seed, Engagement-Schaufenster, OG-Card-Generator | ✓ |
| 9 | Mehrsprachigkeit DE/FR/IT/EN, Zahlungs-Provider, Event-Entity | ✓ |
| 10.1 | Monitoring: TraceId-Filter mit W3C-`traceparent`, Actuator-Probes, JSON-Logs | ✓ |
| 10.2 | Sentry Error-Tracking (Java + Browser, DSG-konform, SRI-Pinning) | ✓ |
| 10.3 | DSG: Impressum, Datenschutz, AGB (4-sprachig), kein Cookie-Banner nötig | ✓ |
| 10.4 | Pilot-Launch: Smoke-IT, Kontakt-Funnel ✓ — HTTPS/SMTP-prod/DNS Ops ⏳ | ⏳ |
| 10.5 | Security-Hardening: CSP, Permissions-Policy, Referrer-Policy, SRI | ✓ |
| 11 | Pilot-Hardening: Onboarding, Support, Anhänge, Verträge, Rechnungen | ✓ |
| 12 | Customizable Task-Engine (Aufgaben mit Trigger-Regeln, Sidebar-Badge) | ✓ |

## Tech-Stack

| Schicht | Technologie |
|---|---|
| Sprache | Java 21 LTS |
| Framework | Spring Boot 3.4 |
| Frontend | Thymeleaf + Bootstrap-light |
| DB (dev) | H2 (file-based, `MODE=PostgreSQL`) |
| DB (prod) | PostgreSQL 17 |
| Migrationen | Flyway (26 Versionen) |
| Build | Maven 3.9+ |
| Container | Docker Multi-Stage |
| CI | GitHub Actions |
| i18n | Spring MessageSource (DE/FR/IT/EN) |

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
→ Dev-Login: `dev@sponsorplatz.ch` (Passwort siehe `sponsorplatz.dev.passwort` in application-dev.properties)

### Demo-Modus (mit Beispieldaten)

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=demo
```

→ Lädt 5 Vereine, 10 Projekte, 3 Sponsor-Orgs mit realistischen Daten
→ Gelber „DEMO"-Banner im Header

### Tests laufen lassen

```bash
mvn test                                    # alle (~350+ Tests)
mvn test -Dtest=OrganisationServiceTest     # einzeln
mvn clean verify                            # inkl. JaCoCo-Coverage
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

### H2-DB-Reset bei Migration-Konflikten

```bash
rm -rf data/
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

Bounded-Context-orientiert — jedes fachliche Package hält seine eigenen Entities, Repositories, Services, Controller und DTOs zusammen.

```
sponsorplatz/
├── src/main/java/ch/sponsorplatz/
│   ├── PlatformApplication.java
│   ├── shared/                 # Querschnitts-Infrastruktur
│   │   ├── config/             # SecurityConfig, LocaleConfig, RateLimitFilter, LoginSuccessHandler
│   │   ├── exception/          # NotFoundException, GlobalExceptionHandler
│   │   ├── util/               # SlugGenerator, TokenGenerator
│   │   ├── pdf/                # PdfGeneratorService
│   │   ├── storage/            # StorageService + Lokal-Implementierung
│   │   └── mail/               # MailService (SMTP-Abstraktion)
│   │
│   ├── benutzer/               # AppUser, Auth, Profil, Verifizierung, PasswortReset, Einstellungen
│   ├── organisation/           # Organisation, Mitgliedschaft, AccessControl, Branche, Sponsor-Self-Reg
│   ├── projekt/                # Projekt, SponsoringPakete, Watchlist, MedienAssets, Marktplatz,
│   │                           # Suche, Matching, Dashboard, Sitemap, Events, OG-Images
│   ├── anfrage/                # SponsoringAnfrage, Vertrag, Rechnung, QR-Bill, Nachrichten,
│   │                           # Engagement-Schaufenster, PaymentProvider
│   ├── einladung/              # Einladung + Token-basierte Annahme
│   ├── benachrichtigung/       # In-App-Glocke (NotificationService + Badge-Polling)
│   ├── audit/                  # AuditLog + DSG-Datenexport
│   ├── admin/                  # Admin-UI: Verifizierungs-Queue, Audit, Backups
│   └── home/                   # HomeController, InfoController (Impressum/DSG)
│
├── src/main/resources/
│   ├── application*.properties           # default + dev + prod + demo + cloud-free
│   ├── db/migration/V1..V26*.sql         # Flyway (26 Migrationen)
│   ├── templates/                        # Thymeleaf
│   ├── static/css/                       # main.css
│   ├── messages_de_CH.properties         # Deutsch (Schweiz) — Default
│   ├── messages_fr_CH.properties         # Französisch (Schweiz)
│   ├── messages_it_CH.properties         # Italienisch (Schweiz)
│   └── messages_en.properties            # Englisch
│
├── src/test/...                          # ~350+ Tests (Unit, Web, Repo, Integration)
├── specs/                                # Architektur, Datenmodell, Tests, Roadmap
├── docs/                                 # Konzept, Marketing, Naming, Pitch
├── infra/                                # OCI-Infrastruktur, Terraform, Docker-Compose (Staging)
├── Dockerfile
├── docker-compose.yml
├── .github/workflows/
└── pom.xml
```

## Mehrsprachigkeit

Die Plattform unterstützt vier Sprachen: **Deutsch**, **Französisch**, **Italienisch** und **Englisch**.

- **Cookie-basiert**: Sprache wird per `lang`-Cookie gespeichert (365 Tage)
- **URL-Override**: `?lang=de|fr|it|en` wechselt sofort die Sprache
- **User-Profil**: Eingeloggte Benutzer können ihre bevorzugte Sprache in den Einstellungen hinterlegen — wird beim Login automatisch synchronisiert
- **Footer-Switcher**: DE / FR / IT / EN auf jeder öffentlichen Seite

## Roadmap (kurz)

| Phase | Inhalt | Status |
|---|---|---|
| 0–9 | Fundament bis Mehrsprachigkeit | ✓ |
| 10.1–10.3, 10.5 | Monitoring (W3C-Trace), Sentry, DSG-Pages, Security-Hardening | ✓ |
| 10.4 | Pilot-Launch — Smoke-Tests + Kontakt-Funnel code-seitig ✓, Ops (HTTPS/SMTP/DNS) | ⏳ |
| 11 | Pilot-Hardening (Onboarding, Support, Anhänge, Verträge, Rechnungen) | ✓ |
| 12 | Customizable Task-Engine (Aufgaben + Sidebar-Badge) | ✓ |

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
| [`specs/BETA_TESTPLAN.md`](specs/BETA_TESTPLAN.md) | Pilot-Akzeptanz-Tests (manuelle Checks) |
| [`DEPLOYMENT.md`](DEPLOYMENT.md) | Pilot-Launch-Runbook (HTTPS, SMTP, DNS, Backups) |
| [`docs/adr/README.md`](docs/adr/README.md) | Architecture Decision Records |
| [`docs/architektur/README.md`](docs/architektur/README.md) | C4-Diagramme (Structurizr-DSL) |
| [`.instructions.md`](.instructions.md) | Clean Code, TDD-Workflow, Conventions |
| [`docs/konzept.md`](docs/konzept.md) | Vollständiges Konzept-Dokument |
| [`infra/README.md`](infra/README.md) | Infrastruktur-Übersicht (OCI) |

## Mitmachen

Der Workflow ist strikt **Spec → Test → Implementation**.
Details in [`.instructions.md`](.instructions.md).

## Lizenz

[MIT](LICENSE) © 2026 Fabian Aschwanden
