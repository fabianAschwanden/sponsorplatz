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
   IMAGE_URL=ghcr.io/fabianaschwanden/sponsorplatz:staging-latest
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

7. **Pull + Start:**
   ```bash
   cd /opt/sponsorplatz
   sudo docker compose pull
   sudo docker compose up -d
   ```

   GHCR-Pull funktioniert ohne Login solange das Package `sponsorplatz`
   public ist (GitHub → Profile → Packages → sponsorplatz → Package
   settings → Change visibility → Public). Falls private, vorher:
   ```bash
   echo '<github-pat>' | sudo docker login ghcr.io -u '<github-user>' --password-stdin
   ```

8. **Verifikation:**
   ```bash
   curl -fsS https://sponsorplatz.example.ch/actuator/health
   ```

## CD via GitHub Actions

`cd-staging-free.yml` triggert nach jedem grünen CI-Run auf `main`:
1. Image bauen → Trivy-Scan
2. Push nach **GHCR** (`ghcr.io/fabianaschwanden/sponsorplatz:<sha>` + `:staging-latest`)
3. SSH auf VM → `docker compose pull && up -d` für den App-Service

> **Migration von OCIR → GHCR (2026-05-22):** OCI Always-Free lehnt seit
> Mai 2026 Pushes nach OCIR ab (`Free tier account is not supported`).
> Pipeline pusht jetzt nach GHCR via `GITHUB_TOKEN` (`permissions:
> packages: write`). Live-VM-Update siehe Abschnitt "Umstieg auf GHCR"
> weiter unten.

**Erforderliche GitHub-Variablen** (Settings → Secrets and variables → Actions):

| Type | Name | Wert |
|---|---|---|
| Variable | `STAGING_FREE_VM_IP` | öffentliche IP der VM |
| Secret | `STAGING_FREE_SSH_PRIVATE_KEY` | SSH-Key für `opc@<VM-IP>` |
| Secret | `SLACK_WEBHOOK_URL` (optional) | für Failure-Notifications |

Für GHCR braucht es **keine** Secrets: `GITHUB_TOKEN` ist automatisch
verfügbar, und das Package wird nach erstem Push (manuell einmalig) auf
`public` gestellt → Pull auf der VM funktioniert ohne Login.

## Umstieg auf GHCR (einmalig, nach Migration)

Nach dem ersten erfolgreichen CD-Run auf GHCR:

1. **Package auf public stellen** (sonst kann die VM nicht pullen ohne PAT):
   GitHub → Profil → Packages → `sponsorplatz` → Package settings →
   Danger Zone → Change visibility → Public.

2. **Live-VM: docker-compose.yml umstellen** auf den neuen Image-Pfad:
   ```bash
   ssh opc@<VM-IP>
   sudo sed -i 's|zrh\.ocir\.io/[^/]*/sponsorplatz|ghcr.io/fabianaschwanden/sponsorplatz|' \
     /opt/sponsorplatz/docker-compose.yml
   grep image: /opt/sponsorplatz/docker-compose.yml
   # erwartet: image: ghcr.io/fabianaschwanden/sponsorplatz:staging-latest
   ```

3. **Alten OCIR-docker-login auf der VM aufräumen** (optional, schadet aber sonst nichts):
   ```bash
   sudo docker logout zrh.ocir.io
   ```

4. **Pull + Restart** mit neuem Image:
   ```bash
   cd /opt/sponsorplatz
   sudo docker compose pull app
   sudo docker compose up -d --force-recreate app
   ```

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

3. **Auf laufenden VMs** (provisioniert vor 2026-05-26): die docker-compose-
   `environment:`-Sektion auf der VM hatte vor dem Fix keine
   `SPONSORPLATZ_API_KEY`-Substitution — die `.env` wurde gesynct, kam aber
   nicht in den Container (deshalb 503 trotz gesetztem Secret). Mit dem
   Helper-Skript einmalig nachziehen:
   ```bash
   ./infra/scripts/patch-vm-compose-envs.sh opc@<OCI-VM-IP>
   ./infra/scripts/patch-vm-compose-envs.sh sponsoradmin@<AZURE-VM-IP>
   ```
   Der Helper fügt die Substitution idempotent ein und restartet den
   Container.

4. **Verifizieren** nach dem Deploy:
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

## Sentry-Release-Tagging (CD-managed)

Jeder erfolgreiche CD-Run setzt automatisch `SENTRY_RELEASE=sponsorplatz@<git-sha>`
in `/opt/sponsorplatz/.env`, sodass Spring beim Container-Recreate den
Release-Tag in Sentry-Events einbettet. Sentry selbst bleibt deaktiviert,
solange `SENTRY_DSN` nicht als Repo-Secret gesetzt ist.

**Aktivieren:**

1. Sentry-Projekt-DSN holen (Settings → Client Keys (DSN) → kopieren).
2. Repo-Secrets setzen (Settings → Secrets and variables → Actions):
   - `SENTRY_DSN` → kompletter DSN-String
   - `SENTRY_AUTH_TOKEN` (optional) → Internal Integration Token mit `project:releases`
3. Repo-Vars setzen für den optionalen `getsentry/action-release`-Step:
   - `SENTRY_ORG` → Sentry-Org-Slug
   - `SENTRY_PROJECT` → Project-Slug
4. CD neu auslösen — beim nächsten Container-Recreate ist Sentry an, und
   bei gesetztem AUTH_TOKEN landet der Release-Marker auch im Sentry-UI.

## OIDC Google-Login (CD-managed)

Google-SSO ist **off-by-default** — ohne CLIENT_ID/SECRET registriert Spring
den Client nicht, und auf `/login` erscheint kein Google-Button.

**Aktivieren:**

1. Google Cloud Console → APIs & Services → Credentials → **OAuth client ID**
   (Web application). Authorized Redirect URIs:
   ```
   https://sponsorplatz.for-better.biz/login/oauth2/code/google
   https://sponsorplatz.for-the.biz/login/oauth2/code/google     (Azure-Pendant)
   http://localhost:8080/login/oauth2/code/google                (lokal/dev)
   ```
2. Repo-Secrets setzen (Settings → Secrets and variables → Actions):
   - `GOOGLE_CLIENT_ID` → die `*.apps.googleusercontent.com`-ID
   - `GOOGLE_CLIENT_SECRET` → das `GOCSPX-…`-Secret (nur 1× sichtbar nach Anlage)
3. CD neu triggern (Push auf `main` oder `gh workflow run "CD Staging (Free Tier)"`).
   Der `Sync managed Secrets`-Step setzt beide ENVs idempotent in
   `/opt/sponsorplatz/.env`, `--force-recreate` startet den App-Container neu.
4. **Verifizieren:** `/login` → "Google"-Button → Consent-Screen → eingeloggt.
   Im App-Log: `OIDC JIT-Provisioning: neuer AppUser für …` beim Erst-Login.

**Optional: Domain-Whitelist** als Account-Takeover-Schutz (Spec §6.3).
Setze in der manuellen `/opt/sponsorplatz/.env` (nicht CD-managed):
```
SPONSORPLATZ_OIDC_EMAIL_DOMAIN_WHITELIST=css.ch,sponsorplatz.ch
```
Logins von Emails ausserhalb dieser Domains werden mit 401 abgewiesen.

**Constants** (CLIENT_NAME, SCOPE, ISSUER_URI) sind hardcoded in
`infra/envs/staging-free/cloud-init.yaml.tftpl` und brauchen keinen
Secret-Roundtrip — nur die wirklich geheimen Werte fliessen durch GitHub.

**Auf laufender VM (ohne Terraform-Re-Apply):** falls die VM vor dem
Google-OIDC-Wiring (2026-05-25) bereitgestellt wurde, fehlen die 5
`SPRING_SECURITY_OAUTH2_CLIENT_*_GOOGLE_*`-Einträge in der laufenden
`/opt/sponsorplatz/docker-compose.yml`. Der Helper patcht das idempotent:

```bash
./infra/scripts/patch-vm-compose-envs.sh opc@<OCI-VM-IP> --restart
```

Fügt Sentry- und Google-OIDC-Blöcke vor `JAVA_OPTS:` ein (no-op wenn schon
da), validiert via `docker compose config`, restartet die App. Backup
mit Timestamp wird unter `/opt/sponsorplatz/docker-compose.yml.bak.*`
hinterlegt — Rollback bei Bedarf via `sudo cp -a <backup> docker-compose.yml`.

**Key-Rotation:** Secret-Wert im Repo aktualisieren, Workflow neu triggern.
Google-Side: in der Cloud Console → Credentials → "Add Secret" + altes
Secret nach erfolgreichem Roll-Out löschen.

## Rollback (vorheriger Image-Tag)

Wenn ein Deploy ein Regression-Issue verursacht, ist der Rollback ein
fixer Image-Tag auf der VM — ohne erneuten CD-Lauf. Dauer ~30 Sekunden,
keine DB-Migration rückwärts notwendig (additive Migrations-Policy, siehe
CLAUDE.md).

1. **Vorherigen erfolgreichen Tag finden:**
   ```bash
   # Lokal — letzte 10 erfolgreichen CD-Runs
   gh run list --workflow=cd-staging-free.yml --status=success --limit=10
   # oder auf der VM — lokal vorhandene Images
   ssh opc@<VM-IP> 'sudo docker image ls ghcr.io/fabianaschwanden/sponsorplatz --format "{{.Tag}}\t{{.CreatedSince}}"'
   ```

2. **VM-`.env` auf den Pin-Tag setzen:**
   ```bash
   ssh opc@<VM-IP>
   ROLLBACK_SHA=<vorheriger-sha>     # z.B. a1c524bdfc5b...
   sudo grep -v '^IMAGE_URL=' /opt/sponsorplatz/.env > /tmp/env.new || true
   echo "IMAGE_URL=ghcr.io/fabianaschwanden/sponsorplatz:$ROLLBACK_SHA" >> /tmp/env.new
   sudo install -m 0600 -o root -g root /tmp/env.new /opt/sponsorplatz/.env
   cd /opt/sponsorplatz && sudo docker compose pull app && sudo docker compose up -d --force-recreate app
   ```

3. **Smoke verifizieren:**
   ```bash
   curl -fsS https://sponsorplatz.for-better.biz/login -o /dev/null -w "%{http_code}\n"  # erwartet 200
   ```

4. **Fix nachschieben + CD wieder normalisieren:**
   - Issue in `main` fixen + pushen.
   - Nach grünem CI/CD `IMAGE_URL`-Pin aus `.env` wieder entfernen, damit
     der nächste CD-Run automatisch das `staging-latest`-Floating-Tag
     pullen kann:
     ```bash
     ssh opc@<VM-IP> "sudo sed -i '/^IMAGE_URL=/d' /opt/sponsorplatz/.env"
     ```

**Kein Rollback der DB**: Migrationen sind strikt additiv (siehe CLAUDE.md
Abschnitt "Migrationen"), neuer Code läuft auch gegen das alte Schema.
Falls eine Migration einen Datenfehler produziert, gilt der Backup-Restore-
Pfad aus dem Admin-UI (`/admin/backups`, `/admin/datei-backups`).

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
