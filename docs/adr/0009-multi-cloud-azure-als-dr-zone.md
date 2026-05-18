# ADR-0009: Multi-Cloud — Azure als zweite Zone (Warm-DR)

## Status
Akzeptiert

## Datum
2026-05-18

## Kontext

Sponsorplatz läuft heute auf **einer** OCI-Always-Free-VM in `eu-zurich-1`:
- Eine VM (`VM.Standard.E5.Flex`, 1 GB RAM)
- Eine Postgres-Instanz (Docker-Container auf derselben VM)
- Ein Object-Storage-Tenant für Uploads + Backups

Das ist ein **dreifacher Single-Point-of-Failure**:
1. **VM-Hardware/Hypervisor** — Reboot kann 10–60 min dauern, OCI gibt
   für Always-Free kein SLA.
2. **Region** — Naturereignis / Provider-Outage in `eu-zurich-1` legt
   uns offline (Cloudflare-Dashboard zeigt OCI-Zürich gelegentlich rot).
3. **Tenancy** — falls Oracle den Free-Tenant aus Compliance-Gründen
   stilllegt (z.B. CPU-Auslastung > 95 % über 7 Tage triggert Reclamation),
   verlieren wir VM **und** Object Storage gleichzeitig.

Für Phase 14 (erster Echtbetrieb, 5 Vereine) ist das tolerierbar — wir
haben tägliche Backups, Wiederherstellung dauert max. ein paar Stunden,
und die Pilot-Vereine wissen, dass es ein Pilot ist.

Spätestens wenn echte Sponsoring-Verträge fliessen (Phase 15.1
Zahlungs-Provider, Phase 15.2 Mahnwesen), brauchen wir eine zweite
Zone, in die wir innerhalb von ≤ 30 min umschalten können.

## Entscheidung

Wir bauen **Azure als zweite Zone** auf, initial im Modus **Warm-DR**:

1. **Provider-Wahl: Azure** (nicht GCP, nicht AWS).
   Begründung in *Alternativen* unten.

2. **DR-Modus: warm.** Azure läuft permanent mit derselben App-Version,
   eigener DB (Azure Database for PostgreSQL Flexible Server, Burstable
   B1ms), eigenem Blob-Storage. Empfängt aber **keinen Produktiv-Traffic**
   bis zum Promote-Switch.

3. **DB-Cross-Cloud-Sync: Backup-Restore** (nicht Logical Replication).
   `BackupService` (siehe Phase 6 — Production-Readiness) lädt den
   nächtlichen `pg_dump` in **beide** Buckets. Im DR-Fall: latest Dump
   aus Azure Blob nach Flexible Server restored — RPO ≤ 24h.

4. **Image-Registry: OCIR + ACR parallel.** Beide Clouds pullen aus
   ihrer eigenen Registry. Cross-Cloud-Pull-Risiko (Bandbreite, Auth-
   Komplexität, Latenz) entfällt.

5. **DNS-Failover: Cloudflare** (nicht Azure Traffic Manager).
   Cloudflare ist provider-neutral — die Failover-Policy hängt nicht
   am gleichen Provider wie das Failure-Target. Health-Check auf
   `/login` mit 60s-Intervall, Threshold 3.

6. **App-Code-Abstraktion: `@ConditionalOnProperty(...="azure")`.**
   `AzureBlobStorageService` und `AzureBackupCloudUploader` sind
   parallel zu den OCI-Pendants, aktiv über `sponsorplatz.storage.provider`.
   Beide Provider können gleichzeitig aktiv sein (List-Injection im
   `BackupService` für Cross-Replication — Slice 6).

## Konsequenzen

### Positiv

- **Echtes DR-Target** — OCI-Komplettausfall ist überbrückbar.
- **Provider-Lock-in-Risiko sinkt** — App ist erstmals nachweislich
  cloud-agnostisch (zwei produktive Backends, statt nur "wir könnten
  theoretisch").
- **App-Schicht-Abstraktion zahlt sich aus** — `StorageService` und
  `BackupCloudUploader` sind bereits Interfaces (ADR-0001 Feature-Folder
  zwingt zu sauberen Schnitten), das Hinzufügen eines zweiten Providers
  ist primär Wiring + Tests, nicht Refactor.
- **Bessere Kennzahlen pro Provider** — wir messen reale Storage-Kosten,
  Latenz, Verfügbarkeit in beiden Clouds und können fundiert für
  Phase 15+ entscheiden.

### Negativ

- **Azure ist nicht Free-Tier.** Standard_B2s (~CHF 30) + Flex Postgres
  B1ms (~CHF 25) + Storage Account (~CHF 5) + ACR Basic (~CHF 5) ≈
  **CHF 50–80/Monat**. Muss vor Slice 3 (Terraform-Apply) freigegeben
  werden — bricht das Free-Tier-Versprechen aus Phase 14.
- **RPO ≤ 24h ist nicht ausreichend für Phase 15.1+** (echte
  Zahlungen). Wir akzeptieren das bewusst und planen Logical Replication
  als separates Phase-15.X-Vorhaben, sobald Zahlungsflüsse aktiv sind.
- **Operative Doppellast** — zwei Stacks zu monitoren, zwei
  TF-States, zwei CD-Workflows. Mitigation: identische Compose-Files,
  identisches cloud-init, eigener `cd-azure-staging.yml` mit
  Preflight-Skip-Pattern.
- **Test-Komplexität wächst** — neue CLOUD-STO-AZ + CLOUD-BKP-AZ
  Test-IDs, plus Cross-Replication-IT (BKP-X). Unit-Tests mocken die
  Azure-SDK-Clients (kein Live-Azure für CI nötig).

### Spannungsfelder

- **Aktiv-Aktiv später** — der Warm-DR-Modus ist explizit ein
  Zwischenschritt. Aktiv-Aktiv mit shared DB würde Logical Replication
  (oder eine global verteilte DB wie CockroachDB / Spanner) erfordern
  und ist ein separates Projekt. Wir bauen die App-Abstraktion so, dass
  beide Provider parallel aktiv sein können — aber wir promovieren das
  erst, wenn Slice 1–7 stabil laufen.
- **DNS-Provider-Lock-in nach Cloudflare** — wir verlagern den
  SPoF eine Schicht höher (Cloudflare statt OCI). Cloudflare hat
  besseres SLA + Multi-Cloud-Health-Checks; akzeptiert.

## Alternativen

### Hetzner als zweite Zone
- **Pro:** günstig (ca. CHF 8/Monat), EU-Datenresidenz, keine
  Vendor-Komplexität.
- **Contra:** kein managed Postgres, kein Object Storage (S3-kompatibel
  nur via 3rd-party), kein Container Registry — wir müssten alles
  selbst betreiben. Würde die operative Last verdreifachen statt
  verdoppeln. **Verworfen.**

### AWS als zweite Zone
- **Pro:** Marktstandard, beste Doku, RDS + S3 + ECR sind
  ausgereift.
- **Contra:** Datenresidenz CH/EU komplexer (eu-central-1 Frankfurt
  wäre die nächste Region, kein CH-Endpoint). Kostenstruktur weniger
  vorhersehbar (Egress, NAT-Gateway, Request-Pricing). **Verworfen für
  die initiale zweite Zone**, bleibt Option für eine dritte Zone.

### GCP als zweite Zone
- **Pro:** ähnlich AWS, Cloud SQL + GCS sind solide.
- **Contra:** keine CH-Region (nur europe-west6 Zürich für einige
  Services, nicht Cloud SQL Flexible). Tooling weniger CH-vertraut.
  **Verworfen.**

### Azure als zweite Zone (gewählt)
- **Pro:** Region `switzerlandnorth` (Zürich) verfügbar, Azure Database
  for PostgreSQL Flexible Server CH-residentähig, ACR + Blob Storage
  + Managed Identity sind Standard. Microsoft-Stack ist bei
  potenziellen Sponsor-Orgs (KMU, Versicherungen) das vertraute
  Vendor-Profil.
- **Contra:** keine Free-Tier-Variante, Pricing > OCI.
- **Entscheidung:** akzeptiert wegen CH-Region + Standard-Stack +
  Managed Identity (kein Connection-String-Management auf der VM).

### DR-Modus: cold vs. warm vs. aktiv-aktiv
- **Cold** — Azure-Stack wird im Failover-Fall erst aufgebaut. Pro:
  null Laufkosten. Contra: RTO mehrere Stunden, TF-Apply auf der
  Failover-Achse zu langsam und fehleranfällig. **Verworfen.**
- **Warm** — gewählt, siehe oben.
- **Aktiv-Aktiv** — beide Zonen serven Traffic. Erfordert shared DB
  (siehe Spannungsfelder). **Verschoben** auf separates Projekt nach
  Slice 1–7.

### DB-Sync: Backup-Restore vs. Logical Replication
- **Logical Replication** — Postgres-natives Cross-Cloud-Streaming.
  Pro: RPO Sekunden. Contra: VPN/Peering zwischen OCI und Azure nötig,
  Komplexität hoch (Replikations-Slot-Management, DDL-Migrations-
  Koordination), Operations-Aufwand übersteigt den Nutzen für
  Phase 15.3. **Verworfen für initiale Umsetzung**, bleibt
  Folge-Vorhaben.

### DNS-Failover: Cloudflare vs. Azure Traffic Manager
- **Azure Traffic Manager** — wäre einfacher in Azure-Console
  integriert. Contra: hängt am gleichen Provider wie das Failover-Ziel.
  **Verworfen.**

## Referenzen

- [`specs/ROADMAP.md`](../../specs/ROADMAP.md) — Phase 15.3
- [`infra/staging-free/README.md`](../../infra/staging-free/README.md) — bestehender OCI-Stack
- [`infra/envs/staging-free/`](../../infra/envs/staging-free/) — TF-Modul-Vorlage
- ADR-0001 (Feature-Folder) — begründet, warum die Storage-Abstraktion
  bereits sauber kapselbar ist
- Tests: CLOUD-STO-AZ-01..06, CLOUD-BKP-AZ-01..04, BKP-X-01..03, SMOKE-MC-01..02
