# Architektur-Diagramme (C4 Model via Structurizr)

Lebende Architektur-Sicht auf Sponsorplatz. Single source of truth ist
[`workspace.dsl`](workspace.dsl) — die generierten Diagramme unter
[`diagramme/`](diagramme/) werden von der CI bei jeder Änderung neu erzeugt.

Bei jeder strukturellen Architektur-Änderung (neuer Feature-Folder, neue
Container-Abhängigkeit, neue externe Integration) wird `workspace.dsl`
aktualisiert. Das ist Pflicht-Bestandteil der Definition-of-Done — analog
zu Spec-Update und ArchUnit-Regel.

## C4-Views

| View | Beschreibung |
|---|---|
| **C1 — SystemContext** | Wer interagiert mit Sponsorplatz, welche externen Systeme sind angebunden |
| **C2 — Container** | Web-Anwendung + PostgreSQL + Datenflüsse zu Entra/Datatrans/SMTP/OCI |
| **C3 — Komponenten (Web)** | Feature-Folder als Komponenten — direkte Spiegelung der Paket-Struktur |

Tiefere Sichten (C4-Code-Ebene, Deployment-Diagramme) werden bewusst nicht
modelliert — sie wären Doppelung des Codes selbst und der OCI-Cloud-Configs.

## Workflow

### Diagramm-Änderung in einer PR

1. `workspace.dsl` editieren
2. Lokale Vorschau via Structurizr Lite (siehe unten) — optional, aber empfohlen
3. Commit + Push → CI rendert die Diagramme neu, validiert die DSL und committet
   die `.mmd`-Files nach dem Merge automatisch zurück nach `diagramme/`
4. In der PR-Description nutzen wir die Mermaid-Diagramme als visuelle Diff-Hilfe

### Lokale Vorschau mit Structurizr Lite (Docker)

```bash
# Vom Repo-Root aus starten
docker run -it --rm \
  -p 8080:8080 \
  -v "$(pwd)/docs/architektur:/usr/local/structurizr" \
  structurizr/lite
```

Dann im Browser: <http://localhost:8080>

Structurizr Lite erkennt automatisch `workspace.dsl` im gemounteten Verzeichnis,
rendert alle Views interaktiv und unterstützt Live-Reload bei Speicher-Änderungen
am DSL-File. Ideal für iterative Anpassungen.

### Lokales Rendern ohne Docker (Structurizr CLI)

```bash
# Einmalig: Structurizr CLI installieren (Java 21 nötig)
curl -L -o /tmp/structurizr-cli.zip \
  https://github.com/structurizr/cli/releases/download/v2024.12.07/structurizr-cli.zip
mkdir -p ~/.structurizr/cli
unzip -q /tmp/structurizr-cli.zip -d ~/.structurizr/cli

# Diagramme exportieren
~/.structurizr/cli/structurizr.sh export \
  -workspace docs/architektur/workspace.dsl \
  -format mermaid \
  -output docs/architektur/diagramme/
```

## Konventionen für `workspace.dsl`

- **Sprache:** Deutsch im Code, englische Element-Typen aus dem Structurizr-DSL.
- **Naming:** Komponenten-Namen entsprechen exakt den Java-Paket-Namen
  (`organisation`, `projekt`, `anfrage`, …) — damit ist der Bezug zum Code 1:1.
- **`shared` als Querschnitt:** Eigener Tag `Querschnitt`, anderes Styling, keine
  ausgehenden Beziehungen modelliert (es wird nur *zu* shared gezeigt).
- **Externe Systeme** kriegen Tag `External`. Backlog-Komponenten (Entra, Datatrans)
  zusätzlich `Backlog` für dashed-Stil.
- **Beziehungs-Labels** beschreiben den fachlichen Inhalt, nicht das Protokoll
  (Protokoll als Technologie-Annotation).

## Bezug zu anderen Specs

- [`docs/adr/0001-feature-folder-statt-schichten.md`](../adr/0001-feature-folder-statt-schichten.md) — warum Feature-Folder
- [`docs/adr/0008-structurizr-fuer-c4-diagramme.md`](../adr/0008-structurizr-fuer-c4-diagramme.md) — Tool-Wahl
- [`specs/TESTSTRATEGIE.md`](../../specs/TESTSTRATEGIE.md) §ARCH-06 — Feature-Folder-Zyklus-Freiheit
  (statisch geprüft, das Diagramm visualisiert es)
- [`CLAUDE.md`](../../CLAUDE.md) — Verzeichnisstruktur, die hier visuell abgebildet wird

## Wenn ein Diagramm "falsch" aussieht

Die Auto-Layout-Engine von Structurizr ist deterministisch, aber nicht immer
schön. Wenn der C3-View überladen wirkt:

- Komponenten gruppieren (`group "Auth" { ... }`)
- Auto-Layout-Richtung anpassen (`autoLayout tb` für top-bottom)
- Komponenten-Beziehungen prüfen — wenn alles mit allem verbunden ist, ist das ein
  echtes Architektur-Signal (Modul-Boundaries zu weich)
