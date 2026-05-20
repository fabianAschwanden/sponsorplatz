# Technische Spezifikation

## Architektur-Prinzipien (verbindlich)

- **Layering**: `Controller → Service → Repository`. Controller halten keine Business-Logik; Services sind `@Transactional`.
- **DTO-Pflicht am Web-Layer**: Templates erhalten ausschliesslich Records aus `ch.sponsorplatz.dto.*View` oder `*FormDto` — niemals JPA-Entities (verhindert LazyInitializationException + Persistenz/Präsentations-Kopplung). Konvention + Beispiele: siehe [`CLAUDE.md`](../CLAUDE.md#view-dto-pflicht-entities-verlassen-service-layer-nicht).
- **Exception-Mapping zentral** in `GlobalExceptionHandler` → gerendertes `error.html` mit korrektem HTTP-Status (`NotFoundException`→404, `IllegalArgumentException`→400, `IllegalStateException`→409, `AccessDeniedException`→403).
- **Mass-Assignment-Defense**: Update-Pfade identifizieren Ressourcen über URL-Path-Variable, niemals über Body-`id` (siehe Org-Update-Pattern: `POST /organisationen/{slug}` mit AccessControl-Check, `OrganisationFormDto` ohne `id`-Feld).
- **AccessControl** ist die einzige Stelle für Org-Rollen-Checks. Controller rufen `accessControl.kannOrg…NachSlug(slug, auth)` programmatisch auf, werfen `AccessDeniedException` bei `false`.

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

### `cloud-free` (OCI Always-Free-VM)

- Erbt von `prod` (via `spring.profiles.group.cloud-free=prod`)
- Tomcat-Threadpool + HikariCP klein gehalten (1 GB RAM)
- Mail über externen SMTP-Relay (Mailgun/SES)
- Forwarded-Headers für Caddy-Reverse-Proxy
- `STORAGE_PROVIDER=oci` aktiviert OCI Object Storage (siehe Storage-Abschnitt)

### `cloud-azure` (Azure-Staging, Phase 15.3 — Warm-DR)

- Erbt von `prod` (via `spring.profiles.group.cloud-azure=prod`)
- DB extern: **Azure Database for PostgreSQL Flexible Server** im delegierten VNet — Connection-String über `DB_URL`, kein lokaler Postgres-Container
- HikariCP-Pool auf das `B1ms`-Connection-Limit gekürzt
- Storage über **Azure Blob** mit User-Assigned Managed Identity (`AZURE_AUTH_MODE=managed-identity`), Fallback `connection-string` für lokales Testen gegen Azurite
- Image-Quelle: Azure Container Registry (Pull via MSI, kein Admin-User)

## Infrastruktur

Vollständige IaC-Doku: [`infra/README.md`](../infra/README.md). Zwei Cloud-Zonen, drei Pfade:

| Pfad | Wann | Doku |
|---|---|---|
| Manuell (OCI) | Erst-Setup, Debugging | [`infra/staging-free/README.md`](../infra/staging-free/README.md) — OCI-VM mit der Hand bootstrappen |
| Terraform (OCI) | Reproduzierbar | [`infra/envs/staging-free/README.md`](../infra/envs/staging-free/README.md) — VCN + VM + Buckets + IAM |
| Terraform (Azure) | Zweite Zone — Warm-DR | [`infra/envs/azure-staging/README.md`](../infra/envs/azure-staging/README.md) — VNet + VM + Flex-Postgres + ACR + Blob + MSI |

Beim Terraform-Pfad bootstrappt `cloud-init.yaml.tftpl` Docker, schreibt `docker-compose.yml` + `Caddyfile` ins `/opt/sponsorplatz/`-Verzeichnis und startet den Stack via systemd. Spätere App-Updates kommen über die CD-Pipeline (`docker compose pull && up -d`), nicht über Terraform.

**Auth zwischen App-VM und Cloud-Storage:**
- **OCI:** Instance Principal — VM ist Mitglied der Dynamic Group `sponsorplatz-vm-staging-free`, die per IAM-Policy Zugriff auf die Buckets hat
- **Azure:** User-Assigned Managed Identity — `AcrPull`-Rolle auf den Container Registry + `Storage Blob Data Contributor`-Rolle auf den Storage Account. Cron `/usr/local/bin/sponsorplatz-acr-refresh` auf der VM frischt das ACR-Token alle 2h via MSI auf.

Keine API-Keys auf den VMs.

## Storage

`StorageService` ist die einzige Schnittstelle für Datei-Uploads. Provider-Auswahl per Property:

| `sponsorplatz.storage.provider` | Implementierung | Aktiv in |
|---|---|---|
| `lokal` (default) | `LokalerStorageService` — Dateisystem unter `sponsorplatz.storage.lokal.basis-pfad` | dev, test, prod-onprem |
| `oci` | `OciStorageService` — OCI Object Storage über `oci-java-sdk-objectstorage` | cloud-free |
| `azure` | `AzureBlobStorageService` — Azure Blob Storage über `azure-storage-blob` | cloud-azure |

Aktivierung über `@ConditionalOnProperty` — pro Provider darf nur ein Bean existieren.

**Methoden:**
- `speichere(MultipartFile, zielpfad)` — Upload-Pfad (User-Forms)
- `speichereBytes(byte[], contentType, zielpfad)` — Restore-Pfad (Datei-Backup)
- `loesche(storagePfad)` — Soft-Löschung
- `ladeAlsResource(storagePfad)` — Auslieferung als HTTP-Resource. Wirft `StorageObjectNotFoundException` (typed) bei 404, damit der Controller einen sauberen HTTP-404 statt 500-Stacktrace liefern kann (siehe `MedienController.ausliefern`).

### OCI-Auth (nur wenn `provider=oci`)

| `sponsorplatz.storage.oci.auth-mode` | Provider | Wofür |
|---|---|---|
| `instance` (default) | `InstancePrincipalsAuthenticationDetailsProvider` | OCI-VMs ohne Credentials — Dynamic Group + Policy |
| `config` | `ConfigFileAuthenticationDetailsProvider` | Lokales Testen mit `~/.oci/config` |

### Azure-Auth (nur wenn `provider=azure`)

| `sponsorplatz.storage.azure.auth-mode` | Provider | Wofür |
|---|---|---|
| `managed-identity` (default) | `DefaultAzureCredentialBuilder` mit UAMI-Client-ID | Azure-VM ohne Credentials — Managed Identity an VM gehängt |
| `connection-string` | Connection-String aus `sponsorplatz.storage.azure.connection-string` | Lokales Testen gegen Azurite-Emulator oder Storage-Account-Key |

Für die Test-Bypass-Problematik (Azure-SDK-Klassen sind `final` → mit subclass-Mockito nicht stubbar) gibt es eine schmale `AzureBlobOperations`-Adapter-Schnittstelle. Die Service-Schicht wirft die package-eigenen `AzureBlobNotFoundException` + `AzureBlobOperationException`, kein Azure-SDK-Typ leakt über den Adapter hinaus.

Buckets/Container werden **nicht** vom Code angelegt — Erstellung + Versioning + Lifecycle-Rules sind Infra-Verantwortung (Terraform-Module `infra/envs/staging-free/` + `infra/envs/azure-staging/`).

## Backups

Zwei parallele Backup-Pfade — DB und Datei-Uploads, beide unabhängig konfiguriert.

### DB-Backup (`BackupService` + `BackupRestoreService`)

`BackupService` erstellt täglich (`@Scheduled cron=0 0 2 * * *`) einen DB-Dump:
- H2 (dev): `SCRIPT TO`
- PostgreSQL (prod/cloud-free/cloud-azure): `pg_dump`

Wenn ein optionaler `BackupCloudUploader` im Context registriert ist (`OciBackupCloudUploader` bei `provider=oci`, `AzureBackupCloudUploader` bei `provider=azure`), wird das lokale Backup zusätzlich in einen Cloud-Bucket hochgeladen. Upload-Fehler eskalieren **nicht** — das lokale Backup gilt als primärer Schutzpfad.

`BackupRestoreService.restore(byte[], ausgefuehrtVon)` spielt einen Dump aus dem Admin-UI zurück (`POST /admin/backups/restore`, RESTORE-Bestätigung pflicht).

### Datei-Backup (`DateiBackupService` + `DateiBackupRestoreService`, Phase 15.4)

Parallel zum DB-Backup: alle in `MedienAsset` referenzierten Storage-Objekte werden in ein **ZIP-Archiv** gepackt (`sponsorplatz_uploads_<ts>.zip`), in dem der ZIP-Entry-Name dem Storage-Pfad entspricht. Damit ist das Archiv cross-cloud transportabel (OCI ↔ Azure).

- `DateiBackupService.erstelleDateiBackup()` walkt das `MedienAssetRepository`, streamt jede Datei via `StorageService.ladeAlsResource(...)` in den `ZipOutputStream`. Orphaned Records (Storage-404) werden geloggt + im Audit-Detail dokumentiert, der Backup-Lauf bricht nicht ab.
- `DateiBackupRestoreService.restore(byte[], ausgefuehrtVon)` liest jeden ZIP-Entry, prüft Path-Traversal, leitet Content-Type aus Datei-Endung ab (`URLConnection.guessContentTypeFromName`) und ruft `StorageService.speichereBytes(...)`. Bestehende Storage-Objekte werden überschrieben.
- UI: `/admin/datei-backups` (Liste + Create + Download + Delete + Restore-Form mit RESTORE-Bestätigung).
- Audit-Aktionen: `DATEI_BACKUP_ERSTELLT`, `DATEI_BACKUP_RESTORED`.

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
| GET, POST | `/passwort-vergessen` | public | Reset-Mail anfordern (kein Info-Leak) |
| GET, POST | `/passwort-reset` | public | Token-Validation + Passwort setzen |
| GET | `/verifizieren` | public | E-Mail-Verifizierung via Token |

#### Zugriffs-Regeln (gesamt — Stand Phase 11)

| Pfad-Pattern | Zugriff |
|---|---|
| `/`, `/login`, `/registrieren`, `/passwort-vergessen`, `/passwort-reset`, `/verifizieren` | `permitAll` |
| `/css/**`, `/images/**`, `/favicon.ico`, `/sitemap.xml` | `permitAll` |
| `/actuator/health`, `/actuator/info` | `permitAll` |
| `/h2-console/**` (nur dev) | `permitAll` |
| `/impressum`, `/datenschutz` | `permitAll` |
| `/oauth2/**`, `/login/oauth2/**` | `permitAll` (OIDC-Flow) |
| `/sponsor/**` | `permitAll` (Sponsor-Self-Reg) |
| `/einladung/**` | `permitAll` (Token-Auth via URL) |
| `/marktplatz/**`, `/medien/**`, `/vereine/**`, `/og/**`, `/fuer-marken`, `/marken/*/engagements` | `permitAll` |
| `/payment/webhook/**` | `permitAll` + CSRF-Ausnahme |
| `/organisationen` (GET) | `permitAll`, ergebnis-gefiltert auf Mitgliedschaften (Plattform-Admin sieht alle) |
| `/organisationen/{slug}` (GET) | `permitAll` |
| `/organisationen/neu`, `POST /organisationen` | `authenticated` |
| `/organisationen/{slug}/bearbeiten`, `/organisationen/{slug}/loeschen` | `accessControl.kannOrgEditierenNachSlug(slug)` (programmatisch) |
| `/organisationen/{slug}/mitglieder/**` | `accessControl.kannOrgVerwaltenNachSlug(slug)` |
| `/organisationen/{slug}/projekte/**` | `accessControl.kannOrgEditierenNachSlug(slug)` |
| `/organisationen/{slug}/anfragen/**` | `accessControl.kannOrgEditierenNachSlug(slug)` |
| `/admin/**` | `hasRole('PLATFORM_ADMIN')` |
| `/aufgaben`, `/aufgaben/{id}/erledigen` | `authenticated`, ergebnis nach Org-Mitgliedschaft + Platform-Rolle gefiltert |
| `/admin/aufgaben-definitionen/**` | `hasRole('PLATFORM_ADMIN')` |
| `/anfragen` | `authenticated`, ergebnis-rollenabhängig |
| `/anfragen/neu`, `/anfragen/erstellen` | `authenticated` + `kannOrgEditieren(anfragenderOrg)` |
| `/anfragen/neu-kontakt`, `/anfragen/kontakt-erstellen` | `authenticated` + `OrgTyp.VEREIN`-Whitelist |
| `/onboarding/**`, `/support`, `/dashboard`, `/einstellungen`, `/watchlist`, `/benachrichtigungen` | `authenticated` |
| alle anderen | `authenticated` |

### Anfrage-Flow (Phase 4 + Phase 11)

| Methode | Pfad | Zugriff | Beschreibung |
|---|---|---|---|
| GET | `/anfragen` | authenticated, gefiltert | Eingehende für alle eigenen Orgs; ausgehende-Sektion + Outbound-Button nur für Vereins-Mitglieder mit Edit-Recht |
| POST | `/anfragen/{id}/annehmen|ablehnen` | `kannOrgEditieren(empfaengerOrg)` | IDOR-Schutz |
| GET | `/anfragen/neu?paketId=…` | authenticated | Paket-Anfrage Sponsor → Verein (vom Marktplatz-Detail-Klick) |
| POST | `/anfragen/erstellen` | `kannOrgEditieren(anfragenderOrg)` | Empfänger immer vom Paket abgeleitet |
| GET | `/anfragen/neu-kontakt` | Vereins-Edit-Recht | Sponsor-Picker für Kontakt-Anfrage |
| POST | `/anfragen/kontakt-erstellen` | Vereins-Edit-Recht; `empfaenger.typ == UNTERNEHMEN` | Kontakt-Anfrage ohne Paket (V30) |

### Onboarding & Support (Phase 11)

| Methode | Pfad | Zugriff | Beschreibung |
|---|---|---|---|
| GET | `/onboarding` | authenticated; redirected wenn Mitgliedschaft existiert | Wizard nach erstem Login; DashboardController redirectet User ohne Mitgliedschaft hierhin |
| POST | `/onboarding/verein-erstellen` | authenticated | Schnell-Org anlegen + ORG_OWNER-Mitgliedschaft (`OrganisationService.erstelleMitEigentuemer`) |
| POST | `/onboarding/einladung-annehmen` | authenticated | Token-Whitelist-Validierung + Forward auf `/einladung/annehmen` |
| GET, POST | `/support` | authenticated | Mail-Form an `sponsorplatz.support.empfaenger` (ENV `SPONSORPLATZ_ADMIN_EMAIL`, Default `support@sponsorplatz.ch`); bei Mail-Fehler bleibt Form mit Fehlermeldung offen |

### Aufgaben-Verwaltung (Phase 12 — generische Task-Engine)

| Methode | Pfad | Zugriff | Beschreibung |
|---|---|---|---|
| GET | `/aufgaben` | authenticated | „Meine offenen Aufgaben" — Vereinigung aus Org-Aufgaben aller eigenen Org-Mitgliedschaften (jede Rolle) und PLATFORM_ADMIN-Aufgaben (wenn die Plattform-Rolle gesetzt ist). |
| POST | `/aufgaben/{id}/erledigen` | authenticated + IDOR-Check: User muss zur Sichtbarkeit der Aufgabe gehören | Manuelles Abhaken. Auto-Erledigung über `AufgabenEngine` ist der Normalfall; dieser Endpoint nur für „kann manuell weggewischt werden". |
| GET | `/admin/aufgaben-definitionen` | PLATFORM_ADMIN | Liste aller Workflow-Vorlagen mit System-Badge. |
| GET, POST | `/admin/aufgaben-definitionen/neu` | PLATFORM_ADMIN | Neue Definition anlegen — Admin pflegt Trigger-Entity-Typ + Trigger-Status + Ziel-Status + Assignee-Regel. |
| GET, POST | `/admin/aufgaben-definitionen/{id}/bearbeiten` | PLATFORM_ADMIN | Bei System-Defs ist das `<fieldset>` für Trigger-Felder gesperrt — Service-Verkabelung bleibt intakt. |
| POST | `/admin/aufgaben-definitionen/{id}/loeschen` | PLATFORM_ADMIN; System-Defs werfen `IllegalStateException` | Custom-Defs löschbar, System-Seeds nur deaktivierbar (aktiv=false). |

**Engine-Trigger** sind in den Domain-Services verdrahtet (kein eigener Endpoint) — `OrganisationService` / `SponsoringAnfrageService` / `VertragService` / `RechnungService` rufen `AufgabenEngine.on<Entity>StatusWechsel(entity)` an jeder Status-Setz-Stelle. Engine ist idempotent (existsByDefinitionIdAndEntityIdAndStatus-Guard).

**Sichtbarkeit**: Eine Aufgabe ist sichtbar wenn entweder `assignee_org_id` zu einer Org-Mitgliedschaft des Users gehört (jede Rolle reicht — der Vorstand ist oft VIEWER und braucht trotzdem Reporting), oder `nur_platform_admin=true` und der User PLATFORM_ADMIN ist.

### Medien & Datei-Anhänge (Phase 11)

| Methode | Pfad | Zugriff | Beschreibung |
|---|---|---|---|
| GET | `/medien/{id}` | public | Bilder inline, Dokumente als Attachment-Download (`ContentDisposition.builder()`, RFC-5987-encoded Filename) |
| POST | `/organisationen/{orgSlug}/projekte/{projektSlug}/medien` | `kannOrgEditierenNachSlug` | Upload Bild oder Anhang. Bilder JPEG/PNG/WebP max 5 MB ODER Dokumente PDF/PPTX/DOCX/XLSX/PPT/DOC/XLS max 20 MB, max 10 Assets pro Entity |
| POST | `/organisationen/{slug}/medien` | `kannOrgEditierenNachSlug` | Org-Logo/Cover-Upload |
| POST | `/medien/{id}/loeschen` | typabhängig | ORGANISATION → Org-Edit; PROJEKT → `kannOrgEditieren(p.org)`; USER → `entityId == eigene UserId` |

### Volltextsuche

`VolltextSucheService` routet je nach DB-Dialekt:
- **PostgreSQL** (prod): `tsvector` + GIN-Index (V22, Postgres-only) für Stemming auf Deutsch
- **H2** (dev/test): JPQL-LIKE-Fallback in `ProjektRepository.sucheOeffentliche` (alle Spalten lower-case)

Alle Projekt-Listen-Queries (`findBySlug`, `findByOrgIdOrderByCreatedAtDesc`, `findBySichtbarkeitOrderByVeroeffentlichtAmDesc`, `sucheOeffentliche`, `findePassende`) hängen `JOIN FETCH p.org` dran — `ProjektView.von(p)` greift auf `p.getOrg().getName/getSlug` zu, sonst LazyInit unter `spring.jpa.open-in-view=false`.

## Verzeichnisstruktur

Bounded-Context-orientiert (jeder fachliche Bereich enthält Entity + Repository + Service + Controller + DTOs):

```
src/main/java/ch/sponsorplatz/
├── PlatformApplication.java
├── shared/                # Querschnitts-Infrastruktur, kein Domänen-State
│   ├── config/            # SecurityConfig, LocaleConfig, RateLimitFilter, ModelAttributeNames
│   ├── exception/         # NotFoundException, GlobalExceptionHandler
│   ├── util/              # SlugGenerator, TokenGenerator
│   ├── pdf/               # PdfGeneratorService
│   ├── storage/           # StorageService + Lokal/OCI-Implementierung
│   ├── mail/              # MailService (zentrale SMTP-Abstraktion)
│   └── einstellungen/     # PlattformEinstellungen (DB-Settings)
│
├── benutzer/              # AppUser, OnboardingController, SupportController, Auth, Profil,
│                          # Verifizierung, PasswortReset, Einstellungen, OIDC-Mapping (FederierteIdentitaet),
│                          # SeedRunner (Dev/Demo/Prod-Admin)
├── organisation/          # Organisation, Mitgliedschaft, AccessControl,
│                          # Branche, SponsorBranche, OrgHierarchieService, Zefix,
│                          # Sponsor-Self-Service-Reg
├── projekt/               # Projekt, SponsoringPaket, Watchlist, MedienAsset (inkl. ANHANG-Typ),
│                          # Marktplatz, Suche, Matching, Dashboard, Sitemap, Event
├── anfrage/               # SponsoringAnfrage (Paket + Kontakt-Anfrage ab V30),
│                          # Vertrag, Rechnung, Nachricht, BenachrichtigungsService,
│                          # PaymentProvider (Webhook + StubProvider)
├── einladung/             # Einladung + Mail-Listener + Cleanup-Job
├── benachrichtigung/      # In-App-Glocke (NotificationService + Bell-UI)
├── aufgabe/               # Customizable Task-Engine: Aufgabe + AufgabenDefinition,
│                          # AufgabenEngine (Status-Wechsel-Listener), AssigneeKontext,
│                          # AufgabenController (/aufgaben),
│                          # AdminAufgabenDefinitionController (/admin/aufgaben-definitionen)
├── audit/                 # AuditLog + DSG-Datenexport
├── backup/                # BackupService + Restore + Cloud-Upload
├── ops/                   # Ops-Dashboard, Alerts, RecentErrors, DB/Bucket-Stats
├── admin/                 # Admin-UI: Backlog, Mail-Settings, Verifizierung,
│                          # AdminBenachrichtigungService (Push an PLATFORM_ADMINs bei neuer Org-Reg)
└── home/                  # HomeController, InfoController (Impressum/DSG)

src/main/resources/
├── application*.properties               # default + dev + prod + cloud-free + cloud-azure
├── db/migration/V*.sql                   # Flyway (V1..V42)
├── templates/                            # ~50 Thymeleaf-Templates (DE/FR/IT/EN i18n)
├── static/                               # CSS, Bilder
└── messages_{de_CH,fr_CH,it_CH,en}.properties   # i18n-Bundles, ~600 Keys

specs/                                    # technische Specs (aktiv gehalten)
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
- Bundles: `messages_de_CH.properties` (Quelle), `messages_fr_CH.properties`, `messages_it_CH.properties`, `messages_en.properties` (~600 Keys)
- CHF-Format `1'234.50`
- Datum `dd.MM.yyyy`
- Zeitzone `Europe/Zurich`
- `LocaleConfig` mappt URL-Param `?lang=de|fr|it|en` auf konkrete Locales (`de_CH`/`fr_CH`/`it_CH`/`en`) — Whitelist + Country-Suffix sind beide notwendig, sonst löst Spring auf das (unvollständige) Default-Bundle auf. Werte ausserhalb der Whitelist → Cookie zurückgesetzt → Default-Locale.
- Cookie-Name `lang`, 365 Tage gültig.
- Templates verwenden `#{key}`-Resolution; ~50 Templates konvertiert. Marketing-Copy-Translations sind ein erster Wurf — sollten von Native-Speakern reviewt werden vor Pilot-Launch.

## Cloud-Deployment

**Zwei Zonen parallel** seit Phase 15.3 — Architektur-Entscheidung in
[`docs/adr/0009-multi-cloud-azure-als-dr-zone.md`](../docs/adr/0009-multi-cloud-azure-als-dr-zone.md):

| Zone | Rolle | Hostname | Profil |
|---|---|---|---|
| **OCI Always-Free** (eu-zurich-1) | Primary | `sponsorplatz.for-better.biz` | `cloud-free` |
| **Azure Sweden Central** | Warm-DR | `sponsorplatz.for-the.biz` | `cloud-azure` |

Beide Stacks:
- Eigene CD-Pipeline (`.github/workflows/cd-staging-free.yml` + `cd-azure-staging.yml`), parallel-unabhängig — Failure auf einer Cloud blockiert die andere nicht.
- Eigene DB + eigener Object/Blob-Storage. DB-Sync OCI→Azure heute via manuellen `pg_dump`-Restore über `/admin/backups`, Files via ZIP-Restore über `/admin/datei-backups`.
- Audit-Einträge tragen `umgebung`-Marker (`oci-staging-free` / `azure-staging`), Sentry-Events tragen Tag `sponsorplatz.umgebung` — Cross-Cloud-Provenienz ist im UI + Dashboard sichtbar.

Status der Multi-Cloud-Slices: siehe [`specs/ROADMAP.md`](ROADMAP.md) §15.3 — App-Schicht + Terraform + CD ✓, DNS-Failover via Cloudflare + automatische Cross-Replication + beidseitiger Smoke noch offen (Slices 5–7).
