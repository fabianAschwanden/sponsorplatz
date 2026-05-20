# ADR-0010: Umgebungs-Marker in Audit-Log + Sentry-Events

## Status
Akzeptiert

## Datum
2026-05-19

## Kontext

Phase 15.3 ([ADR-0009](0009-multi-cloud-azure-als-dr-zone.md)) brachte
Azure als zweite Zone neben OCI. Im Warm-DR-Modus wird die Postgres-DB
periodisch von OCI nach Azure restored (heute manuell via
`/admin/backups` + `pg_dump`-Dump, später automatisch via Slice 6).
Konsequenz für die `audit_log`-Tabelle:

- Nach dem Restore liegen identische Audit-Rows auf **beiden** Clouds.
- Aus den Daten allein ist nicht ablesbar, ob ein Eintrag auf der
  jeweiligen Cloud *entstanden* ist oder nur als Kopie reingespielt.
- Operatives Problem: bei Incident-Investigation
  ("Wann wurde diese Org verifiziert? Wer hat das ausgelöst?")
  vermischen sich die beiden Quellen.

Gleiches Bild im Sentry-Dashboard: ein Error-Event sieht aus wie das
andere, kein automatischer Filter "nur OCI-Events / nur Azure-Events".

## Entscheidung

Jeder Audit-Eintrag und jedes Sentry-Event trägt einen **Umgebungs-Marker**.

1. **Audit-Log:** neue Spalte `audit_log.umgebung VARCHAR(50) NOT NULL`
   ([Migration V41](../../src/main/resources/db/migration/V41__audit_log_umgebung.sql)),
   Index auf `(umgebung, zeitpunkt DESC)` für Filter.
2. **Wert kommt aus Spring-Property** `sponsorplatz.umgebung` (Default
   `lokal`). `AuditService` liest sie via `@Value` und setzt sie pro
   `protokolliereIntern`/`protokolliereMitBenutzer`. Wert auf der VM via
   Compose-ENV `SPONSORPLATZ_UMGEBUNG` gesetzt — OCI:
   `oci-staging-free`, Azure: `azure-staging`.
3. **Backfill** für historische Rows in
   [V42](../../src/main/resources/db/migration/V42__audit_log_umgebung_backfill_oci.sql):
   `unknown` → `oci-staging-free`, weil das vor Multi-Cloud das einzige
   produktive Deployment war.
4. **Sentry:** `SentryConfig.sentryBeforeSendCallback` setzt
   `event.setTag("sponsorplatz.umgebung", value)` auf jedem Event.
   Dashboard-Filter `sponsorplatz.umgebung:oci-staging-free` wird damit
   möglich.
5. **UI:** `/admin/audit` rendert eine `Umgebung`-Spalte mit farbig
   codiertem Badge.

## Konsequenzen

### Positiv

- **Audit-Trail-Integrität** ist nach Cross-Cloud-Restore wieder
  eindeutig. Die Provenienz jedes Eintrags ist explizit.
- **Sentry-Filter** trennen Cloud-Issues automatisch. Bei DR-Drills
  oder Outages lässt sich die fehlerhafte Cloud isolieren ohne
  Hostnamen-Magic.
- **Schema-Migration vergibt nicht die Zukunft:** der Default ist ein
  freier String, keine Enum-Constraint. Neue Umgebungen (z.B.
  `eu-west-aws`) brauchen keine Schema-Änderung, nur eine andere
  Property.
- **Backfill ist idempotent** (WHERE umgebung='unknown'), kann
  beliebig oft laufen, läuft auch auf Azure nach DB-Restore korrekt
  durch (Source der Rows ist ja wirklich OCI).

### Negativ

- **NOT NULL DEFAULT 'unknown'** auf V41 ist Defense in Depth, hat
  aber den Nebeneffekt dass ein vergessener AuditService-Aufruf (z.B.
  Direct-INSERT in einem Test) `unknown` produzieren würde — also ein
  Bug-Detect-Mechanismus.
- **Zwei Schreib-Stellen** synchron halten: V41-Default und
  AuditService-Property. Bei zukünftiger Schema-Refactoring-Migration
  daran denken.
- **Storage-Overhead** ist marginal (50 Bytes pro Row, Index ist
  small-cardinality).

## Alternativen

### Hostnamen / IP-Adresse
- **Pro:** kein Spring-Property-Setup nötig
- **Contra:** Bei Container-Replacement ändert sich der Hostname, der
  Audit-Trail wirkt "drift-ig". Plus IP-Privacy in DSG-Kontext.
- **Verworfen.**

### Separate `audit_log_oci` + `audit_log_azure` Tabellen
- **Pro:** keine Mischung möglich
- **Contra:** verdoppelt Schema, Queries, Indizes; Cross-Cloud-Reports
  würden UNION ALL brauchen; Restore-Story wird komplexer.
- **Verworfen.**

### Eintrag implizit lassen, Sentry-Tag reicht
- **Pro:** keine Schema-Migration
- **Contra:** Audit-UI hätte keine Information; Cross-Cloud-Forensik
  nur über Sentry möglich (das ist ein externer DSG-relevanter Dienst,
  nicht Source-of-Truth).
- **Verworfen.**

### Globally Unique Run-ID pro App-Start (UUID)
- **Pro:** auch Container-Restart-Generationen differenzierbar
- **Contra:** Zu hohe Granularität — Operateur will "OCI vs Azure"
  wissen, nicht "OCI-Pod-37 vs OCI-Pod-38". Außerdem keine Lesbarkeit
  in der UI.
- **Verworfen.**

## Referenzen

- [ADR-0009](0009-multi-cloud-azure-als-dr-zone.md) — Multi-Cloud-
  Entscheidung, die dieses Problem erst aufwirft
- [`specs/DATENMODELL.md`](../../specs/DATENMODELL.md) — V41 + V42
  Migrations-Eintrag, audit_log-Tabellen-Beschreibung
- [`specs/TESTSTRATEGIE.md`](../../specs/TESTSTRATEGIE.md) — AUDIT-04,
  AUDIT-05, VIEW-AUDIT-01
