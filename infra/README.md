# Infrastructure as Code — Sponsorplatz

Multi-Cloud-Setup seit Phase 15.3 ([ADR-0009](../docs/adr/0009-multi-cloud-azure-als-dr-zone.md)) —
zwei parallele Zonen mit eigenen Pipelines und eigenem State.

## Zonen

| Zone | Provider / Region | Rolle | Hostname | Spring-Profil |
|---|---|---|---|---|
| **Primary** | OCI Always-Free, eu-zurich-1 | produktiv | `sponsorplatz.for-better.biz` | `cloud-free` |
| **Warm-DR** | Azure, Sweden Central | Backup-Zone, kein Live-Traffic | `sponsorplatz.for-the.biz` | `cloud-azure` |

Beide Zonen sind **provider-getrennt**: eigene VM, eigene DB
(Postgres-Docker bzw. Azure Flex), eigener Object/Blob-Storage, eigener
TF-State. Cross-Cloud-Sync läuft heute manuell über `/admin/backups` +
`/admin/datei-backups` (DB-Dump + ZIP-File-Backup); Slices 5–7 der
Phase 15.3 automatisieren das später.

## Pfade pro Zone

| Zone | Manueller Pfad | Terraform-Pfad |
|---|---|---|
| OCI | [`staging-free/`](staging-free/) — Caddyfile, docker-compose.yml, README mit Schritt-für-Schritt-Anleitung | [`envs/staging-free/`](envs/staging-free/) — VCN + VM + Buckets + IAM |
| Azure | – (kein manueller Pfad, weil zu viele Ressourcen) | [`envs/azure-staging/`](envs/azure-staging/) — VNet + VM + Flex-Postgres + ACR + Blob + UAMI |

Der Terraform-Pfad ist die langfristige Quelle der Wahrheit. Der
manuelle OCI-Pfad bleibt als Fallback dokumentiert (für Tests in einer
fremden Tenancy oder bei Terraform-Ausfall).

## Verzeichnisstruktur

```
infra/
├── README.md                       # diese Datei
├── modules/
│   └── storage/                    # OCI Object-Storage-Buckets (uploads + backups)
├── envs/
│   ├── staging-free/               # OCI Always-Free-VM via Terraform
│   └── azure-staging/              # Azure-Stack via Terraform (Phase 15.3)
├── shared/
│   └── bootstrap/                  # einmaliger OCI-State-Bucket-Setup
└── staging-free/                   # manueller OCI-Pfad (Caddyfile + Compose + README)
```

## Erstmaliger Setup

### OCI

1. [`shared/bootstrap/README.md`](shared/bootstrap/README.md) — State-Buckets + CICD-Group anlegen
2. [`envs/staging-free/README.md`](envs/staging-free/README.md) — Apply für VM + Stack
3. Manuelle Alternative: [`staging-free/README.md`](staging-free/README.md)

### Azure

1. [`envs/azure-staging/README.md`](envs/azure-staging/README.md) — Voraussetzungen (`az login`, SSH-Key), Terraform-Apply, DNS-A-Record, Service-Principal für CD, GitHub-Secrets

**Kostenhinweis:** Azure ist **nicht** Free-Tier — ~CHF 50–80/Monat
für Standard_B2as_v2 + Flex Postgres B1ms + Storage + ACR Basic.
OCI bleibt Always-Free.

## Cross-Cloud-Sync

- **DB:** `pg_dump` auf OCI via `/admin/backups` → ZIP/SQL-Download →
  `/admin/backups/restore` auf Azure. Postgres-Versionen müssen
  übereinstimmen (V41+ Migration setzt Azure auf PG17 als Default).
- **Files:** `/admin/datei-backups` → ZIP-Download mit allen
  `MedienAsset`-Files → Upload auf der anderen Cloud. Provider-
  agnostisch via `StorageService.speichereBytes(...)`.
- **Audit-Trail-Schutz:** Jeder Audit-Eintrag trägt
  `audit_log.umgebung` (V41), nach Cross-Cloud-Restore bleibt sichtbar
  wo der Eintrag ursprünglich entstand ([ADR-0010](../docs/adr/0010-umgebung-marker-im-audit-log.md)).

## Konventionen

- Tags an allen Ressourcen: `project=sponsorplatz`, `environment=<env>`, `managed_by=terraform`
- Ressourcennamen: `sponsorplatz-<art>-<env>`
- `terraform fmt` vor jedem Commit
- `terraform.tfvars` und `*.tfstate*` werden NIE committet (siehe `.gitignore` pro Env)
- Module nur über Inputs konfigurieren — keine Hardcodings im Modul-Code
- Beim Tear-Down zuerst manuell `cloud-init.rendered.yaml` löschen
  (enthält Klartext-Passwörter aus dem Templating-Output)

## Deploy

Beide Pipelines triggern auf `workflow_run` nach grünem CI auf `main`:

| Workflow | Ziel | Image-Registry |
|---|---|---|
| [`cd-staging-free.yml`](../.github/workflows/cd-staging-free.yml) | OCI-VM | OCIR (`zrh.ocir.io`) |
| [`cd-azure-staging.yml`](../.github/workflows/cd-azure-staging.yml) | Azure-VM | ACR (`<name>.azurecr.io`) |

Beide bauen das Image einmal, scannen via Trivy, pushen zum jeweiligen
Registry und SSH-deployen mit `docker compose pull && up -d --force-recreate app`.

**Terraform-Apply ist NICHT Teil der CD-Pipeline** — Infrastruktur-
Änderungen werden manuell ausgerollt, App-Updates rollend.
