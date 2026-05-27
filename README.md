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

Kuratierte Schweizer Sponsoring-Plattform für **Sport- und Health-Vereine** und für **Marken mit
Health-Affinität** (Krankenkassen, Apotheken, Lebensmittel, Fitness, Stiftungen). Vereine ohne
klaren Sport- oder Gesundheitsbezug werden bei der Verifizierung abgelehnt — diese Schärfe ist
unser Vertrauens-Versprechen an die Sponsoren-Seite.

**Plattform-Bausteine:**

- **Kollaboratives CRM** mit geteilten Sponsoren-Stammdaten + Edit-Rechten pro Verein
- **Öffentlicher Marktplatz** mit Projekt-Profilen, Sponsoring-Paketen, Volltextsuche, SEO
- **Anfrage-Workflow** mit Threaded Messages, Vertrag-Generator, Swiss-QR-Bill
- **Mehrsprachigkeit** DE/FR/IT/EN (Cookie + Profil), CHF-Format, DSG-konform
- **Auth**: Form-Login + 2FA-TOTP + OIDC (Google/Entra/SwissID/edu-ID)
- **Multi-Cloud Warm-DR**: OCI Always-Free + Azure Sweden Central, eigene CD-Pipelines

## Tech-Stack

| Schicht | Technologie |
|---|---|
| Sprache / Framework | Java 21 LTS · Spring Boot 3.5 |
| Frontend | Thymeleaf, eigenes CSS (Dashboard-Stil) |
| DB | H2 (dev, file-based, `MODE=PostgreSQL`) · PostgreSQL 17 (prod) |
| Migrationen | Flyway V1–V46 |
| Build / Container | Maven 3.9+ · Docker Multi-Stage (`eclipse-temurin:21-jre-jammy`) |
| CI/CD | GitHub Actions (CI + getrennte CDs für OCI + Azure) |
| Cloud-Storage | OCI Object Storage · Azure Blob Storage · Local Volume |
| Error-Tracking | Sentry (Browser + Java, off-by-default ohne DSN) |

Architektur statisch durchgesetzt: **13 ArchUnit-Regeln** (ARCH-01..13), **~700 Tests**,
Feature-Folder-Topologie ohne Cycles.

## Schnellstart

```bash
git clone https://github.com/fabianaschwanden/sponsorplatz.git
cd sponsorplatz
mvn spring-boot:run                              # http://localhost:8080
```

- **Dev-Login:** `dev@sponsorplatz.ch` / Passwort siehe `application-dev.properties`
- **H2-Konsole:** http://localhost:8080/h2-console (User `sa`, Passwort leer)
- **Demo-Modus** mit Beispieldaten: `mvn spring-boot:run -Dspring-boot.run.profiles=demo`
- **Tests:** `mvn test` · einzeln: `-Dtest=OrganisationServiceTest`
- **Postgres + MailHog lokal:** `docker compose up -d postgres mailhog`
- **DB-Reset bei Migrations-Konflikten:** `rm -rf data/`

VS Code wird empfohlen — `.vscode/`-Konfiguration mit allen nötigen Extension-Recommendations
ist im Repo. Erstes Öffnen: *„Install All"*-Notification akzeptieren.

## Projekt-Struktur

Bounded-Context-orientiert — jedes fachliche Package hält Entities, Repositories, Services,
Controller und DTOs zusammen.

```
src/main/java/ch/sponsorplatz/
├── shared/          # Querschnitt: SecurityConfig, MailService, StorageService, PdfGenerator
├── benutzer/        # AppUser, Auth, OIDC, 2FA, Einstellungen, Datenexport
├── organisation/    # Organisation, Mitgliedschaft, AccessControl, Hierarchie, Sponsor-Reg
├── projekt/         # Projekt, SponsoringPakete, Watchlist, Medien, Marktplatz, Events
├── anfrage/         # SponsoringAnfrage, Vertrag, Rechnung, QR-Bill, Nachrichten
├── einladung/       # Token-basierte Mitglieder-Einladung
├── benachrichtigung/# In-App-Glocke + Mail-Notifications
├── audit/           # AuditLog mit umgebung-Marker + DSG-Datenexport
├── backup/          # DB + Datei-Backup, Cloud-Upload (OCI + Azure)
├── aufgabe/         # Customizable Task-Engine
├── ops/             # Ops-Dashboard, RecentErrors, Alerts
├── admin/           # Verifizierungs-Queue, Audit-Viewer, Backups, Mail-Settings
└── home/            # Public: Landing, Marktplatz, Impressum/DSG/AGB

src/main/resources/
├── application*.properties         # default, dev, prod, demo, cloud-free, cloud-azure
├── db/migration/V1..V46__*.sql     # additive Migrationen, strikt nicht-destruktiv
├── templates/                      # Thymeleaf nach Bounded Context
└── messages_{de_CH,en,fr_CH,it_CH}.properties

infra/
├── envs/{staging-free,azure-staging}/   # Terraform für OCI + Azure
├── scripts/                              # Helper (compose-Patcher, etc.)
└── staging-free/README.md                # Setup-, Rollback-, Aktivierungs-Anleitungen
```

## Roadmap

Stand 26.05.2026. Detaillierte Plan-/Slice-Sicht in [`specs/ROADMAP.md`](specs/ROADMAP.md).

| Phase | Inhalt | Status |
|---|---|:---:|
| 0–9 | Fundament: Org/Mitglied/AccessControl, Self-Reg, Projekte+Pakete, Marktplatz, Anfragen, Vertrag/Rechnung, Mehrsprachigkeit | ✅ |
| 10 | Production-Readiness: Monitoring, Sentry, DSG-Pages, Security-Hardening (10.4 → Phase 14) | ✅ |
| 11 + 12 | Backup/Restore, Ops-Dashboard, Customizable Task-Engine | ✅ |
| 13 | Pre-Pilot-Hardening: A11y-auth-Smoke, 2FA-TOTP (+ Admin-Reset), OIDC Multi-Provider | ✅ |
| **14** | **Produktivschaltung sponsorplatz.ch** — HTTPS, prod-SMTP, SPF/DKIM/DMARC, DNS, Pilot-Welle | 🔜 |
| 15.3 | Multi-Cloud Azure als Warm-DR (Slices 1–4 ✅, 5–7 DNS-Failover/Cross-Replication offen) | ⏳ |
| 15.4 | Datei-Backup + Restore als ZIP | ✅ |
| 15.1 / 15.2 | Echte Zahlungs-Provider-Integration, Mahnwesen | 📋 |

## Dokumentation

| Datei | Inhalt |
|---|---|
| [`CLAUDE.md`](CLAUDE.md) | Stack, Phase-Status, TDD-Pflicht, ARCH-Regeln — Einstieg für neue Devs + KI-Assistenten |
| [`.instructions.md`](.instructions.md) | Clean Code, TDD-Workflow, Naming-Conventions |
| [`docs/konzept.md`](docs/konzept.md) | High-Level-Konzept v3 mit Rollen, Datenmodell, Vision |
| [`specs/`](specs/) | Specs (Datenmodell, Rollenkonzept, Teststrategie, ROADMAP, AUTH_*) |
| [`docs/adr/`](docs/adr/) | Architecture Decision Records |
| [`infra/staging-free/README.md`](infra/staging-free/README.md) | OCI-Setup, OIDC-Aktivierung, Rollback-Pfad |
| [`infra/envs/azure-staging/README.md`](infra/envs/azure-staging/README.md) | Azure-Setup + Cloud-spezifische Unterschiede |
| [`DEPLOYMENT.md`](DEPLOYMENT.md) | Pilot-Launch-Runbook |

## Mitmachen

Strikter Workflow: **Spec → Test → Implementation**. Details in [`.instructions.md`](.instructions.md).
Jede Code-Änderung braucht einen Spec-Eintrag + Test (rot zuerst), bevor die Implementation kommt.

## Lizenz

MIT — siehe [LICENSE](LICENSE).
