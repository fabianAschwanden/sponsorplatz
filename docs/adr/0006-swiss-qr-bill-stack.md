# ADR-0006: Swiss-QR-Bill mit OpenHTMLtoPDF + ZXing

## Status
Akzeptiert

## Datum
2026-05-08

## Kontext

Sponsorplatz generiert nach jedem unterzeichneten Vertrag eine Rechnung als
PDF mit Swiss QR-Bill (Six-Group-Spec v2.2). Sponsor scannt den QR-Code in
seiner Banking-App und überweist auf den Vereins-IBAN.

Anforderungen an den Tech-Stack:

- Six-Group-konformes PDF-Layout (Empfangsschein, Zahlteil, Perforations-Marker, Schweizerkreuz-Marker)
- QR-Code mit korrekt berechneter QR-Referenz (Mod-10-Recursive bei QR-IBAN)
- Lizenz-konform für kommerziellen Open-Source-Stack (Apache 2.0 / MIT bevorzugt)
- Wartbar mit aktivem OSS-Projekt
- Integration mit Thymeleaf-Templates (`rechnung-pdf.html`)

Bibliotheks-Optionen evaluiert:

| Stack | Lizenz | QR-Bill | Aktivität | Bewertung |
|---|---|---|---|---|
| **iText 7** | AGPL + kommerziell | ja | aktiv | Lizenz-Risiko für CSS-Use |
| **JasperReports** | LGPL | indirekt | aktiv | overkill für Sponsorplatz-Volumen |
| **OpenHTMLtoPDF + ZXing** | Apache 2.0 + Apache 2.0 | manuell zu bauen | beide aktiv | sauberer Pfad |
| **ch.codeblock.qrinvoice** | Apache 2.0 | ja, fertig | weniger aktiv | Black-Box, weniger Kontrolle |

## Entscheidung

Wir verwenden den Stack **OpenHTMLtoPDF + ZXing** mit eigenem
QR-Body-Generator.

**Implementierung:**

- `OpenHTMLtoPDF` rendert die Thymeleaf-Templates `rechnung-pdf.html` /
  `vertrag-pdf.html` zu PDF. Die Templates definieren das PDF-Layout
  vollständig in HTML+CSS (mit Six-Group-Style-Guide-konformen Maßen).
- `ZXing` generiert den QR-Code als PNG (Schweizerkreuz-Marker manuell
  overlayed).
- Der QR-Body wird per `QrBillBodyBuilder` aus IBAN, Betrag, Sponsor-Adresse
  und Referenz zusammengesetzt — strict nach Six-Group-Implementation-Guidelines.
- Bei QR-IBAN (Institut-ID 30000-31999) wird die 27-stellige Referenz mit
  Mod-10-Recursive-Prüfziffer generiert.

Beide Bibliotheken sind Apache-2.0-lizenziert — keine Lizenz-Tradeoffs für
kommerziellen oder internen CSS-Einsatz.

## Konsequenzen

**Positiv:**

- **Kein Lizenz-Risiko** — sauber Apache 2.0 durchgehend.
- **Volle Kontrolle** über QR-Layout und Body-Berechnung — wir sind nicht von
  einer Spezial-Lib abhängig, deren Bug-Fixes verzögert kommen könnten.
- **Thymeleaf-Templates** bleiben einheitlicher Render-Pfad für Web und PDF.
- **HTML+CSS-Layout** ist debugbar im Browser — kein Black-Box PDF-Code.
- **ZXing ist Industrie-Standard** — gut getestet, Maintained von Google-nahen Communities.

**Negativ:**

- **Mehr Eigen-Code** als bei einer fertigen QR-Bill-Lib — wir müssen den
  QR-Body und die Mod-10-Prüfziffer selbst korrekt implementieren.
  Mitigation: Test-Vektoren aus dem Six-Group-Style-Guide gegen unsere
  `QrReferenzGenerator`-Implementation.
- **HTML→PDF-Rendering** hat Eigenheiten (Page-Breaks, exakte mm-Maße). Lerneffekt einmal nötig.
- **Style-Guide-Compliance** ist Pflicht-Test — wenn Six-Group den Style-Guide ändert (v2.3, v3.0), müssen wir Templates anpassen. Bei Lib-Abhängigkeit wäre das ein Lib-Update.

## Alternativen

- **iText 7 (kommerziell)** verworfen — AGPL für OSS-Build, kommerzielle Lizenz wäre für CSS-Use vertretbar, aber Apache-2.0-Pfad ist sauberer und kostenfreier.
- **iText 5 (AGPL)** verworfen — End-of-Life, keine neuen Features, AGPL-Implikationen unklar.
- **ch.codeblock.qrinvoice** verworfen — geringere Community-Aktivität, Black-Box ohne klaren Bug-Fix-Pfad. Wir behalten Variante in der Hinterhand, falls Eigen-Stack später Schmerz macht.
- **JasperReports** verworfen — overkill, eigene Template-Sprache (JRXML), schlechte Thymeleaf-Konsistenz.
- **Eigene PDF-Library** verworfen — Wahnsinn.

## Konsequenzen für andere PDF-Anwendungsfälle

Mit `OpenHTMLtoPDF` als zentraler PDF-Pfad können wir auch andere PDFs (Vertrag,
Bestätigungs-Belege, Datenexporte) im gleichen Stack rendern — der Investment
amortisiert sich.

## Referenzen

- [`specs/SPONSORING_ZAHLUNGSFLUSS.md`](../../specs/SPONSORING_ZAHLUNGSFLUSS.md) §4 Swiss-QR-Bill Compliance
- [Six Group Implementation Guidelines QR-Bill v2.2](https://www.six-group.com/dam/download/banking-services/standardization/qr-bill/ig-qr-bill-de.pdf)
- [Six Group Style Guide QR-Bill](https://www.six-group.com/dam/download/banking-services/standardization/qr-bill/style-guide-de.pdf)
- [OpenHTMLtoPDF](https://github.com/danfickle/openhtmltopdf)
- [ZXing](https://github.com/zxing/zxing)
- Templates `src/main/resources/templates/{rechnung,vertrag}-pdf.html`
- Test-IDs QRB-01..03 (PNG-Output), RECH-12 (Referenz-Generator)
