# ADR-0013: Swiss-QR-Bill mit net.codecrete.qrbill statt ZXing-Eigenbau

## Status
Akzeptiert (ersetzt den QR-Generator-Teil von [ADR-0006](0006-swiss-qr-bill-stack.md))

## Datum
2026-06-08

## Kontext

[ADR-0006](0006-swiss-qr-bill-stack.md) entschied sich für **ZXing + eigenen
QR-Body-Generator** (Schweizerkreuz-Marker manuell overlayed, QR-Body und
Mod-10-Referenz selbst implementiert). In der Umsetzung hat sich gezeigt, dass
der Eigenbau der Six-Group-konformen QR-Bill (exakte Feldpositionen, Empfangs-
schein-Layout, Zeichensatz-Validierung, Adress-Strukturierung) deutlich mehr
Pflege kostet als erwartet — genau das in ADR-0006 als „mehr Eigen-Code" und
„Style-Guide-Compliance ist Pflicht-Test" benannte Spannungsfeld.

`net.codecrete.qrbill` (die in ADR-0006 unter „ch.codeblock.qrinvoice … in der
Hinterhand" angedachte Kategorie der fertigen Lib) ist MIT-lizenziert, aktiv
gepflegt, Six-Group-v2.x-konform und kapselt Validierung (`QRBillValidationError`),
QR-Referenz (`Payments.createQRReference`/`isQRIBAN`) und das vollständige
QR-Bill-Rendering.

## Entscheidung

Die QR-Bill wird mit **`net.codecrete.qrbill`** erzeugt (`QrBillService` →
`PNGCanvas`, Output `QR_BILL_ONLY` als PNG, eingebettet via Data-URL ins
PDF/HTML). ZXing + der geplante eigene `QrBillBodyBuilder`/`QrReferenzGenerator`
entfallen.

**Unverändert aus ADR-0006:** `OpenHTMLtoPDF` bleibt der zentrale PDF-Render-Pfad
(`PdfGeneratorService` rendert die Thymeleaf-Templates `rechnung-pdf.html` /
`vertrag-pdf.html`). Die QR-Bill ist ein PNG, das ins HTML-Template eingebettet
wird — die HTML→PDF-Pipeline bleibt also identisch.

## Konsequenzen

**Positiv:**

- **Weniger Eigen-Code** — Validierung, Referenz-Berechnung und Layout liefert
  die Lib; wir testen nur noch die Integration (QRB-01..05).
- **Six-Group-Konformität** wird von der Lib gepflegt — Style-Guide-Updates
  kommen als Lib-Update statt als Template-Nacharbeit.
- **MIT-Lizenz** — keine Lizenz-Tradeoffs (wie schon der Apache-Pfad).

**Negativ / Betriebs-Eigenheiten (BETA-V09):**

- **`PNGCanvas` nutzt Java2D/AWT-Text-Rendering** → braucht im Runtime-Container
  installierte Fonts + `fontconfig` + `-Djava.awt.headless=true`. Das fontlose
  `eclipse-temurin:jre-jammy` führte sonst zu einem `InternalError`. Mitigation:
  `fontconfig` + `fonts-liberation` + `fonts-dejavu-core` im Dockerfile;
  `QrBillSelbsttest` (`ApplicationReadyEvent`) macht das Problem beim Boot
  sichtbar; `QrBillService.erzeuge` fängt Render-/Font-Fehler (`Error`, ausser
  `VirtualMachineError`) ab und mappt auf 409 statt rohem 500.
- **Lazy-Loading-Falle:** `erzeugeAlsDataUrlFuerId` lädt die Rechnung und greift
  auf die LAZY-Relation `Rechnung.org` zu — muss `@Transactional(readOnly=true)`
  sein, weil `spring.jpa.open-in-view=false` (prod) die Session sonst vor dem
  Org-Zugriff schliesst (`LazyInitializationException` → 500). Regression
  QRB-LAZY-01.

**Black-Box-Risiko** (in ADR-0006 als Gegenargument zur fertigen Lib genannt):
akzeptiert — die Lib ist aktiv gepflegt, der Eigenbau-Aufwand überwog.

## Alternativen

- **ZXing-Eigenbau beibehalten** (ADR-0006) verworfen — Pflegeaufwand für
  Six-Group-Konformität zu hoch.
- **`ch.codeblock.qrinvoice`** — gleiche Kategorie, aber `net.codecrete.qrbill`
  ist aktiver und hat die klarere Validierungs-/Referenz-API.

## Referenzen

- [ADR-0006](0006-swiss-qr-bill-stack.md) — Vorgänger-Entscheidung (PDF-Pfad bleibt gültig)
- `src/main/java/ch/sponsorplatz/anfrage/QrBillService.java`
- `docker/Dockerfile` — fontconfig + Fonts + headless
- Test-IDs QRB-01..05, QRB-LAZY-01, QRB-SELF-01/02
- [net.codecrete.qrbill](https://github.com/manuelbl/SwissQRBill)
