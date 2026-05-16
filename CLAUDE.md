# CLAUDE.md — Kontext für Claude in VS Code

> Diese Datei wird automatisch von Claude Code / Cursor / ähnlichen IDE-Integrationen geladen.
> Sie ist die zentrale Anlaufstelle für KI-Assistenten, die an diesem Projekt mitarbeiten.

---

## Projekt: Sponsorplatz

**Vision:** Schweizer Sponsoring-Plattform für **Sport und Gesundheit** — *Wo Vereine und Marken zueinander finden.*
**Nische (strikt):** Wir nehmen ausschliesslich Vereine auf, deren Mission im Sport- oder Gesundheitsbereich liegt.
Themen-Umfang ist **breit:** Sport, Bewegung, Reha, Behindertensport, Seniorensport, Prävention, Mental Health,
Ernährung, Wellness, Selbsthilfe, Patientenorganisationen — siehe `Branche`-Enum.
**Modell:** Kollaborative Plattform — mehrere Vereine teilen eine offene Datenbasis, Edit-Rechte über `Mitgliedschaft`.
**Sprache:** Deutsch (Code, Logs, Specs). Default-Locale `de_CH`. CHF, Datum `dd.MM.yyyy`.
**Lizenz:** MIT.

## Tech-Stack (zwingend einhalten)

| Schicht | Wahl |
|---|---|
| Sprache | Java 21 LTS |
| Framework | Spring Boot 3.4.x |
| Frontend | Thymeleaf + light CSS (kein SPA) |
| DB dev | H2 (file: `./data/sponsorplatz`) |
| DB prod | PostgreSQL 17 |
| Schema | Flyway (versioniert, additiv) |
| Build | Maven 3.9+ |
| Container | Docker multi-stage |
| CI | GitHub Actions |

---

## Wo stehen wir gerade

**Phase 0 — Skelett:** ✅ erledigt
**Phase 0.1 — Organisation-Entity:** ✅ erledigt (V2-Migration, 21 Tests)
**Phase 0.2 — AppUser + Mitgliedschaft + AccessControl:** 🔜 als nächstes

Vollständige Roadmap in [`specs/ROADMAP.md`](specs/ROADMAP.md), detaillierte Phase-Pläne in `docs/` (siehe unten).

### Zuletzt umgesetzt (Phase 0.1)

- Migration `V2__organisation.sql` — Tabelle mit ENUM-CHECK-Constraints
- Entity `Organisation` mit `OrgTyp` (VEREIN/UNTERNEHMEN/STIFTUNG/ANDERE) und `OrgStatus` (PENDING/VERIFIED/ACTIVE/SUSPENDED)
- `OrganisationRepository`, `OrganisationService`, `SlugGenerator` (Umlaute → ASCII)
- Controller mit Routen `/organisationen[/neu|/{slug}|/{slug}/bearbeiten|/{slug}/loeschen]`
- 3 Templates (Liste, Form, Detail)
- 21 Tests: SlugGenerator (6), OrganisationRepository (4), OrganisationService (5), OrganisationController (6)
- Specs aktualisiert: DATENMODELL, TECHNISCHE_SPEZIFIKATION, TESTSTRATEGIE
- dev-Profil: Flyway aktiv, `ddl-auto=validate`

### Nächste Iteration: Phase 0.2

1. Migration `V3__app_user_und_mitgliedschaft.sql` mit Tabellen `app_user` (BCrypt-Pw) + `mitgliedschaft` (UNIQUE user_id, org_id, rolle)
2. Entities `AppUser`, `Mitgliedschaft`, `Rolle` (ORG_OWNER, ORG_EDITOR, ORG_VIEWER)
3. Repositories + Services
4. `AccessControl`-Bean mit `kannOrgEditieren(orgId, auth)` und `kannOrgVerwalten(orgId, auth)` — siehe [`specs/ROLLENKONZEPT.md`](specs/ROLLENKONZEPT.md)
5. Mitglieder-Verwaltungs-UI unter `/organisationen/{slug}/mitglieder`
6. Tests: AC-01 bis AC-08, AU-01 bis AU-05, MG-01 bis MG-04

---

## Verbindliche Arbeitsweise

Diese Regeln gelten für jede Code-Änderung:

### TDD-Pflicht (in dieser Reihenfolge)

```
1. SPEC   → in specs/ beschreiben, was passieren soll
2. TEST   → Test schreiben, der zunächst rot ist
3. IMPL   → Minimale Implementation, die den Test grün macht
```

> **Kein produktiver Code ohne vorherigen Test. Kein Test ohne vorherige Spec.**

### Clean-Code-Regeln (aus `.instructions.md`)

- **Deutsche Domain-Sprache:** `speichere`, `findeNachSlug`, `kannOrgEditieren` — nicht `save`, `findBySlug`
- **Keine Abkürzungen:** `sponsoringPaket` statt `sp`
- **Booleans als Aussage:** `istVerifiziert`, `kannEditieren`, `hatFehler`
- **Eine Aufgabe pro Methode**, max. 2–3 Einrückungs-Ebenen
- **Guard Clauses** statt tiefes `else`-Nesting
- **Keine Magic Strings:** Model-Attribute-Keys in `ModelAttributeNames`
- **Konstanten** als `private static final`
- **Services werfen** spezifische Exceptions:
  - `NotFoundException` (404) — Slug/ID nicht gefunden
  - `IllegalArgumentException` (400) — ungültige Eingabe (Slug-Konflikt, leerer Name)
  - `IllegalStateException` (409) — inkonsistenter Zustand (z.B. Org löschen mit Mitgliedschaften)
  - `AccessDeniedException` (403) — fehlende Berechtigung
- **Controller fangen keine** Business-Fehler — `GlobalExceptionHandler` übernimmt das Mapping auf HTTP-Statuscodes und rendert `error.html`

### View-DTO-Pflicht (Entities verlassen Service-Layer NICHT)

> **Verbindlich für jede neue oder geänderte Controller-Methode, die ein Template rendert.**
> Verstoß = Code Review-Block. Bei Verletzung: zuerst View-DTO nachziehen, dann Feature mergen.

**Regel:** Im Controller darf `model.addAttribute(...)` ausschliesslich Java-Records aus `ch.sponsorplatz.dto.*View` (oder `*FormDto` für Schreibe-Forms) bekommen. **Keine** JPA-Entity, **keine** `List<Entity>`, **keine** `Optional<Entity>`.

**Schnell-Check in Code-Review:**
```bash
# Diese grep-Zeile darf NIE Treffer liefern (außer in den fachlichen Packages und Tests):
grep -rn 'model.addAttribute' src/main/java/ch/sponsorplatz/ \
  | grep -E '(service|repository)\.|\.get'
```

**Bestehende Views** (jeweils im fachlichen Bounded-Context, z. B. `ch.sponsorplatz.organisation.OrganisationView`):

| View | Wofür |
|---|---|
| `OrganisationView` | Org-Detail/Liste (volle Felder) |
| `ProjektView` (mit nested `OrganisationKurzView`) | Projekt-Detail/Liste, inkl. Marktplatz |
| `MitgliedView` | Mitgliederliste — flacht `user.anzeigename`/`user.email` ein, **kein passwortHash** |
| `SponsoringPaketView` | Pakete einer Org/Projekt |
| `AnfrageView` | Sponsoring-Anfragen — `paketName` flach |
| `WatchlistEintragView` (mit nested `ProjektView`) | Watchlist |

**Neuer Controller? Pattern:**
```java
// FALSCH — Entity ins Model:
model.addAttribute("anfragen", anfrageService.findeEingehende(orgId));

// RICHTIG — View vor model.addAttribute:
List<SponsoringAnfrage> anfragen = anfrageService.findeEingehende(orgId);
model.addAttribute("anfragen", AnfrageView.von(anfragen));
```

**Neue Entity → neuer View:**
1. Java-`record` neben der Entity im selben fachlichen Package (z. B. `src/main/java/ch/sponsorplatz/organisation/<Entity>View.java`)
2. Statische `von(entity)` und `von(List<entity>)`-Methoden
3. **Mapping-Test** `<Entity>ViewTest` mit Test-ID `VIEW-NN` in `specs/TESTSTRATEGIE.md`
4. Niemals Felder ins View packen, die nicht auf einer Detail-/Liste-Seite gerendert werden (Defense in depth — z. B. nie `passwortHash`, `verifikationsToken`)

**Templates** sprechen ausschliesslich View-Properties an, niemals JPA-Relationen wie `${m.user.email}`. Bei nested Daten: View flachet ein (`${m.userEmail}`) oder hält nested-Record (`${e.projekt.name}`).

### Test-Konventionen

- Naming: `<Klasse>Test` (Unit/Web/Repo) bzw. `<Klasse>IT` (Integration)
- Test-IDs nach Schema `<Bereich>-<Nummer>` in `specs/TESTSTRATEGIE.md` pflegen
- Jede Spec-Anforderung referenziert ihre Test-ID
- AssertJ statt Hamcrest, Mockito für Mocks
- **Jeder neue View-DTO** braucht einen `<Name>ViewTest` mit `VIEW-NN`-Test-ID

### Architektur-Verifikation (Schicht 1)

Die in CLAUDE.md und `.instructions.md` formulierten Regeln werden **automatisch** durch `ArchitekturRegelnTest` (ArchUnit) durchgesetzt — bricht eine PR die Disziplin, ist der Build rot. Test-IDs `ARCH-01..13` in `specs/TESTSTRATEGIE.md`.

**Vor jedem Refactoring:** `mvn test -Dtest=ArchitekturRegelnTest` lokal laufen lassen. Wenn eine Regel bewusst gelockert werden soll, neue Test-ID pflegen und Ausnahme dokumentieren — niemals stillschweigend ändern.

### Migrationen

- **Additiv, niemals destruktiv ändern.** Neue Spalte → Backfill → alte Spalte droppen erst in nächster V-Nummer.
- SQL kompatibel zu H2 (dev) und PostgreSQL (prod): `MODE=PostgreSQL` am H2-JDBC-URL hilft.
- Vor Deployment auf prod: gegen Prod-Snapshot im Staging testen.
- `ddl-auto=validate` in beiden Profilen — Hibernate prüft, dass Schema zur Annotation passt.

### Verzeichnisstruktur

Bounded-Context-orientiert: jedes fachliche Package hält seine eigenen
Entities, Repositories, Services, Controller und DTOs zusammen.

```
src/main/java/ch/sponsorplatz/
├── PlatformApplication.java
├── shared/                # Querschnitts-Infrastruktur, kein Domänen-State
│   ├── config/            # SecurityConfig, RateLimitFilter, ModelAttributeNames
│   ├── exception/         # NotFoundException, GlobalExceptionHandler
│   ├── util/              # SlugGenerator, TokenGenerator
│   ├── pdf/               # PdfGeneratorService
│   ├── storage/           # StorageService + Lokal/OCI-Implementierung
│   ├── mail/              # MailService (zentrale SMTP-Abstraktion)
│   └── einstellungen/     # PlattformEinstellungen (DB-Settings)
│
├── benutzer/              # AppUser, Auth, Profil, Verifizierung,
│                          # PasswortReset, Einstellungen, Seed-Runner
├── organisation/          # Organisation, Mitgliedschaft, AccessControl,
│                          # Branche, Zefix, Sponsor-Self-Service-Reg
├── projekt/               # Projekt, Sponsoring-Pakete, Watchlist,
│                          # MedienAssets, Marktplatz, Suche, Matching,
│                          # Dashboard, Sitemap
├── anfrage/               # SponsoringAnfrage + Vertrag + Rechnung +
│                          # QR-Bill + Nachrichten + Mail-Benachrichtigung
├── einladung/             # Einladung + Mail-Listener + Cleanup-Job
├── benachrichtigung/      # In-App-Glocke (NotificationService + Bell-UI)
├── audit/                 # AuditLog + DSG-Datenexport
├── backup/                # BackupService + Restore + Cloud-Upload
├── ops/                   # Ops-Dashboard, Alerts, RecentErrors,
│                          # DB/Bucket-Stats, SystemSnapshot
├── admin/                 # Admin-UI: Backlog, Mail-Settings, Verifizierung
└── home/                  # HomeController, InfoController (Impressum/DSG)

src/main/resources/
├── application*.properties     # default + dev + prod
├── db/migration/V*.sql         # Flyway
├── templates/                  # Thymeleaf
├── static/                     # CSS, Bilder
└── messages_de_CH.properties   # i18n

specs/                          # technische Specs (aktiv gehalten)
docs/                           # Konzept-Dokumente (Hintergrund)
```

**Konvention:** Neue Klassen gehören in das fachlich passende Package.
Querschnitts-Tools (Mail, PDF, Storage) bleiben in `shared/`. Wenn ein
neues Aggregat entsteht, das in keinen bestehenden Kontext passt, lege
ein eigenes Top-Level-Package an statt es in `shared/` abzuladen.

---

## Häufige Befehle

```bash
# Lokal entwickeln
mvn spring-boot:run                              # → http://localhost:8080

# Tests
mvn test                                         # alle
mvn test -Dtest=OrganisationServiceTest          # einzeln

# Build
mvn clean package
mvn clean verify                                 # inkl. JaCoCo-Coverage

# Docker
docker compose up -d postgres mailhog            # nur Backing-Services
docker compose --profile app up --build          # alles inkl. App

# H2-DB-Reset bei Migration-Konflikten in dev
rm -rf data/
```

---

## Wo finde ich was

| Suchen | Datei |
|---|---|
| **Architektur-Entscheidungen + Warum** | [`docs/adr/README.md`](docs/adr/README.md) (ADR-Index) |
| **Architektur-Diagramme (C4)** | [`docs/architektur/README.md`](docs/architektur/README.md) — Structurizr-DSL + Auto-Render |
| Was ist Sponsorplatz | [`specs/PROJEKT_INFO.md`](specs/PROJEKT_INFO.md) |
| Stack & Routen | [`specs/TECHNISCHE_SPEZIFIKATION.md`](specs/TECHNISCHE_SPEZIFIKATION.md) |
| Datenbank-Schema | [`specs/DATENMODELL.md`](specs/DATENMODELL.md) |
| Berechtigungen | [`specs/ROLLENKONZEPT.md`](specs/ROLLENKONZEPT.md) |
| Tests & IDs | [`specs/TESTSTRATEGIE.md`](specs/TESTSTRATEGIE.md) |
| Roadmap | [`specs/ROADMAP.md`](specs/ROADMAP.md) |
| TDD-Prozess + Clean Code | [`.instructions.md`](.instructions.md) |
| **Vollständiges Konzept** | [`docs/konzept.md`](docs/konzept.md) |
| **Ausführliche Roadmap** | [`docs/roadmap-detailliert.md`](docs/roadmap-detailliert.md) |
| Marketing-Strategie | [`docs/marketing.md`](docs/marketing.md) |
| Naming-Begründung | [`docs/naming.md`](docs/naming.md) |
| Pitch-Präsentation | [`docs/Pitch_Sponsorplatz.pptx`](docs/Pitch_Sponsorplatz.pptx) |
| Infrastruktur (Übersicht) | [`infra/README.md`](infra/README.md) |
| Terraform staging-free | [`infra/envs/staging-free/README.md`](infra/envs/staging-free/README.md) |
| Manuelles VM-Setup | [`infra/staging-free/README.md`](infra/staging-free/README.md) |

---

## Offene Punkte / Backlog

- Phase 0.2 (siehe oben) — als nächstes
- `target/` ist in `.gitignore`, niemals committen
- VS-Code-Configs in `.vscode/` werden bewusst committet (Team-Standard)
- Java-Upgrade auf 21 ist bereits aktiv (`pom.xml` java.version=21)
- Domain `sponsorplatz.ch` ist zu sichern (außerhalb Code, Hosting-Aufgabe)

---

## Wenn Du als Claude in einer neuen Session startest

Sag dem Benutzer:
1. „Ich habe die `CLAUDE.md` gelesen, Phase 0.1 ist erledigt, Phase 0.2 (`AppUser` + `Mitgliedschaft` + `AccessControl`) ist als nächstes dran."
2. Frage gezielt: Soll ich mit Spec-Update beginnen oder zuerst die Tests anlegen? (TDD-Disziplin halten!)
3. Lies vor jeder Spec-/Test-Änderung die zugehörige Datei in `specs/` — dort ist der aktuelle Stand.

---

**Maintainer:** Fabian Aschwanden (`fabian.aschwanden@gmail.com`)
