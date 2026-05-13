# Kontakt-Anfrage → Vertrag

> **Spec-Erweiterung zu** [`DATENMODELL.md`](DATENMODELL.md) **und** [`SPONSORING_ZAHLUNGSFLUSS.md`](SPONSORING_ZAHLUNGSFLUSS.md)
> — Dokumentiert den zweiten Vertrags-Entstehungspfad: aus einer proaktiv vom Verein
> gestellten Kontakt-Anfrage (Verein → Sponsor), nicht nur aus klassischen
> Paket-Anfragen (Sponsor → Verein-Paket).

## Motivation

Bisher konnte ein Vertrag nur aus einer **Paket-Anfrage** entstehen:

```
Sponsor    ──(Anfrage zum Paket)──>    Verein    ──(Annahme)──>    Vertrag
                                                                   ▲
                                                                   │ erstellt
                                                                Verein
```

Vereine, die proaktiv einen Sponsor suchen (z.B. „CSS würde uns als Verein für das
Sommerfest sponsern?"), können seit Phase Wachstum eine **Kontakt-Anfrage** stellen
— hatten danach aber kein UI-Pfad zum Vertrag. Diese Spec schliesst die Lücke.

## Erweiterter Flow

```
Verein    ──(Kontakt-Anfrage)──>    Sponsor    ──(Annahme)──>    Vertrag
                                                                 ▲
                                                                 │ erstellt
                                                              Verein
```

Schritte:

1. **Verein-Owner** stellt Kontakt-Anfrage via `/anfragen/neu-kontakt` (existiert seit Phase 0)
2. **Sponsor-Owner** sieht die Anfrage in „Eingehende Anfragen" und nimmt sie an
3. **Verein-Owner** sieht die nun `ANGENOMMEN`-Anfrage in „Meine ausgehende Anfragen"
   oder „Ausgehende Anfragen zu \<Org\>" mit neuem **„Vertrag erstellen"-Button**
4. Klick → `POST /organisationen/{verein-slug}/anfragen/{id}/vertrag/erstellen`
   → Vertrags-Entwurf entsteht, Verein-Owner ergänzt Preis + Leistungen im Edit-Form
   → `markiereUnterzeichnet` → Vertrag aktiv

## Technische Spezifikation

### Vertrags-Orgs-Mapping nach OrgTyp

`VertragService.erstelle()` muss bei beiden Anfrage-Richtungen das gleiche
Vertrags-Schema produzieren:

| Vertrag-Feld | Paket-Anfrage | Kontakt-Anfrage |
|---|---|---|
| `v.org` (Verein-Seite) | `anfrage.empfaengerOrg` (Verein) | `anfrage.anfragenderOrg` (Verein) |
| `v.sponsorOrg` (Sponsor-Seite) | `anfrage.anfragenderOrg` (Sponsor) | `anfrage.empfaengerOrg` (Sponsor) |

Implementierung via **OrgTyp-Check** — robust für beide Richtungen:

```java
Organisation vereinOrg = (anfrage.getEmpfaengerOrg().getTyp() == OrgTyp.VEREIN)
        ? anfrage.getEmpfaengerOrg()
        : anfrage.getAnfragenderOrg();
```

### Paket-Snapshot

Kontakt-Anfragen haben kein Paket — daher kein Preis, kein Leistungsumfang.
Mit V33 können Vereine optional einen **Wunsch-Betrag** mitgeben, der als
Initial-Preis in den Vertrag übernommen wird:

| Vertrag-Feld | Paket-Anfrage | Kontakt-Anfrage |
|---|---|---|
| `paketName` | `paket.getName()` | `anfrage.getBetreff()` |
| `paketBeschreibung` | `paket.getBeschreibung()` | `anfrage.getNachricht()` |
| `preisChf` | `paket.getPreisChf()` | `anfrage.getWunschBetragChf()` (oder 0, wenn nicht angegeben) |

Der Verein-Owner kann `preisChf` + `leistungVerein`/`leistungSponsor` im Vertrags-
Edit-Form anpassen, bevor `markiereUnterzeichnet` aufrufbar ist. Der Wunsch-Betrag
ist ein Vorschlag — Sponsor + Verein verhandeln im Vertrag-Entwurf den finalen Wert.

**V33-Schema-Erweiterung** (`sponsoring_anfrage.wunsch_betrag_chf NUMERIC(12,2) NULL`):

- NULLABLE: Paket-Anfragen brauchen den Wert nie; Kontakt-Anfragen können ihn
  weglassen (= "kein Richtbetrag genannt").
- CHECK `>= 0`: defense-in-depth zur Form-Validierung (`@DecimalMin("0")`).
- 0 ist erlaubt — Naturalien-Sponsoring (keine Geldzahlung, nur Sachleistungen).

### AnfrageView.vereinSlug()

Damit das Template die Vertrag-Erstellungs-URL bauen kann, ohne die Anfrage-Richtung
kennen zu müssen:

```java
public String vereinSlug() {
    if (anfragenderOrgTyp == OrgTyp.VEREIN) return anfragenderOrgSlug;
    if (empfaengerOrgTyp  == OrgTyp.VEREIN) return empfaengerOrgSlug;
    return empfaengerOrgSlug; // Fallback für Datensätze ohne Typ
}
```

### Access-Control

`VertragController.erstellen` ruft `accessControl.kannOrgEditierenNachSlug(slug, auth)`:

- Paket-Anfrage: `slug` ist `empfaengerOrgSlug` (= Verein-Slug) — Verein-Owner hat Edit-Recht ✓
- Kontakt-Anfrage: `slug` ist `anfragenderOrgSlug` (= Verein-Slug) — Verein-Owner hat Edit-Recht ✓

Defensive Check zusätzlich im Service:
```
v.getOrg() == anfrage[verein-side] && currentUser ∈ v.getOrg().mitgliedschaften
```
(implizit über die Slug-Berechtigung; kein doppelter Check nötig)

## UI-Spezifikation (meine-anfragen.html)

### Vertrag-Button-Sichtbarkeit

```
                            │ Paket-Anfrage          │ Kontakt-Anfrage
────────────────────────────┼────────────────────────┼─────────────────────
Eingehend                   │ ✓ wenn ANGENOMMEN      │ ✗ (Sponsor sieht es,
(= Verein bei Paket,        │   (Verein ist Empfänger,│    aber Verein erstellt
   Sponsor bei Kontakt)     │    erstellt Vertrag)   │    den Vertrag)
────────────────────────────┼────────────────────────┼─────────────────────
Meine ausgehende            │ — (Verein stellt keine │ ✓ wenn ANGENOMMEN
(= Verein bei Kontakt)      │   Paket-Anfragen)      │   (Verein-Owner erstellt)
────────────────────────────┼────────────────────────┼─────────────────────
Org-ausgehend               │ —                      │ ✓ wenn ANGENOMMEN
(= andere Mitglieder)       │                        │   (jeder Verein-Editor
                            │                        │    kann erstellen)
```

Template-Bedingungen:
- Eingehend: `${a.status.name() == 'ANGENOMMEN' and a.istPaketAnfrage()}` (unverändert)
- Ausgehend (beide Sections): `${a.status.name() == 'ANGENOMMEN' and !a.istPaketAnfrage()}`

## Test-IDs (siehe [TESTSTRATEGIE.md](TESTSTRATEGIE.md))

| ID | Test | Verifiziert |
|---|---|---|
| **VTR-09** | `VertragServiceTest` | Kontakt-Anfrage → Vertrag mit OrgTyp-basiertem Mapping (Verein = `org`, Sponsor = `sponsorOrg`) |
| **VTR-10** | `VertragServiceTest` | Kontakt-Anfrage-Snapshot ohne Wunsch-Betrag: `betreff` → `paketName`, `preisChf` = 0 |
| **VTR-10b** | `VertragServiceTest` | Kontakt-Anfrage mit `wunschBetragChf=5000` → Vertrag startet mit `preisChf=5000` (Verein kann's noch editieren) |
| **VIEW-13** | `AnfrageViewTest` | `vereinSlug()` liefert Anfragender-Slug bei Kontakt-Anfrage, Empfänger-Slug bei Paket-Anfrage |
| **ANF-08** | `SponsoringAnfrageServiceTest` | `erstelleKontaktAnfrage` mit negativem Wunsch-Betrag wirft `IllegalArgumentException` (Defense-in-Depth zum DB-CHECK) |
| **E2E-01** | `sponsor-anfrage-zu-vertrag.feature` | End-to-End: Verein registriert → Verein anlegt → Kontakt-Anfrage → CSS nimmt an → Vertrag |

## Offene Punkte (TBD)

- **Pflicht-Validierung des Preises**: Vertrag muss vor `markiereUnterzeichnet`
  einen `preisChf > 0` haben (oder explizit als „Naturalien-Sponsoring"
  markiert). Aktuell akzeptiert das Edit-Form jeden Wert.
- **Mehrfache Verträge pro Sponsor-Beziehung**: bisher `UNIQUE(anfrage_id)`.
  Wenn ein Verein wiederkehrend bei demselben Sponsor anfragt, gibt es pro
  Anfrage einen Vertrag — keine Aggregation auf Org-Ebene. Bewusst so für Phase 0.
- **Mehrere Pakete pro Anfrage** (Variante B aus dem Review): Anfrage-zu-Paket
  als M:N — eine Anfrage könnte mehrere Pakete „auswählen" lassen. Nicht für
  Phase 0; benötigt separate Spec mit Verhandlungs-Status-Maschine.

## Migration

- **V33** (`anfrage_wunschbetrag.sql`): fügt `wunsch_betrag_chf NUMERIC(12,2)
  NULL` plus `chk_anfrage_wunsch_betrag_nicht_negativ` zur Tabelle
  `sponsoring_anfrage` hinzu.
- Die `vertrag.anfrage_id`-FK existiert seit V18 und ist bereits
  nullable-tolerant gegenüber `paket_id`. Snapshot-Felder (`org_name`,
  `paket_name`, `preis_chf`, `sponsor_org_id`) sind bereits in der Tabelle.

Restliche fachliche Erweiterung passiert auf Service + Template + View-DTO-Ebene.
