# `staging-free` — Terraform-managed OCI Always-Free-Stack

Vollautomatischer Stack für die Always-Free-VM (E2.1.Micro):
VCN + Public Subnet + Internet Gateway + Security List + VM (mit Cloud-Init für Docker + Compose-Stack) + Object-Storage-Buckets (uploads + backups) + IAM (Dynamic Group + Policies für Instance Principal).

## Voraussetzungen

- Bootstrap einmal gelaufen (`infra/shared/bootstrap`) — State-Buckets existieren
- OCI-CLI lokal konfiguriert: `~/.oci/config` mit `[DEFAULT]`-Profil
- Customer-Secret-Key generiert (Identity Domain → Users → My Profile → Customer Secret Keys) für den S3-kompatiblen Backend-Zugriff

## Lokal verwalten

```bash
cd infra/envs/staging-free

cp terraform.tfvars.example terraform.tfvars
# → tenancy_ocid, compartment_ocid, region, namespace, AD, ssh_public_key,
#   domain, acme_email, db_password, admin_email, admin_password einsetzen

# S3-Backend gegen OCI Object Storage (für Remote-State)
export AWS_ACCESS_KEY_ID="<oci customer-secret-key-id>"
export AWS_SECRET_ACCESS_KEY="<oci customer-secret>"
# Endpoint kommt aus dem Bootstrap-Output `tfstate_backend_endpoint`
export AWS_S3_ENDPOINT="https://<NS>.compat.objectstorage.eu-zurich-1.oraclecloud.com"

terraform init
terraform plan
terraform apply
```

## Was wird angelegt

| Resource | Name | Zweck |
|---|---|---|
| VCN | `sponsorplatz-vcn-staging-free` | 10.40.0.0/16 |
| Subnet | `sponsorplatz-subnet-staging-free` | Public, 10.40.1.0/24 |
| Security List | `sponsorplatz-sl-staging-free` | 22, 80, 443 ingress; egress all |
| VM | `sponsorplatz-vm-staging-free` | Always-Free E2.1.Micro |
| Bucket | `sponsorplatz-uploads-staging-free` | Medien-Assets |
| Bucket | `sponsorplatz-backups-staging-free` | DB-Dumps (90-Tage-Archive, 365-Tage-Delete) |
| Dynamic Group | `sponsorplatz-vm-staging-free` | Match: `instance.compartment.id` |
| Policy | `sponsorplatz-vm-storage-staging-free` | Bucket-Access via Instance Principal |
| Policy | `sponsorplatz-os-lifecycle-staging-free` | OCI-Service-Principal für Lifecycle-Rules |

## Erste Inbetriebnahme

1. **Apply ausführen** — Terraform liefert `vm_public_ip`, `app_url_fallback`, `bucket_names`
2. **DNS-A-Record** auf `vm_public_ip` setzen für `var.domain` (oder Fallback `app_url_fallback` benutzen, der via nip.io-Wildcard ohne DNS funktioniert)
3. **Cloud-Init abwarten** — auf der VM `tail -f /var/log/cloud-init-output.log`. Erster Start dauert 5–10 Minuten (Docker-Install + Image-Pull).
4. **Health-Check:** `curl -fsS https://${var.domain}/actuator/health`
5. **Initial-Login:** `${admin_email}` / `${admin_password}` → Passwort sofort ändern.
6. **GitHub-Variablen setzen** (für CD-Pipeline):
   - `STAGING_FREE_VM_IP` ← Output `vm_public_ip_for_github`
   - `OCIR_NAMESPACE` ← Output `namespace_for_github`
   - `OCIR_USERNAME` ← `var.ocir_username`
   - Secret `OCIR_AUTH_TOKEN` ← `var.ocir_auth_token`
   - Secret `STAGING_FREE_SSH_PRIVATE_KEY` ← Private-Key des `var.ssh_public_key`

## Update-Workflow

| Was | Wer | Wie |
|---|---|---|
| App-Image | CD-Pipeline | `cd-staging-free.yml` → `docker compose pull && up -d` |
| Compose-Konfig | manuell | `terraform apply` rendert neu, aber `lifecycle.ignore_changes` auf `metadata["user_data"]` verhindert VM-Replace — Konfig also via SSH editieren oder VM gezielt neu provisionieren |
| Secrets-Rotation (DB-PW) | manuell | `tfvars` ändern → SSH auf VM → `docker compose down -v && up -d` (DB-Volume verlieren!) ODER `ALTER USER … PASSWORD …` direkt in Postgres |
| OS-Updates | manuell | `ssh opc@<ip> sudo dnf -y upgrade && sudo reboot` |

## State-Recovery

State liegt im S3-kompatiblen Backend (OCI Object Storage), Bucket `sponsorplatz-tfstate-staging-free` (versioned). Lock via `dynamodb_table` ist NICHT konfiguriert — bei parallelen Apply-Versuchen kann es zu Konflikten kommen, daher Apply-Disziplin: nur eine Person zur Zeit.

## Manueller Teardown

```bash
terraform destroy
```

⚠️ Löscht VM + VCN + Buckets. Backups-Bucket vorher manuell exportieren!
