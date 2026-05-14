# Sponsoring-Zahlungsfluss — End-to-End-Spec

> **Status:** Aktiv (Mai 2026)
> **Bezug:** V16 `vertrag`, V17 `rechnung + iban + adresse`, Phase 9.2 Zahlungs-Provider-Anbindung
> **Referenz-Specs:** [`DATENMODELL.md`](DATENMODELL.md), [`ROLLENKONZEPT.md`](ROLLENKONZEPT.md), [`TESTSTRATEGIE.md`](TESTSTRATEGIE.md)
> **Code-Anker:** `ch.sponsorplatz.anfrage.{Vertrag,Rechnung,Payment*}`

---

## 1. Ziel und Scope

Der Sponsoring-Zahlungsfluss schliesst die Lücke zwischen einer **angenommenen Sponsoring-Anfrage** und dem **eingegangenen Geld** auf dem Verein-Konto. Diese Spec ist der End-to-End-Vertrag für alle beteiligten Entities, ihre Statusmaschinen, die Swiss-QR-Bill-Compliance, MwSt, Mahnwesen, Storno und den Datenschutz.

**Im Scope:**

- Vertragsbildung aus angenommener Anfrage
- Rechnung mit Swiss-QR-Bill (offline-Bezahlung per Banking-App)
- Status-Lifecycles und erlaubte Übergänge
- Storno mit/ohne Geldfluss
- Mahnwesen
- DSG-Permission-Matrix
- Audit-Log-Pflicht-Events

**Backlog (separate Detail-Spec folgt):**

- Datatrans-Online-Zahlung (Phase 9.2-Erweiterung)
- Buchhaltungs-Export (CSV/camt.054)
- Mehrere Rechnungen pro Vertrag (Teilzahlungen / Raten)
- Stripe Connect
- MwSt-Reverse-Charge bei ausländischen Sponsoren

## 2. Zahlungsfluss-Lifecycle

```
┌───────────────────────────────────────────────────────────────────────────┐
│  1. Sponsoring-Anfrage    → AnfrageStatus.NEU                             │
│                                                                            │
│  2. Verein bearbeitet     → AnfrageStatus.IN_PRUEFUNG                     │
│                                                                            │
│  3. Verein nimmt an       → AnfrageStatus.ANGENOMMEN                      │
│                            ▼                                              │
│  4. Vertrag wird erstellt → VertragsStatus.ENTWURF                        │
│     (Snapshot Org / Sponsor / Paket / Preis aus Anfrage)                  │
│                            ▼                                              │
│  5. Verein-Owner          → VertragsStatus.UNTERZEICHNET                  │
│     unterzeichnet          (unterzeichnet_am, unterzeichnet_von gesetzt)  │
│                            ▼                                              │
│  6. Rechnung erstellt     → RechnungsStatus.OFFEN                         │
│     (PDF mit Swiss-QR-Code, an sponsor_email versendet)                   │
│                            ▼                                              │
│  7. Sponsor scannt QR     ─────── Geldfluss off-platform ───────►        │
│     in Banking-App und                              IBAN des Vereins      │
│     überweist                                                             │
│                            ▼                                              │
│  8. Verein erhält Geld    → Verein-Owner markiert manuell als BEZAHLT     │
│     auf Bankkonto          (bezahlt_am, bezahlt_von gesetzt)              │
│                                                                            │
│  Optional: Datatrans-Webhook (Phase 9.2 Erweiterung)                      │
│  → RechnungService.markiereAlsBezahltViaWebhook (idempotent)              │
└───────────────────────────────────────────────────────────────────────────┘
```

**Stationen mit Akteuren:**

| Schritt | Aktion | Akteur | Resultat |
|---|---|---|---|
| 1 | Anfrage erstellen | Sponsor-User | `sponsoring_anfrage.NEU` |
| 2 | Anfrage prüfen | Verein-EDITOR/OWNER | `IN_PRUEFUNG` |
| 3 | Anfrage annehmen | Verein-EDITOR/OWNER | `ANGENOMMEN` + Mail an Sponsor |
| 4 | Vertrag erstellen | Verein-OWNER | `vertrag.ENTWURF` |
| 5 | Vertrag unterzeichnen | Verein-OWNER | `vertrag.UNTERZEICHNET` |
| 6 | Rechnung erstellen | Verein-OWNER | `rechnung.OFFEN` + PDF an `sponsor_email` |
| 7 | Banking-App-Zahlung | Sponsor (extern) | Geldfluss auf Vereins-IBAN |
| 8 | Bezahlt-Markierung | Verein-OWNER | `rechnung.BEZAHLT` |

## 3. Statusmaschinen

> **Phase-0-Unterzeichnungs-Modell (manuell + physisch):**
> Die Plattform fungiert als Speicher der Vertrags-Eckdaten (Parteien, Paket,
> Preis, Laufzeit, Leistungen, Status) und bietet PDF-Export. Verein und
> Sponsor unterzeichnen den Vertrag <em>physisch</em> (Ausdruck → Originale
> per Post oder Übergabe). Der Verein-Owner klickt anschließend auf der
> Plattform „Als unterzeichnet markieren" — `markiereUnterzeichnet`
> bestätigt also die offline erfolgte physische Unterschrift, ist keine
> digitale Signatur. Digitale QES (Skribble/DocuSign, Roadmap 5.G) ist
> bewusst auf Backlog verschoben, bis das Pilot-Volumen einen Lizenz-ROI
> rechtfertigt.

### 3.1 `VertragsStatus`

```
                            ┌─────────────┐
                            │   ENTWURF   │
                            └──────┬──────┘
                                   │ markiereUnterzeichnet()  ← bestätigt
                                   │   physische Unterschrift (Phase 0)
                                   ▼
                            ┌─────────────┐
                            │ UNTERZEICHNET│──────────► (Rechnung erstellbar)
                            └──────┬──────┘
                                   │ kuendige()
                                   ▼
                            ┌─────────────┐
                            │ GEKUENDIGT  │
                            └─────────────┘
```

| Übergang | Erlaubt? | Vorbedingung | Wer |
|---|---|---|---|
| ENTWURF → UNTERZEICHNET | ✓ | — | ORG_OWNER der Verein-Org |
| UNTERZEICHNET → GEKUENDIGT | ✓ | Keine offene Rechnung mit Status BEZAHLT (siehe 3.3) | ORG_OWNER |
| GEKUENDIGT → * | ✗ | terminal | — |
| ENTWURF → GEKUENDIGT | ✗ | nur via Löschen des Entwurfs | — |

### 3.2 `RechnungsStatus`

```
                            ┌─────────────┐
                            │    OFFEN    │
                            └──────┬──────┘
                                   │ markiereBezahlt()
                                   ▼
                            ┌─────────────┐
                            │   BEZAHLT   │
                            └──────┬──────┘
                                   │ rueckbuchung()  (Backlog Phase 12)
                                   ▼
                            ┌─────────────┐
                            │  STORNIERT  │◄──────── OFFEN → stornieren()
                            └─────────────┘
```

| Übergang | Erlaubt? | Vorbedingung | Wer |
|---|---|---|---|
| OFFEN → BEZAHLT | ✓ | — | ORG_OWNER / Datatrans-Webhook |
| OFFEN → STORNIERT | ✓ | — | ORG_OWNER |
| BEZAHLT → STORNIERT | ◐ | nur via expliziten `rueckbuchung()`-Flow (Backlog) | ORG_OWNER |
| STORNIERT → * | ✗ | terminal | — |
| BEZAHLT → OFFEN | ✗ | nicht erlaubt — Buchhaltungs-Integrität | — |

**Stornierung-Detail:**

- `OFFEN → STORNIERT` setzt nur den Marker. Lückenlosigkeit der Rechnungsnummerierung bleibt erhalten (siehe 5).
- `BEZAHLT → STORNIERT` ist Backlog — erfordert echte Rückabwicklung (Provider-Refund oder manuelles Inkasso). Initial wirft `RechnungService.stornieren` bei `status=BEZAHLT` `IllegalStateException` (gegen Test RECH-06).

### 3.3 Vertrag-Kündigung mit offener Rechnung

Wenn der Vertrag gekündigt werden soll, aber die zugehörige Rechnung Status `BEZAHLT` hat, wirft `VertragService.kuendige` `IllegalStateException` mit dem Hinweis "Rechnung muss erst storniert/zurückgebucht werden". Damit bleibt die Buchhaltungs-Konsistenz erhalten.

## 4. Swiss-QR-Bill — Compliance

### 4.1 IBAN-Anforderungen

- **Format:** ISO 13616 (Mod-97-Prüfsumme)
- **Schweiz:** `CH` + 2 Prüfziffern + 5 Stellen Institut-ID + 12 Stellen Kontonummer
- **QR-IBAN:** Institut-ID-Range `30000-31999` — nur dann darf eine 27-stellige strukturierte QR-Referenz verwendet werden (Six-Group-Spec)
- **Normale IBAN:** Institut-ID ausserhalb 30000-31999 — strukturierte Referenz darf nicht gesetzt sein, Zahlungszweck als unstrukturierter Verwendungszweck

Validierung im `IbanValidator`-Util (TBD):

```java
public class IbanValidator {
    boolean istCh(String iban);             // CH-Präfix + 21 Stellen total
    boolean istQrIban(String iban);         // Institut-ID 30000-31999
    boolean istChecksumValide(String iban); // Mod-97
}
```

### 4.2 QR-Referenz-Berechnung (Mod-10-Recursive)

Bei QR-IBAN: 26 Stellen Referenz-Body + 1 Prüfziffer nach Six-Group-Spec.
Body-Konvention für Sponsorplatz:

```
Stellen 1-7:   ORG_ID-Hash (numerisch, 7-stellig)
Stellen 8-12:  Jahr (z. B. 02026 — fünfstellig mit führender 0)
Stellen 13-19: Rechnung-Lfd-Nr (7-stellig, pro Org-Jahr)
Stellen 20-26: 0-padding
Stelle  27:    Mod-10-Prüfziffer
```

Beispiel: `0001234 02026 0000042 0000000 7`

Implementiert in `QrReferenzGenerator` (TBD).

### 4.3 PDF-Layout

Strikt nach [Six-Group-Style-Guide Swiss-QR-Bill v2.2](https://www.six-group.com/dam/download/banking-services/standardization/qr-bill/style-guide-de.pdf):

- Zahlteil unten auf der Seite, A4-Hochformat, Perforations-Marker vorhanden
- Empfangsschein links (62 × 105 mm)
- Zahlteil mittig (148 × 105 mm)
- Schweizer-Kreuz-Marker im QR-Code (7 × 7 mm zentriert)
- Mindestschriftgrösse 8 Pt für die Zahlteil-Felder

**PDF-Bibliothek:** `OpenHTMLtoPDF` (Apache 2.0, MIT-kompatibel) rendert die Thymeleaf-Templates `rechnung-pdf.html` / `vertrag-pdf.html`. QR-Code wird mit `ZXing` (Apache 2.0) als PNG generiert und im Template inline-base64 eingebettet.

### 4.4 Sponsor-Adresse als Schuldner-Block

Pflicht im Zahlteil. Aus `rechnung.sponsor_name` + `rechnung.sponsor_adresse`. Wenn `sponsor_adresse = NULL`, wird ein leerer "Ohne Schuldner"-Block gerendert (gemäss Six-Spec erlaubt — Sponsor füllt selbst aus).

## 5. Rechnungs-Nummerierung

### 5.1 Format

```
R-YYYY-NNNNN
   │    │
   │    └── pro Org laufende Nummer, 5-stellig, ab 1 pro Jahr
   └── 4-stelliges Jahr aus erstellt_am
```

Beispiele: `R-2026-00001`, `R-2026-00042`, `R-2027-00001`

### 5.2 Generator-Regel

Pro `org_id` und Jahr eine **lückenlose** laufende Nummer. Implementierung:

```java
public String naechsteRechnungsnummer(UUID orgId) {
    int jahr = LocalDate.now().getYear();
    String prefix = "R-" + jahr + "-";
    long nextNr = repository.findMaxLfdNrForOrgAndJahr(orgId, jahr) + 1;
    return prefix + "%05d".formatted(nextNr);
}
```

**Lückenlosigkeit:** stornierte Rechnungen bleiben in der Nummerierung mit Status STORNIERT — sie werden nicht gelöscht. Das erfüllt die Schweizer Buchhaltungs-Pflichten (OR Art. 957 ff.).

**Thread-Safety:** `synchronized` auf Service-Ebene oder `@Lock(LockModeType.PESSIMISTIC_WRITE)` auf das Org-Locking. Bei Bedarf eigene `rechnungsnummer_lfdnr`-Tabelle mit `INSERT ... ON CONFLICT` für Postgres.

### 5.3 Migration

Bestehende V17 hat `rechnungsnummer VARCHAR(50) NOT NULL` und `UNIQUE (org_id, rechnungsnummer)`. Kein Schema-Change nötig — nur Service-Logik.

## 6. MwSt

### 6.1 Default-Verhalten

Schweizer Sportvereine sind in der Regel **nicht MwSt-pflichtig** (Umsatzgrenze CHF 100'000 für gemeinnützige Vereine). Default-Verhalten: **Rechnung wird ohne MwSt-Ausweis erstellt**.

### 6.2 MwSt-Erweiterung (Backlog Phase 12)

Wenn eine Org MwSt-pflichtig ist:

- Migration: neue Spalten `organisation.mwst_nummer` (VARCHAR 16, Format `CHE-123.456.789 MWST`), `organisation.ist_mwst_pflichtig` (BOOLEAN)
- Rechnungs-Snapshot bei Erstellung: `rechnung.mwst_satz_prozent` (z. B. 8.1 für CH Standardsatz 2024+), `rechnung.mwst_betrag_chf`
- Rechnungs-PDF zeigt:
  ```
  Netto:    CHF  929.69
  MwSt 8.1%: CHF   75.31
  ──────────────────────
  Total:    CHF 1'005.00
  ```
- `betrag_chf` bleibt Brutto-Betrag (Konsistenz mit Vertrag-`preis_chf`, der ebenfalls inkl. MwSt verstanden wird)

**Initial-Implementation:** MwSt-Felder NULL, keine Anzeige auf PDF. Wenn Pilot-Vereine MwSt-pflichtig sind, Phase-12-Erweiterung.

## 7. Mahnwesen (Phase 11/12)

### 7.1 Reminder-Sequenz

| Zeitpunkt | Stufe | Aktion | Mahngebühr |
|---|---|---|---|
| 7 Tage **vor** Fälligkeit | Höflicher Reminder | Mail an `sponsor_email` mit Rechnungs-Wiedergabe | – |
| 7 Tage **nach** Fälligkeit | 1. Mahnung | Mail mit "Zahlungserinnerung" | – |
| 14 Tage **nach** Fälligkeit | 2. Mahnung | Mail mit Mahngebühr-Hinweis | CHF 20.– (auf neuer Rechnung, Backlog) |
| 30 Tage **nach** Fälligkeit | Eskalation | Hinweis-Mail an Verein-OWNER, kein automatischer Sponsor-Kontakt mehr | manuelles Inkasso |

### 7.2 Implementation

`MahnungsCronJob` läuft täglich um 06:00 (Spring `@Scheduled(cron = "0 0 6 * * *")`), iteriert über alle `rechnung WHERE status=OFFEN`, prüft `faellig_am` und versendet Mails. Idempotenz via `rechnung.mahnstufe` (INT) + `letzte_mahnung_am` (TIMESTAMP).

Migration V26 oder höher:

```sql
ALTER TABLE rechnung ADD COLUMN mahnstufe       INT       NOT NULL DEFAULT 0;
ALTER TABLE rechnung ADD COLUMN letzte_mahnung_am TIMESTAMP;
```

## 8. Storno

### 8.1 OFFEN → STORNIERT (einfacher Pfad)

- Aufruf: `RechnungService.stornieren(rechnungId, grund)`
- Setzt `status=STORNIERT`, `bezahlt_am=NULL`, optionalen `storno_grund` (Migration nötig)
- AuditLog-Event `RECHNUNG_STORNIERT` mit `grund` und `vorheriger_status=OFFEN`
- Mail an `sponsor_email` mit Storno-Hinweis

### 8.2 BEZAHLT → Rückabwicklung (Backlog Phase 12)

Initial: nicht unterstützt — `IllegalStateException` (siehe RECH-06).

Backlog-Konzept:
- Manuelles Inkasso: Verein-Owner stösst Rückzahlung an Sponsor an
- Mit Datatrans-Provider: `PaymentProvider.widerrufe(transaktionsReferenz)` triggert Refund
- Bilateraler Anhang: `rechnung.rueckbuchung_in_arbeit=true`, `rueckbuchung_am`, `rueckbuchung_durchgefuehrt_von`

## 9. DSG / Permission-Matrix

| Aktion | ORG_VIEWER der Verein-Org | ORG_EDITOR der Verein-Org | ORG_OWNER der Verein-Org | Sponsor-Org-Mitglied | PLATFORM_ADMIN | Public |
|---|:---:|:---:|:---:|:---:|:---:|:---:|
| Vertrag lesen | ✓ | ✓ | ✓ | ✓ (eigene) | ✓ (Audit) | ✗ |
| Vertrag erstellen | ✗ | ✗ | ✓ | ✗ | ✗ | ✗ |
| Vertrag unterzeichnen | ✗ | ✗ | ✓ | ✗ | ✗ | ✗ |
| Vertrag kündigen | ✗ | ✗ | ✓ | ✗ | ✗ | ✗ |
| Vertrag-PDF herunterladen | ✓ | ✓ | ✓ | ✓ (eigene) | ✓ | ✗ |
| Rechnung lesen | ✓ | ✓ | ✓ | ✓ (eigene) | ✓ | ✗ |
| Rechnung erstellen | ✗ | ✓ | ✓ | ✗ | ✗ | ✗ |
| Rechnung als bezahlt markieren | ✗ | ✗ | ✓ | ✗ | ✗ (nur via Webhook) | ✗ |
| Rechnung stornieren | ✗ | ✗ | ✓ | ✗ | ✗ | ✗ |
| Rechnung-PDF herunterladen | ✓ | ✓ | ✓ | ✓ (eigene) | ✓ | ✗ |
| Storno-Grund einsehen | ✗ | ✓ | ✓ | ✗ | ✓ | ✗ |

Durchgesetzt im `AccessControl`-Bean. Sponsor-Org-Recht via `vertrag.sponsor_org_id = membership.org_id`-Match.

## 10. Audit-Log — Pflicht-Events

Folgende Aktionen müssen via `AuditService.protokolliere(...)` geloggt sein (siehe V14 `audit_log`):

| Event-Konstante | Wann | Felder |
|---|---|---|
| `VERTRAG_ERSTELLT` | `VertragService.erstelle` | vertrag_id, anfrage_id, ersteller_id |
| `VERTRAG_UNTERZEICHNET` | `markiereUnterzeichnet` | vertrag_id, unterzeichner_id |
| `VERTRAG_GEKUENDIGT` | `kuendige` | vertrag_id, grund |
| `RECHNUNG_ERSTELLT` | `RechnungService.erstelle` | rechnung_id, vertrag_id, betrag, rechnungsnummer |
| `RECHNUNG_BEZAHLT` | `markiereBezahlt` | rechnung_id, bezahlt_von_id, quelle (`MANUELL` / `WEBHOOK_DATATRANS`) |
| `RECHNUNG_STORNIERT` | `stornieren` | rechnung_id, grund, vorheriger_status |
| `RECHNUNG_MAHNUNG_VERSENDET` | `MahnungsCronJob` | rechnung_id, mahnstufe |
| `RECHNUNG_PDF_HERUNTERGELADEN` | `RechnungController.pdf` | rechnung_id, user_id |

Konstanten in `AuditAktion.java` ergänzen (vorhanden).

## 11. Datatrans-Integration (Backlog Phase 9.2)

### 11.1 Architektur

- Datatrans **Hosted Payment Page** (HPP) — kein Card-Data-Handling in Sponsorplatz
- Flow: Verein erstellt Rechnung → wählt "Online-Zahlungslink aktivieren" → `PaymentService.erstelleZahlung(rechnung)` → Datatrans-init-Call → Bezahllink im PDF
- Sponsor klickt Link → Datatrans-HPP → Card / TWINT / PostFinance / Apple Pay
- Datatrans-Webhook → `POST /payment/webhook/datatrans` → `RechnungService.markiereAlsBezahltViaWebhook(rechnungsnummer, quelle=WEBHOOK_DATATRANS)`

### 11.2 Endpoints

| Umgebung | Base-URL |
|---|---|
| Sandbox | `https://api.sandbox.datatrans.com` |
| Production | `https://api.datatrans.com` |

### 11.3 Sicherheit

- HMAC-SHA256-Signatur-Verifikation auf Webhook (Header `Datatrans-Signature`)
- Idempotenz via `payment_transaction.provider_reference` UNIQUE — doppelter Webhook ist kein Fehler, sondern 200 ohne State-Change
- Replay-Schutz via `payment_transaction.received_at` mit 24h-Toleranz

### 11.4 Migration V27 (TBD)

```sql
CREATE TABLE payment_transaction (
    id                    UUID         PRIMARY KEY,
    rechnung_id           UUID         NOT NULL REFERENCES rechnung(id),
    provider              VARCHAR(50)  NOT NULL,
    provider_reference    VARCHAR(255) NOT NULL,
    status                VARCHAR(20)  NOT NULL,
    betrag_chf            NUMERIC(10,2) NOT NULL,
    raw_payload           TEXT,        -- Vollständiger Webhook-Payload für Audit
    received_at           TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    processed_at          TIMESTAMP,

    CONSTRAINT chk_payment_status   CHECK (status IN ('PENDING', 'AUTHORIZED', 'SETTLED', 'FAILED', 'REFUNDED')),
    CONSTRAINT chk_payment_provider CHECK (provider IN ('STUB', 'DATATRANS')),
    CONSTRAINT uq_payment_provider_ref UNIQUE (provider, provider_reference)
);

CREATE INDEX idx_payment_rechnung_id ON payment_transaction(rechnung_id);
```

## 12. Tests

### 12.1 Bestehende Test-IDs (siehe TESTSTRATEGIE.md)

- **VTR-01..06** — Vertrag-Erstellung, Snapshot, Status-Übergänge
- **RECH-01..06** — Rechnung-Erstellung, IBAN-Pflicht, QR-Referenz, Bezahlung, Storno
- **PAY-01..06** — Payment-Provider-Stub, Webhook-Idempotenz, Service-Delegation

### 12.2 Neue Test-IDs aus dieser Spec

| ID | Test-Klasse | Beschreibung |
|---|---|---|
| **VTR-07** | `VertragServiceTest` | `kuendige` mit bezahlter Rechnung wirft `IllegalStateException` |
| **VTR-08** | `VertragServiceTest` | `kuendige` mit offener Rechnung erlaubt, Rechnung wird mit storniert |
| **RECH-07** | `RechnungsnummerGeneratorTest` | Format `R-YYYY-NNNNN`, pro Org-Jahr fortlaufend, lückenlos |
| **RECH-08** | `RechnungsnummerGeneratorTest` | Jahres-Rollover startet bei 1, kein Reset bei laufendem Jahr |
| **RECH-09** | `RechnungsnummerGeneratorTest` | Stornierte Rechnung bleibt in Nummerierung, nächste Nummer überspringt nicht |
| **RECH-10** | `IbanValidatorTest` | Mod-97-Prüfsumme erkennt ungültige Iban |
| **RECH-11** | `IbanValidatorTest` | `istQrIban` true für 30000-31999, false sonst |
| **RECH-12** | `QrReferenzGeneratorTest` | 27-stellige Referenz mit Mod-10-Prüfziffer am Ende |
| **RECH-13** | `RechnungControllerTest` | GET `/rechnungen/{id}` ohne ORG_VIEWER-Recht → 403 |
| **RECH-14** | `RechnungControllerTest` | GET `/rechnungen/{id}/pdf` mit Sponsor-Org-Mitgliedschaft → 200, eigene Rechnung |
| **RECH-15** | `RechnungServiceTest` | `markiereBezahlt` schreibt `RECHNUNG_BEZAHLT` ins Audit-Log |
| **RECH-16** | `RechnungServiceTest` | `stornieren` schreibt `RECHNUNG_STORNIERT` ins Audit-Log mit Grund |
| **MAHN-01** | `MahnungsCronJobTest` | 7 Tage vor Fälligkeit → Reminder versendet, `mahnstufe=0` bleibt |
| **MAHN-02** | `MahnungsCronJobTest` | 7 Tage nach Fälligkeit → 1. Mahnung, `mahnstufe=1` gesetzt |
| **MAHN-03** | `MahnungsCronJobTest` | Mehrfacher Lauf am gleichen Tag versendet nur eine Mahnung (Idempotenz) |
| **MAHN-04** | `MahnungsCronJobTest` | BEZAHLT-Rechnung wird nie gemahnt |
| **PAY-07** | `DatatransProviderIT` *(Backlog)* | HMAC-Signatur-Verifikation: falsche Signatur → 401, korrekte → 200 |
| **PAY-08** | `DatatransProviderIT` *(Backlog)* | Webhook mit `status=SETTLED` markiert Rechnung als BEZAHLT |
| **PAY-09** | `PaymentTransactionRepositoryTest` *(Backlog)* | UNIQUE-Constraint auf `(provider, provider_reference)` verhindert Replay |

Die VTR-/RECH-/PAY-/MAHN-Blöcke werden in `TESTSTRATEGIE.md` ergänzt.

## 13. Buchhaltungs-Export (Backlog Phase 12)

CSV-Export für Vereins-Buchhaltung. Spalten:

```
rechnungsnummer; datum; sponsor_name; betrag_chf; mwst_satz; mwst_betrag; status; bezahlt_am; storno_grund
```

Erweiterung: camt.054-XML für direkten Bank-Import, Bexio-/Sage-API-Push als optionale Provider-Integration.

## 14. Offene Entscheidungen

| Frage | Optionen | Empfehlung |
|---|---|---|
| **PDF-Bibliothek** | A: OpenHTMLtoPDF + Thymeleaf. B: iText 7 (kommerziell). C: JasperReports | **A** — Apache 2.0, gute Thymeleaf-Integration, bestehende `*-pdf.html`-Templates passen |
| **QR-Code-Lib** | A: ZXing + Manuell QR-Body. B: qr-bill-tools (kommerziell). C: nayuki/qrcodegen | **A** — Apache 2.0, voller Kontroll-Pfad, kein Lizenz-Risiko |
| **Rechnungsnummer-Generator-Thread-Safety** | A: `synchronized` auf Service. B: DB-Lock pro Org-Jahr. C: Eigene Lfd-Nr-Tabelle | **A** initial, bei Last-Problemen → **C** |
| **MwSt initial implementieren?** | A: Initial weglassen. B: Felder anlegen, Default Null. C: Voll umsetzen | **A** — Pilot-Vereine sind erfahrungsgemäss nicht MwSt-pflichtig; Phase 12 wenn nötig |
| **Mahnstufe als Spalte oder eigene Tabelle?** | A: INT-Spalte auf rechnung. B: `mahnung`-Tabelle | **A** für initial 4 Stufen, **B** wenn beliebig viele Mahnungen pro Rechnung |
| **Storno-Grund Pflicht?** | A: Pflicht. B: Optional. C: Pflicht ab Stufe 2 | **A** für Auditierbarkeit |
| **Sponsor-Org sieht eigene Rechnungen?** | A: Ja, Read-Only. B: Nein (Sponsor erhält nur Mail-Anhang) | **A** — Convenience, kein Sicherheits-Risiko |
| **Vertrag-FK auf Rechnung mit CASCADE?** | A: ON DELETE CASCADE (V17 aktuell). B: ON DELETE RESTRICT | **B** umstellen — Buchhaltungs-Integrität verhindert versehentliches Löschen über Vertrag-Lösch-Pfad |

## 15. Migration nach Phase 11 / 12

Diese Spec impliziert folgende kommende Migrationen (Phase 12):

- **V26** *(oder höhere freie Nummer)* — `rechnung.mahnstufe` + `letzte_mahnung_am`, `rechnung.storno_grund`, `rechnung.storno_am`, `rechnung.storno_von`
- **V27** — `payment_transaction`-Tabelle für Datatrans-Webhook-Persistenz
- **V28** *(optional)* — `organisation.mwst_nummer` + `organisation.ist_mwst_pflichtig`, `rechnung.mwst_satz_prozent` + `rechnung.mwst_betrag_chf`
- **V29** *(optional)* — ALTER `rechnung.vertrag_id` FK von CASCADE auf RESTRICT

## 16. Referenzen

- [Six Group Swiss Implementation Guidelines QR-Bill v2.2](https://www.six-group.com/dam/download/banking-services/standardization/qr-bill/ig-qr-bill-de.pdf)
- [Six Group Style Guide QR-Bill](https://www.six-group.com/dam/download/banking-services/standardization/qr-bill/style-guide-de.pdf)
- [Datatrans API Documentation](https://docs.datatrans.ch/)
- [OpenHTMLtoPDF](https://github.com/danfickle/openhtmltopdf)
- [ZXing](https://github.com/zxing/zxing)
- [OR Art. 957 ff. — Buchhaltungs-Pflicht](https://www.fedlex.admin.ch/eli/cc/27/317_321_377/de#art_957)
