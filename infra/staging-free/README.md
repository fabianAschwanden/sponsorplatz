# Sponsorplatz — Staging-Free Deployment (manuell)

> **Reproduzierbares Setup via Terraform:** [`infra/envs/staging-free/`](../envs/staging-free/) ist die langfristige Quelle der Wahrheit. Diese manuelle Anleitung bleibt für Erst-Setup, Debugging und Tests in fremden Tenancies.

Deployment auf eine **OCI Always-Free-VM** (E2.1.Micro, 1 GB RAM) mit Docker-Compose + Caddy + Postgres.

## Architektur

```
   Internet  ─→  :443  ─→  Caddy (TLS, Let's Encrypt)
                              │
                              ▼
                          App (8080)
                              │
                              ▼
                        Postgres (5432)
```

Alle drei Container laufen auf derselben VM. Persistente Daten unter `/var/lib/sponsorplatz/`.

## Setup einer neuen VM (manuell, Phase 1 ohne Terraform)

1. **Always-Free-VM erstellen** in OCI:
   - Shape: `VM.Standard.E2.1.Micro` (frei) oder `VM.Standard.A1.Flex` (frei, ARM64)
   - Image: Oracle Linux 9
   - Ports öffnen: 22 (SSH, restricted), 80, 443

2. **Firewall:**
   ```bash
   sudo firewall-cmd --add-service=http --permanent
   sudo firewall-cmd --add-service=https --permanent
   sudo firewall-cmd --reload
   ```

3. **Docker installieren:**
   ```bash
   sudo dnf install -y dnf-plugins-core
   sudo dnf config-manager --add-repo https://download.docker.com/linux/centos/docker-ce.repo
   sudo dnf install -y docker-ce docker-ce-cli containerd.io docker-compose-plugin
   sudo systemctl enable --now docker
   sudo usermod -aG docker opc
   ```

4. **Verzeichnisse + Compose-File:**
   ```bash
   sudo mkdir -p /opt/sponsorplatz /var/lib/sponsorplatz/{db-data,uploads,caddy-data,caddy-config}
   sudo chown -R 999:999 /var/lib/sponsorplatz/db-data    # postgres-User im Container
   sudo chown -R 1001:1001 /var/lib/sponsorplatz/uploads  # sponsor-User im App-Container
   ```

5. **Files auf die VM kopieren:**
   ```bash
   scp Caddyfile docker-compose.yml opc@<VM-IP>:/tmp/
   ssh opc@<VM-IP> 'sudo mv /tmp/Caddyfile /tmp/docker-compose.yml /opt/sponsorplatz/'
   ```

6. **`.env` auf der VM erstellen** unter `/opt/sponsorplatz/.env`
   (mit Mode `0600`, Owner `root`):
   ```dotenv
   DB_PASSWORD=<starkes-pw>
   SMTP_HOST=smtp.mailgun.org
   SMTP_PORT=587
   SMTP_USER=<mailgun-user>
   SMTP_PASSWORD=<mailgun-pw>
   BASIS_URL=https://sponsorplatz.example.ch
   ACME_EMAIL=admin@example.ch
   DOMAIN=sponsorplatz.example.ch
   IMAGE_URL=zrh.ocir.io/<NAMESPACE>/sponsorplatz:staging-latest
   SPONSORPLATZ_ADMIN_EMAIL=admin@example.ch
   SPONSORPLATZ_ADMIN_PASSWORD=<starkes-pw-für-erste-Anmeldung>

   # --- REST-API (off-by-default; CD-managed, NICHT manuell setzen) ---
   # SPONSORPLATZ_API_KEY wird vom GitHub-Actions-CD-Workflow aus dem
   # gleichnamigen Repo-Secret in diese Datei synchronisiert (siehe
   # "REST-API freischalten" unten). Bitte hier weglassen — sonst
   # überschreibt der nächste CD-Run deine manuelle Eingabe.

   # --- Object Storage (optional, sonst lokales Volume) ---
   # STORAGE_PROVIDER=oci          # auskommentiert lassen → lokal
   # OCI_NAMESPACE=<oci os ns get>
   # OCI_REGION=eu-zurich-1
   # OCI_BUCKET_UPLOADS=sponsorplatz-uploads
   # OCI_BUCKET_BACKUPS=sponsorplatz-backups
   ```

7. **OCIR-Login + Pull + Start:**
   ```bash
   sudo docker login zrh.ocir.io -u '<NS>/<USER>' -p '<auth-token>'
   cd /opt/sponsorplatz
   sudo docker compose pull
   sudo docker compose up -d
   ```

8. **Verifikation:**
   ```bash
   curl -fsS https://sponsorplatz.example.ch/actuator/health
   ```

## CD via GitHub Actions

`cd-staging-free.yml` triggert nach jedem grünen CI-Run auf `main`:
1. Image bauen → Trivy-Scan
2. Push nach OCIR (`zrh.ocir.io/<NS>/sponsorplatz:<sha>` + `:staging-latest`)
3. SSH auf VM → `docker compose pull && up -d` für den App-Service

**Erforderliche GitHub-Variablen** (Settings → Secrets and variables → Actions):

| Type | Name | Wert |
|---|---|---|
| Variable | `STAGING_FREE_VM_IP` | öffentliche IP der VM |
| Variable | `OCIR_NAMESPACE` | OCI Object-Storage-Namespace |
| Variable | `OCIR_USERNAME` | OCI-User für OCIR-Push (z.B. `oracleidentitycloudservice/user@x`) |
| Secret | `OCIR_AUTH_TOKEN` | OCI-Auth-Token (User Settings → Auth Tokens) |
| Secret | `STAGING_FREE_SSH_PRIVATE_KEY` | SSH-Key für `opc@<VM-IP>` |
| Secret | `SLACK_WEBHOOK_URL` (optional) | für Failure-Notifications |

**Optional — OCIR-Retention** (cleant alte Images im Repo `sponsorplatz` nach
jedem Deploy, behält die 10 neuesten). Skipt mit Warning, wenn nicht gesetzt:

| Type | Name | Wert |
|---|---|---|
| Secret | `OCI_USER_OCID` | OCID des CICD-Service-Users |
| Secret | `OCI_TENANCY_OCID` | OCID der Tenancy |
| Secret | `OCI_KEY_FINGERPRINT` | Fingerprint des API-Keys (Console → User → API Keys) |
| Secret | `OCI_API_PRIVATE_KEY` | PEM-Inhalt des privaten API-Keys (inkl. Header/Footer) |

API-Key-Upload: Console → Identity → Users → CICD-User → API Keys → Add. Den
Public-Key hochladen, den private Key als `OCI_API_PRIVATE_KEY` ins GitHub-Repo.
Die `manage repos in tenancy`-Policy ist bereits in
`infra/shared/bootstrap/main.tf` enthalten — kein Policy-Update nötig.

## REST-API freischalten (CD-managed)

Die REST-API unter `/api/**` ist **off by default** — ohne Key antwortet
`ApiKeyFilter` mit 503. Zum Aktivieren:

1. **GitHub Repo-Secret setzen:** Settings → Secrets and variables → Actions
   → New repository secret:
   ```
   Name:  SPONSORPLATZ_API_KEY
   Value: <starker-key>     # z.B. `openssl rand -hex 32`
   ```

2. **CD triggern** — entweder via Push auf `main` oder
   `gh workflow run "CD Staging (Free Tier)"`. Der `Sync managed Secrets in
   VM-.env`-Step setzt den Key idempotent in `/opt/sponsorplatz/.env` und
   `--force-recreate` sorgt dafür dass der App-Container die neue ENV beim
   Boot liest.

3. **Verifizieren** nach dem Deploy:
   ```bash
   curl -s -o /dev/null -w "%{http_code}\n" \
     -H "X-API-Key: <dein-key>" \
     https://sponsorplatz.for-better.biz/api/backlog
   # Erwartet: 200
   ```

**Key-Rotation:** Secret-Wert im Repo aktualisieren, Workflow neu triggern.
Alter Key gilt bis zum nächsten erfolgreichen Deploy.

**Auf der VM nichts manuell editieren** — der CD-Workflow überschreibt den
Eintrag bei jedem Lauf. Andere Variablen (`DB_PASSWORD`, `SMTP_*`,
`ADMIN_*`) bleiben in der manuell gepflegten `.env` unangetastet.

## Object Storage (Phase 2)

Mit `STORAGE_PROVIDER=oci` werden Uploads + Backups in OCI Object Storage gespeichert:

- `OciStorageService` — Medien-Uploads (Logos, Cover-Bilder etc.) → Bucket `${OCI_BUCKET_UPLOADS}`
- `OciBackupCloudUploader` — `BackupService` lädt Dumps zusätzlich nach `${OCI_BUCKET_BACKUPS}`

**Buckets vorbereiten** (manuell, später Terraform):
```bash
oci os bucket create --name sponsorplatz-uploads --compartment-id <ocid> --versioning Enabled
oci os bucket create --name sponsorplatz-backups --compartment-id <ocid> --versioning Enabled
```

**Auth via Instance Principal** (Default in `cloud-free`):
- VM braucht eine Dynamic Group + IAM Policy:
  ```
  Allow dynamic-group <vm-dg> to manage objects in compartment <id> where target.bucket.name = /sponsorplatz-*/
  ```
- Kein `~/.oci/config` auf der VM nötig — `InstancePrincipalsAuthenticationDetailsProvider` zieht die Creds aus dem VM-Metadaten-Endpoint.

## Fallback: Manuelle Backups (ohne Object Storage)

```bash
# DB-Dump
ssh opc@<VM-IP> 'sudo docker exec sponsorplatz-db pg_dump -U sponsorplatz sponsorplatz | gzip' > backup-$(date +%F).sql.gz

# Uploads
ssh opc@<VM-IP> 'sudo tar -czf - -C /var/lib/sponsorplatz uploads' > uploads-$(date +%F).tar.gz
```
