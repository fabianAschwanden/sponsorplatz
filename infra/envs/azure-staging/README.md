# Sponsorplatz — Azure-Staging-Modul (Terraform)

> **Phase 15.3 — Multi-Cloud-DR.** Zweite Zone neben OCI Always-Free.
> Initialer Modus: **Warm-DR** (Azure-Stack läuft, empfängt aber keinen
> Produktiv-Traffic). Architektur-Entscheidung:
> [`docs/adr/0009-multi-cloud-azure-als-dr-zone.md`](../../../docs/adr/0009-multi-cloud-azure-als-dr-zone.md).

## Architektur

```
   Internet ─→ :443 ─→ Caddy (TLS, Let's Encrypt) auf VM
                            │
                            ▼
                       App (8080)
                            │
                            ├─→ Azure Blob (Uploads + Backups, MSI-Auth)
                            └─→ Azure Postgres Flexible Server (VNet-privat)
```

Komponenten:
- **VM** Standard_B2s mit Ubuntu 24.04 + cloud-init (Docker + Caddy + Azure-CLI)
- **Azure Database for PostgreSQL Flexible Server** B1ms, im delegierten Subnet
- **Azure Container Registry** Basic — Image-Quelle (`docker push` aus CD)
- **Storage Account** v2 mit zwei Blob-Containern: `sponsorplatz-uploads`, `sponsorplatz-backups`
- **User-Assigned Managed Identity** an der VM — gibt `AcrPull` (Registry)
  und `Storage Blob Data Contributor` (Storage) ohne Secrets

## Kosten

Standard_B2s + Flex Postgres B1ms + Storage + ACR Basic ≈ **CHF 50–80/Monat**
in `switzerlandnorth`. **Nicht Free-Tier.** Vor `terraform apply` Budget
freigeben.

## Voraussetzungen

```bash
# Lokal
brew install terraform azure-cli
az login                                  # Browser-Flow

# Subscription + Tenant ermitteln
az account show --query '{subscriptionId:id, tenantId:tenantId}'
```

Optional: SSH-Key generieren falls noch nicht vorhanden.
```bash
ssh-keygen -t ed25519 -f ~/.ssh/sponsorplatz-azure
```

## Erstinbetriebnahme

```bash
cd infra/envs/azure-staging
cp terraform.tfvars.example terraform.tfvars
# terraform.tfvars editieren — subscription_id, tenant_id, ssh_public_key,
# db_password, admin_password setzen.

terraform init
terraform plan -out plan.tfplan
terraform apply plan.tfplan
```

Nach erfolgreichem Apply:
```bash
terraform output                          # zeigt vm_public_ip, acr_login_server etc.

# DNS-A-Record für var.domain → terraform output vm_public_ip setzen
# (Cloudflare Dashboard, manuell — Slice 5 automatisiert das)

# Initialer Image-Push:
ACR=$(terraform output -raw acr_login_server)
az acr login --name "$ACR"
docker tag sponsorplatz:latest "$ACR/sponsorplatz:azure-staging-latest"
docker push "$ACR/sponsorplatz:azure-staging-latest"

# SSH zur VM — `pull-app`-Alias zieht das neue Image
ssh sponsoradmin@$(terraform output -raw vm_public_ip)
pull-app
```

## Wichtige Details

### Postgres-VNet-Integration

Der Flexible Server hat **keine** öffentliche IP — er ist nur aus dem VNet
erreichbar (delegiertes Subnet + Private DNS Zone). Die App-VM löst den
FQDN automatisch privat auf.

Für lokales DB-Tooling (psql, pgAdmin) gibt es zwei Wege:
1. SSH-Tunnel über die VM: `ssh -L 5432:<flex-fqdn>:5432 sponsoradmin@<vm-ip>`
2. Bastion-Host hinzufügen (out-of-scope für Phase 15.3)

### ACR-Login via Managed Identity

Auf der VM läuft ein 2h-Cron (`/etc/cron.d/sponsorplatz-acr`), der
`az acr login` per MSI auffrischt. ACR-Tokens laufen alle 3h ab, daher
das engere Refresh-Intervall.

Für CD-Pushes aus GitHub Actions wird ein **Service Principal** mit
`AcrPush`-Rolle gebraucht — das ist nicht Teil dieses Moduls (kommt in
Slice 4, CD-Workflow).

### Storage-Account-Naming

Storage-Account-Namen müssen global eindeutig sein und nur lowercase
alphanumerisch (max 24 Zeichen). Das Modul hängt 4 zufällige Ziffern an:
`sponsorplatzazurestaging<XXXX>` (~22 Zeichen).

### State-Backend

Phase-1 nutzt **lokales State-Backend**. Sobald der Storage-Account selbst
existiert, ist Migration auf `azurerm` backend möglich:

```bash
# Container für TF-State im Storage-Account anlegen
STORAGE=$(terraform output -raw storage_account_name)
az storage container create --account-name "$STORAGE" --name tfstate \
  --auth-mode login

# In backend.tf den Block austauschen:
#   backend "azurerm" {
#     resource_group_name  = "sponsorplatz-azure-staging-rg"
#     storage_account_name = "<aus terraform output>"
#     container_name       = "tfstate"
#     key                  = "azure-staging.tfstate"
#   }

terraform init -migrate-state
```

## CD via GitHub Actions

`cd-azure-staging.yml` triggert nach jedem grünen CI-Run auf `main`:
1. Image bauen → Trivy-Scan
2. Push nach ACR (`<acr>.azurecr.io/sponsorplatz:<sha>` + `:azure-staging-latest`)
3. SSH auf VM → `docker compose pull && up -d --force-recreate` für den App-Service

**Erforderliche GitHub-Variablen** (Settings → Secrets and variables → Actions):

| Type | Name | Wert |
|---|---|---|
| Variable | `AZURE_VM_IP` | Public IP der Azure-VM (`terraform output vm_public_ip`) |
| Variable | `AZURE_ACR_LOGIN_SERVER` | ACR-FQDN (`terraform output acr_login_server`, z.B. `sponsorplatzazurestagingacr.azurecr.io`) |
| Variable | `AZURE_STAGING_DOMAIN` *(optional)* | DNS-Name für TLS-Smoke (z.B. `azure-staging.sponsorplatz.ch`). Fehlt → Smoke gegen `http://<vm-ip>/login`. |
| Secret | `AZURE_SP_CLIENT_ID` | Service-Principal-AppID (siehe SP-Setup unten) |
| Secret | `AZURE_SP_CLIENT_SECRET` | Service-Principal-Secret |
| Secret | `AZURE_TENANT_ID` | Azure-AD-Tenant-ID |
| Secret | `AZURE_SUBSCRIPTION_ID` | Subscription-ID |
| Secret | `AZURE_VM_SSH_PRIVATE_KEY` | Private SSH-Key für `sponsoradmin@<vm-ip>` |
| Secret | `SPONSORPLATZ_API_KEY` *(optional)* | wie bei OCI: aktiviert REST-API; ohne Secret bleibt API auf 503 |

### Service-Principal für CD-Push einmalig anlegen

CD pusht zur ACR — die VM-Managed-Identity hat nur `AcrPull` (kein Push).
Ein separater SP mit minimaler `AcrPush`-Rolle deckt den Schreibpfad ab:

```bash
SUB=$(terraform output -raw subscription_id 2>/dev/null || az account show --query id -o tsv)
ACR_ID=$(az acr show --name "$(terraform output -raw acr_login_server | cut -d. -f1)" \
  --query id -o tsv)

az ad sp create-for-rbac \
  --name sponsorplatz-cd-azure-staging \
  --role AcrPush \
  --scopes "$ACR_ID" \
  --years 1
# Output: { "appId": "...", "password": "...", "tenant": "..." }
```

Die 3 Werte aus dem Output (zusammen mit der Subscription-ID) als die 4 Secrets
oben hinterlegen. Das Secret ist 1 Jahr gültig — Rotation via
`az ad sp credential reset --id <appId>`.

## Rollback (vorheriger Image-Tag)

Analog zur OCI-Seite, siehe Detail-Schritte in
[`infra/staging-free/README.md`](../../staging-free/README.md#rollback-vorheriger-image-tag).

Unterschiede für Azure:
- SSH-User ist `sponsoradmin@${AZURE_VM_IP}` statt `opc@<OCI-VM-IP>`
- Image-URL aus dem ACR statt GHCR:
  `${AZURE_ACR_LOGIN_SERVER}/sponsorplatz:<sha>`
- Auf der VM muss vor dem `docker compose pull` `sponsorplatz-acr-refresh`
  laufen (MSI-Token kann abgelaufen sein):
  ```bash
  ssh sponsoradmin@<AZURE_VM_IP> 'sudo /usr/local/bin/sponsorplatz-acr-refresh'
  ```

Sentry-Release-Tagging läuft identisch — siehe Sentry-Abschnitt in der
OCI-README.

OIDC Google-Login ebenfalls identisch zu OCI — siehe
[`infra/staging-free/README.md`](../../staging-free/README.md#oidc-google-login-cd-managed).
Compose-Patch-Helper auf laufende VM (Azure-Key wird automatisch aus
`~/.ssh/sponsorplatz-azure` genutzt, override via `--key <path>`):

```bash
./infra/scripts/patch-vm-compose-envs.sh sponsoradmin@<AZURE_VM_IP> --restart
```

## Cleanup

```bash
terraform destroy
```

⚠️ **Achtung:** Storage-Account hat `delete_retention_policy = 30d`. Blobs
sind nach Destroy noch 30 Tage soft-deleted und kostenwirksam. Für sofortige
Löschung: erst Blobs purgen, dann destroy.

## Querverweise

- [`infra/staging-free/README.md`](../../staging-free/README.md) — bestehender OCI-Stack
- [`infra/envs/staging-free/`](../staging-free/) — analoges Terraform-Modul für OCI
- [`docs/adr/0009-multi-cloud-azure-als-dr-zone.md`](../../../docs/adr/0009-multi-cloud-azure-als-dr-zone.md)
- [`specs/ROADMAP.md`](../../../specs/ROADMAP.md) — Phase 15.3 Slice-Plan
