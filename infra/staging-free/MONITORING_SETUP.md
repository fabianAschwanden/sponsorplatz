# Monitoring-Setup für Sponsorplatz (staging-free)

Aktuell: **kein** Monitoring. Wenn die VM ausfällt oder die App crasht, weiss
das niemand. Dieser Guide aktiviert externes Health-Monitoring mit UptimeRobot
(50 Monitore gratis, 5-min-Polling).

## Schritt 1 — UptimeRobot-Account anlegen

1. https://uptimerobot.com/ → Sign up
2. E-Mail-Bestätigung
3. Dashboard öffnet automatisch

## Schritt 2 — Monitor anlegen

**+ New Monitor** → Settings:

| Feld | Wert |
|---|---|
| **Monitor Type** | HTTP(s) |
| **Friendly Name** | Sponsorplatz Health |
| **URL (or IP)** | `https://sponsorplatz.for-better.biz/actuator/health` |
| **Monitoring Interval** | 5 minutes |
| **Monitor Timeout** | 30 seconds |
| **Keyword** (optional) | `"status":"UP"` — exact-match, alarmiert wenn der String fehlt |

> Status-Code-Check würde reichen, aber Keyword ist robuster: Caddy könnte
> 200 zurückgeben, während die App tot ist und Spring statisches HTML
> ausliefert. `"status":"UP"` schlägt dann nicht an, Alert kommt.

## Schritt 3 — Alert Contact einrichten

**Settings → Alert Contacts → Add Alert Contact:**

| Type | Wofür |
|---|---|
| **E-Mail** | Default — kommt direkt an. Empfehlung: alleine reicht |
| **Slack/Discord** | optional, für Team-Channel |
| **SMS** | nur Pro-Plan, nicht nötig für staging |
| **Webhook** | für eigenes Alerting-System |

Empfehlung: E-Mail an `fabian.aschwanden@gmail.com` reicht für staging.

Den Contact zurück im Monitor unter **Alert Contacts To Notify** auswählen.

## Schritt 4 — Status-Page (optional, public)

UptimeRobot bietet eine kostenlose Public Status-Page:

**My Settings → Public Status Pages → Add Status Page**

- Name: `Sponsorplatz Status`
- Custom Domain: optional (`status.sponsorplatz.for-better.biz` via CNAME)
- Monitors: Sponsorplatz Health auswählen

URL z.B. `https://stats.uptimerobot.com/<id>` — kannst du als Vertrauens-
Signal im Footer verlinken.

## Schritt 5 — Test

Auf der VM kurz die App stoppen:

```bash
ssh -i ~/.ssh/sponsoren_staging_free_deploy opc@144.24.246.244 \
  'cd /opt/sponsorplatz && sudo docker compose stop app'
```

Innerhalb 5–10 min sollte UptimeRobot die DOWN-Mail schicken. Danach:

```bash
ssh -i ~/.ssh/sponsoren_staging_free_deploy opc@144.24.246.244 \
  'cd /opt/sponsorplatz && sudo docker compose start app'
```

→ UP-Recovery-Mail kommt nach dem nächsten Poll-Zyklus.

## Was UptimeRobot NICHT abdeckt

- **Memory-/CPU-Saturation** der VM — App antwortet noch, aber langsam.
  Lösung: OCI Compute Metrics (siehe unten).
- **Festplatte voll** — selbst wenn das Health-Endpoint OK liefert, gehen
  Backups + Uploads kaputt. Lösung: separater Cron + Slack-Webhook auf VM.
- **DB-Connection-Pool erschöpft** — Spring meldet ggf. UNKNOWN beim
  DB-Health-Indicator, aber wir exposen nur health/info ohne details.

## OCI-Native Alternative — Compute Metrics + Notifications

OCI Console → **Observability & Management → Monitoring → Alarm Definitions**:

1. **Create Alarm**
2. Metric Namespace: `oci_computeagent`
3. Metric: `CpuUtilization`
4. Aggregation: `mean`
5. Trigger: `> 90%` for 5 min
6. Severity: WARNING
7. Destination: OCI Notification Service Topic → E-Mail-Subscription auf
   `fabian.aschwanden@gmail.com`

Wiederholen für `MemoryUtilization > 90%` und ggf. `DiskBytesWritten` als
Frühwarnung.

→ Free-Tier-Quota umfasst Monitoring + Notifications gratis (max. 5'000
Notifications/Monat).

## Empfehlung Reihenfolge

1. **UptimeRobot Health-Monitor** (10 min Setup, sofort wertvoll)
2. **OCI Memory-Alarm** (5 min, warnt bevor OOM-Kill kommt)
3. **OCI Disk-Alarm** (5 min, warnt bevor Backups failen)
