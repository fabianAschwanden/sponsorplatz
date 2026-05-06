# Infrastructure as Code — Sponsorplatz

OCI-Always-Free-VM mit Docker-Compose-Stack (Caddy + App + Postgres) + Object-Storage-Buckets für Uploads + Backups.

## Zwei Wege zur VM

| Pfad | Wann | Eintrittspunkt |
|---|---|---|
| **Manuell** (Phase 1) | erstes Setup ohne IaC, Debugging, lokale Dev | [`staging-free/`](staging-free/) — Caddyfile + docker-compose.yml + README mit Schritt-für-Schritt-Anleitung |
| **Terraform** (Phase 3) | reproduzierbares Setup, mehrere Envs, Pipeline-CD | [`envs/staging-free/`](envs/staging-free/) |

Der Terraform-Pfad ist die langfristige Quelle der Wahrheit. Der manuelle Pfad bleibt als Fallback dokumentiert (für Tests in einer fremden Tenancy oder bei Terraform-Ausfall).

## Verzeichnisstruktur

```
infra/
├── README.md                       # diese Datei
├── modules/
│   └── storage/                    # Object-Storage-Buckets (uploads + backups)
├── envs/
│   └── staging-free/               # Always-Free-VM via Terraform
└── shared/
    └── bootstrap/                  # einmaliger State-Bucket-Setup (lokaler Backend-State)
```

(Plus der manuelle Pfad `staging-free/` mit Caddyfile + docker-compose.yml + README.)

## Erstmaliger Setup

Siehe [`shared/bootstrap/README.md`](shared/bootstrap/README.md) — erzeugt State-Buckets + CICD-Group.

Anschliessend [`envs/staging-free/README.md`](envs/staging-free/README.md) — Apply für die VM + Stack.

## Konventionen

- Tags an allen Ressourcen: `project=sponsorplatz`, `environment=<env>`, `managed_by=terraform`
- Ressourcennamen: `sponsorplatz-<art>-<env>`
- `terraform fmt` vor jedem Commit
- `terraform.tfvars` und `*.tfstate` werden NIE committet (siehe `.gitignore`)
- Module nur über Inputs konfigurieren — keine Hardcodings im Modul-Code

## Deploy

CI grün auf `main` → [`.github/workflows/cd-staging-free.yml`](../.github/workflows/cd-staging-free.yml) baut Image, scant via Trivy, pusht nach OCIR, SSH auf VM für `docker compose pull && up -d`.

Terraform-Apply ist NICHT Teil der CD-Pipeline — Infrastruktur-Änderungen werden manuell ausgerollt, App-Updates rollend.
