# Deployment-Runbook — Sponsorplatz

> **Status:** Pilot-Launch-Vorbereitung (Phase 10.4)
> **Zielumgebung:** Oracle Cloud Infrastructure (OCI) Always-Free in Region `eu-zurich-1`
> **Domain:** `sponsorplatz.ch`

Dieses Runbook führt Schritt für Schritt durch den initialen Production-Deploy.
Code-seitig ist Phase 10 abgeschlossen — übrig bleiben die operativen Tasks aus
ROADMAP §10.4. Wer hier Punkte abhakt, kann den Pilot-Welle-1-Onboarding starten.

---

## 1. Voraussetzungen

- OCI-Account mit Free-Tier-Quota (Compute VM, Block Volume, Object Storage,
  Load Balancer)
- Zugriff auf `sponsorplatz.ch` (Registrar-Account: DNS-Records setzen,
  TXT für DKIM/DMARC, MX)
- Mail-Provider mit SMTP-Relay (z.B. Mailjet, SendGrid, Postmark, Brevo)
- GitHub-Repo-Zugriff (CI/CD-Workflows + Secrets)
- Lokale CLI: `oci`, `terraform`, `ssh`, `psql`, `dig`

---

## 2. HTTPS via OCI Load Balancer

OCI Load Balancer terminiert TLS; das Backend (Spring Boot in der VM) bleibt
HTTP-only auf Port 8080 — Loopback im Compose-Network.

1. **Zertifikat besorgen** — zwei Wege:
   - *Empfohlen für den Pilot:* Let's Encrypt via `certbot` auf der VM
     (HTTP-01-Challenge), Auto-Renewal alle 60 Tage.
   - *Alternativ:* OCI-managed Certificate Service (manuell signiert oder
     LE über OCI-Wrapper).
2. **Load Balancer konfigurieren:**
   - Listener: `:443` → TLS → Backend-Set `sponsorplatz-app:8080`
   - HTTP-Listener `:80` → 301-Redirect nach `https://`
   - Health-Check: `GET /actuator/health/liveness` alle 30 s
3. **Security-List / NSG:** Ingress nur `:80` + `:443` vom Public Internet
   auf den LB; VM nur vom LB-Subnetz aus erreichbar (kein direktes :8080).
4. **HSTS:** wird vom Spring `SecurityConfig` als Header gesetzt (siehe
   [`SecurityConfig.java`](src/main/java/ch/sponsorplatz/shared/config/SecurityConfig.java)
   — `prod`-Filter-Chain mit `max-age=31536000; includeSubDomains; preload`).
   Nach 30 Tagen erfolgreicher TLS-Uptime: HSTS-Preload-Liste eintragen.

**Smoke nach Deploy:**

```bash
curl -I https://sponsorplatz.ch                    # 200, Strict-Transport-Security gesetzt
curl -I http://sponsorplatz.ch                     # 301 → https
curl -s https://sponsorplatz.ch/actuator/health    # {"status":"UP"}
```

---

## 3. SMTP-Konfiguration (prod)

In dev läuft MailHog auf `:1025`. In prod brauchen wir einen echten Mail-
Provider — die Plattform versendet Verifizierungs-, Einladungs-, Anfrage-
und Kontakt-Mails.

1. **Bei einem CH-DSG-kompatiblen Provider registrieren** (Mailjet EU,
   Postmark, Brevo). Absender-Domain `noreply@sponsorplatz.ch` validieren.
2. **ENV-Variablen** in `docker-compose.prod.yml` setzen:
   ```yaml
   SPRING_MAIL_HOST: smtp.example.com
   SPRING_MAIL_PORT: 587
   SPRING_MAIL_USERNAME: <provider-user>
   SPRING_MAIL_PASSWORD: <provider-app-password>
   SPRING_MAIL_PROPERTIES_MAIL_SMTP_STARTTLS_ENABLE: "true"
   SPONSORPLATZ_MAIL_ABSENDER: noreply@sponsorplatz.ch
   ```
3. **Mail-User-Trennung:** ein separater SMTP-User pro Anwendung — Pilot,
   Staging, Tests getrennt. So bleibt der Rotations-/Sperr-Aufwand klein,
   wenn ein Schlüssel kompromittiert wird.
4. **Test:** mit dem Admin-User registrieren, Verifizierungs-Mail muss
   im Inbox landen; Bounce-Webhook im Provider konfigurieren.

---

## 4. SPF / DKIM / DMARC

Drei DNS-TXT-Records sind Pflicht, damit Vereins-Inboxen die Mails nicht
in Spam ablegen.

### SPF (TXT auf `sponsorplatz.ch`)

```
v=spf1 include:_spf.<provider>.com -all
```

`-all` = strict (Hard-Fail). Provider-Hostname aus deren Doku übernehmen.

### DKIM

Provider generiert ein DKIM-Key-Paar; öffentlicher Teil als TXT-Record
unter `<selector>._domainkey.sponsorplatz.ch`:

```
v=DKIM1; k=rsa; p=MIGfMA0GCSq...
```

Selector kommt vom Provider (oft `mailjet`, `pm`, etc.).

### DMARC

```
v=DMARC1; p=quarantine; rua=mailto:dmarc@sponsorplatz.ch; pct=100; adkim=s; aspf=s
```

Für den Pilot mit `p=quarantine` starten, nach 30 Tagen sauberer Reports
auf `p=reject` hochziehen. Reports-Auswertung via [postmark.com/dmarc](https://postmark.com/dmarc)
oder ähnlichem Service (kostenlos für niedrige Volumen).

**Verifikation:**

```bash
dig +short TXT sponsorplatz.ch                              # SPF da?
dig +short TXT mailjet._domainkey.sponsorplatz.ch           # DKIM?
dig +short TXT _dmarc.sponsorplatz.ch                       # DMARC?
```

Testmail an `check-auth@verifier.port25.com` — Antwort zeigt SPF/DKIM/DMARC-Pass.

---

## 5. Backups in OCI Object Storage spiegeln

Lokal werden Postgres-Dumps + Medien-Bucket-Inhalte bereits via
[`BackupService`](src/main/java/ch/sponsorplatz/backup/BackupService.java)
periodisch gesichert. Für Disaster-Recovery soll eine Kopie in einen
versionierten OCI-Object-Storage-Bucket fliessen.

1. **Bucket anlegen** im OCI-Tenancy:
   - Name: `sponsorplatz-backups-prod`
   - Region: `eu-zurich-1`
   - Versioning: **enabled**
   - Lifecycle-Rule: Versionen > 365 Tage löschen, Current-Versionen behalten
2. **OCI-Credentials** in ENV der VM hinterlegen (`OCI_TENANCY_OCID`,
   `OCI_USER_OCID`, `OCI_FINGERPRINT`, `OCI_PRIVATE_KEY`, `OCI_REGION`).
3. **Spring-Profile `prod`** lädt diese ENV — `OciStorageService` ist
   bereits implementiert; aktiviere via `sponsorplatz.storage.provider=oci`.
4. **Restore-Drill** einmal pro Quartal:
   - Backup-Tarball aus OCI laden
   - In Staging-VM extrahieren
   - `psql -f dump.sql` + Bucket-rsync
   - Manuelle Smoke-Test-Runde (siehe `SmokeIT`-Routen)
   - Ergebnis in [`docs/incidents/`](docs/incidents/) (anlegen) protokollieren

---

## 6. DNS sponsorplatz.ch + www-Redirect + IPv6

Beim Registrar (oder OCI DNS-Service, wenn Domain dort transferiert):

| Record | Wert | Notiz |
|---|---|---|
| `A` | `<OCI-LB-Public-IP-v4>` | TTL 3600 |
| `AAAA` | `<OCI-LB-Public-IP-v6>` | IPv6 aktivieren — Pflicht für Schweizer Netze |
| `A www` | `<OCI-LB-Public-IP-v4>` | gleicher LB |
| `CAA` | `0 issue "letsencrypt.org"` | nur Let's Encrypt darf für die Domain Zertifikate ausstellen |
| `TXT` | SPF, DKIM, DMARC | siehe §4 |

**`www`-Redirect** kann entweder im OCI-LB als zusätzlicher Listener
mit Rewrite-Regel, oder im Spring-Boot durch einen `WebMvcConfigurer`-
Bean implementiert werden. Beim Pilot: LB-Rewrite reicht.

**Verifikation:**

```bash
dig +short A sponsorplatz.ch
dig +short AAAA sponsorplatz.ch
dig +short A www.sponsorplatz.ch
curl -I https://www.sponsorplatz.ch              # 301 → sponsorplatz.ch
```

---

## 7. Sentry / Glitchtip Error-Tracking aktivieren

Code ist fertig (Phase 10.2), nur die DSN fehlt.

1. **DSN besorgen** (Sentry-EU-Cloud oder Glitchtip self-hosted in OCI).
2. **ENV** auf der VM:
   ```
   SENTRY_DSN=https://<key>@<host>/<project-id>
   SENTRY_ENVIRONMENT=production
   SENTRY_RELEASE=sponsorplatz@<git-tag>     # MUSS gesetzt sein in prod
   ```
3. **CI-Hook (optional):** Release-Tag bei jedem Deploy an Sentry
   pushen (`sentry-cli releases new ...`).
4. **Test:** in Produktion einen `RuntimeException`-Force-Endpoint aufrufen
   (z.B. via `?force-error=1`-Debug-Query in einer Test-Org), Event muss in
   Sentry erscheinen, User-IP darf NICHT enthalten sein (`send-default-pii=false`).

---

## 8. Smoke-Test-Suite gegen prod

Nach jedem Deploy:

```bash
mvn verify -P e2e -Dit.test=SmokeIT
# Sucht in prod-URL-Liste — anpassen in SmokeIT, falls noch nicht
```

5 SMOKE-Checks: Home, Login, Kontakt, Marktplatz-Auth-Gate, Actuator-Health.
Wenn ein Check rot ist → Deploy zurückrollen.

**Manuelle Pilot-Tests:** [`specs/BETA_TESTPLAN.md`](specs/BETA_TESTPLAN.md)
enthält die Akzeptanz-Test-Checkliste für Verein, Sponsor und Admin.

---

## 9. Pilot-Onboarding (5 Vereine)

1. Verein über `/sponsor/registrieren` oder den Self-Reg-Flow anlegen lassen.
2. Plattform-Admin verifiziert via `/admin/verifizierungen` (oder über die
   Aufgaben-Engine, die automatisch eine „Verein freigeben"-Task erzeugt).
3. Owner-Welcome-Mail mit Einladungs-Link an einen Team-Member.
4. Verein lädt drei Sponsoring-Projekte hoch (Empfehlung: zwei realistische
   + eines mit Pitch-Deck-PDF, damit der Anfrage-Flow ausgereizt wird).
5. Nach erfolgter Anfrage durch einen Sponsor: Vertrags-Erstellung +
   Rechnungs-Lifecycle einmal manuell durchspielen.

---

## 10. Public-Launch-Kommunikation

Erst NACHDEM §§1–9 grün sind:

- Blog-Post auf `sponsorplatz.ch/blog` (existiert noch nicht — anlegen oder
  via [Substack/Medium](https://substack.com) abdecken)
- LinkedIn-Post: Founders-Story, Health-Fokus, CH-Hosting, DSG-Compliance
- E-Mail an die lokalen Sport-/Health-Verbände (Liste in `specs/PILOT_KONTAKTE.md`
  — anlegen, sobald Kontakte recherchiert)
- Twitter/X optional, Reddit `r/Switzerland` als Zweitkanal

**Messen** nach Launch (alles aus Phase 10.1-Monitoring):
- Anzahl Registrierungen / Tag
- Conversion `/kontakt`-Submit → manueller Reply / Min-Stunden
- Anzahl verifizierte Vereine (Admin-Dashboard)
- Sentry-Error-Rate < 1 % der Requests

---

## Anhang: Rollback-Plan

Wenn ein Deploy in prod rot wird:

1. `docker compose --profile app down` auf der VM
2. Vorherigen Tag aus dem Docker-Registry pullen:
   `docker pull <registry>/sponsorplatz:<prev-tag>`
3. `docker-compose.prod.yml` Image-Tag ändern, `up -d`
4. Smoke-IT erneut → grün?
5. Sentry-Issue als „regression" taggen
6. Bug-Fix in `main`, Hotfix-Branch, neues Tag, redeploy
