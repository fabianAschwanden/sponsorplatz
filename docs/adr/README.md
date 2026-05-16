# Architecture Decision Records (ADR)

Hier dokumentieren wir bewusste Architektur-Entscheidungen und das Warum.
Code-Specs in `specs/` beschreiben **was** gilt — ADRs beschreiben **warum**
wir uns so entschieden haben. Beide ergänzen sich:

- `specs/` ist die lebende Vertrags-Schicht (Datenmodell, Tests, Roadmap).
- `docs/adr/` ist das Entscheidungs-Gedächtnis (welche Alternative wurde verworfen, warum).

## Format

Wir verwenden eine leichte Variante des Michael-Nygard-Formats:

```markdown
# ADR-XXXX: <kurzer, prägnanter Titel>

## Status
Akzeptiert | Vorgeschlagen | Ersetzt durch ADR-YYYY | Veraltet

## Datum
YYYY-MM-DD

## Kontext
Welches Problem stand zur Lösung an? Welche Umstände prägten die Entscheidung?

## Entscheidung
Was wir entschieden haben — kurz, eindeutig, im Imperativ.

## Konsequenzen
Positive Folgen, negative Folgen, Spannungsfelder.

## Alternativen
Was wir verworfen haben und warum.

## Referenzen
Specs, Tests, externe Quellen.
```

## Neue ADRs anlegen

Nummerierung lückenlos und chronologisch. Bei einer Entscheidung-Korrektur:
neuen ADR mit höherer Nummer anlegen, alten auf Status `Ersetzt durch ADR-YYYY`
setzen — niemals einen ADR löschen oder rückwirkend ändern. Architektur-Entscheidungen
sind ein **Audit-Trail**.

## Index

| ADR | Titel | Status |
|---|---|---|
| [0001](0001-feature-folder-statt-schichten.md) | Feature-Folder statt Schichten-Paket | Akzeptiert |
| [0002](0002-view-dto-pflicht.md) | View-DTO-Pflicht — Entities verlassen den Service-Layer nicht | Akzeptiert |
| [0003](0003-kollaborative-plattform-statt-multi-tenant.md) | Kollaborative Plattform statt Multi-Tenant | Akzeptiert |
| [0004](0004-branche-enum-health-fokus.md) | Branche-Enum strikt auf Health-Fokus | Akzeptiert |
| [0005](0005-foederierte-identitaeten-eigene-tabelle.md) | Föderierte Identitäten in eigener Tabelle | Akzeptiert |
| [0006](0006-swiss-qr-bill-stack.md) | Swiss-QR-Bill mit OpenHTMLtoPDF + ZXing | Akzeptiert |
| [0007](0007-archunit-fuer-statische-verifikation.md) | ArchUnit für statische Architektur-Verifikation | Akzeptiert |
| [0008](0008-structurizr-fuer-c4-diagramme.md) | Structurizr-DSL für C4-Architektur-Diagramme | Akzeptiert |
