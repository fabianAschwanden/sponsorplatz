# ADR-0011: Hybrid — kollaborative Stammdaten + private Sponsor-CRM-Layer

## Status
Akzeptiert

## Datum
2026-05-28

## Kontext

[ADR-0003](0003-kollaborative-plattform-statt-multi-tenant.md) hat Sponsorplatz
bewusst als **kollaborative Plattform** ausgelegt: geteilte Datenbasis, „alle
eingeloggten Benutzer sehen alle Daten", Edit-Rechte über `Mitgliedschaft`. Für
die **Discovery-Seite** (Marktplatz, öffentliche Vereinsprofile, geteilte
Sponsor-Stammdaten als Datenqualitäts-Vorteil) ist das richtig und bleibt es.

Die CRM-Lücken-Analyse ([`specs/CRM-LUECKEN.md`](../../specs/CRM-LUECKEN.md))
zeigt aber: Sobald ein Unternehmen sein Portfolio gesponsorter Vereine
**professionell verwalten** will (Pipeline, Account-Notizen, Forecast-Beträge,
Win/Loss-Gründe, Kontakt-Historie), kollidiert das frontal mit dem
Wikipedia-Modell:

- Pipeline-Daten und interne Forecasts dürfen **nicht** zwischen konkurrierenden
  Sponsoren sichtbar sein (CSS-Pipeline ist nichts für Helsana).
- Account-Notizen über einen Verein sind konkurrenzkritisches Wissen.
- Compliance in Krankenkassen verlangt nachweisbare Datentrennung — das lässt
  sich nicht über Edit-Rollen erschlagen.

Zwei Wege standen zur Wahl:

- **A) ADR-0003 kippen** → Voll-Multi-Tenant. Bricht den Marktplatz, erzwingt
  Migration aller Bestandsdaten, wirft den Datenqualitäts-Vorteil der geteilten
  Stammdaten weg.
- **B) Hybrid** → Die kollaborative Layer bleibt unangetastet, eine **private
  Sponsor-CRM-Layer** kommt additiv dazu.

## Entscheidung

**Wir wählen B (Hybrid).** ADR-0003 wird *nicht* superseded, sondern **ergänzt**:

| Layer | Sichtbarkeit | Entitäten |
|---|---|---|
| **Kollaborativ** (ADR-0003, unverändert) | alle eingeloggten User lesen alles | `Organisation`, `Projekt`, `SponsoringPaket`, `MedienAsset`, Marktplatz, Engagement-Schaufenster |
| **Privat pro Sponsor** (NEU, dieser ADR) | nur Mitglieder der besitzenden Sponsor-Org + PLATFORM_ADMIN | künftige CRM-Aggregate: `SponsorAccount`, `KontaktPerson`, `Aktivitaet`, Pipeline, Forecast |

**Regeln der privaten Layer:**

1. Jede CRM-Entität trägt eine `besitzer_sponsor_org_id` (FK → `organisation`,
   typischerweise `OrgTyp.UNTERNEHMEN`).
2. Lese- *und* Schreibzugriff laufen über
   `AccessControl.kannSponsorDatenSehen(sponsorOrgId, auth)` — true nur für
   Mitglieder der Sponsor-Org (jede Rolle) oder PLATFORM_ADMIN.
3. **Kein** Repository-Query auf CRM-Daten ohne Sponsor-Filter. Das wird durch
   einen Integrationstest abgesichert (Sponsor A sieht Sponsor-B-CRM-Daten NIE)
   und perspektivisch durch eine ArchUnit-Regel, sobald das `crm`-Package steht.
4. CRM-Daten erscheinen **nie** im Marktplatz, in öffentlichen Profilen oder im
   Engagement-Schaufenster.

## Konsequenzen

**Positiv:**
- Keine Migration der Bestandsdaten, kein Bruch am Marktplatz — die private Layer
  ist eine zusätzliche, isolierte Schicht.
- Enterprise-Sponsoren bekommen echtes CRM mit Compliance-tauglicher
  Datentrennung, ohne dass die Community-Idee verschwindet.
- Die Architektur bleibt ein einziges Deployment (kein Tenant-Sharding).

**Negativ / Risiken:**
- Zwei Sichtbarkeits-Modelle nebeneinander erhöhen die Komplexität — ein
  Entwickler muss bei jeder neuen Entität bewusst entscheiden „kollaborativ
  oder privat?".
- Die Isolations-Garantie ist security-kritisch: ein vergessener Filter leakt
  Konkurrenz-Daten. Deshalb ist der Isolations-Test **Pflicht vor** der ersten
  CRM-Entität, nicht danach.
- `Mitgliedschaft` an einer `OrgTyp.UNTERNEHMEN` bekommt eine neue Bedeutung
  (Zugriff auf CRM-Daten) — Rollenkonzept muss das dokumentieren.

## Umsetzung (Cluster 1 der CRM-Lücken-Analyse)

1. Diese Entscheidung (ADR-0011). ✅
2. `AccessControl.kannSponsorDatenSehen(UUID sponsorOrgId, Authentication)` +
   Unit-Test. ✅ (dieser Schritt)
3. Erste CRM-Entität `SponsorAccount` in neuem `crm`-Package + Integrationstest
   der die Isolation beweist (Folge-Schritt).
4. ArchUnit-Regel „CRM-Repos nur mit Sponsor-Scope" sobald das Package existiert.

Siehe [`specs/CRM-LUECKEN.md`](../../specs/CRM-LUECKEN.md) §Vorgehens-Vorschlag.
