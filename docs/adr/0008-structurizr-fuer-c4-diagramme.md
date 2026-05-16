# ADR-0008: Structurizr-DSL für C4-Architektur-Diagramme

## Status
Akzeptiert

## Datum
2026-05-08

## Kontext

Die textbasierten Specs in `specs/` und ADRs in `docs/adr/` beschreiben
Architektur präzise, aber nicht visuell. Stakeholder-Gespräche
(Kickbox-Pitch, CSS-Sponsoring-Team-Review, Pilot-Verein-Onboarding) brauchen
**Diagramme**, die System-Kontext, Container und Komponenten auf den ersten
Blick zeigen. Bisher entstanden solche Diagramme ad hoc in Folien — sie
laufen schnell auseinander vom echten Code.

Wir brauchen ein **Living Architecture Document**: eine textbasierte Quelle,
aus der bei jeder Änderung automatisch konsistente Diagramme generiert
werden. Der Editier-Flow soll genauso PR-basiert sein wie der Code selbst.

Optionen evaluiert:

| Werkzeug | Stärke | Schwäche |
|---|---|---|
| **Structurizr DSL** | C4-nativ, viele Export-Formate (Mermaid, PlantUML, …), aktive Community, OSS-CLI | Eigene DSL zu lernen, Auto-Layout nicht immer schön |
| **PlantUML mit C4-Macros** | PlantUML weit verbreitet, Mermaid-Renderer in GitHub | Manueller Layout-Code, kein Workspace-Konzept, schwer skalierbar |
| **draw.io / Excalidraw** | Visuell, leicht | Nicht textbasiert, keine Git-Diffs, keine CI-Integration |
| **Eigener Mermaid-Code per Hand** | Native GitHub-Rendering | Mehrere Diagramme laufen auseinander, kein Workspace-Konzept |
| **Hexagon Sphinx-needs** | Sehr mächtig | Komplexes Setup, Python-Ökosystem statt JVM |

## Entscheidung

Wir verwenden **Structurizr DSL** als Single Source of Truth, mit
**Structurizr CLI** zum Render in CI.

Konkret:

- `docs/architektur/workspace.dsl` definiert den Workspace: Personas,
  externe Systeme, Sponsorplatz mit seinen Containern und den Feature-Folder-
  Komponenten, plus Beziehungen.
- `.github/workflows/architektur-diagramme.yml` läuft bei jeder Änderung an
  `workspace.dsl`. Workflow:
  1. Structurizr CLI lädt den Workspace
  2. Validiert die DSL syntaktisch
  3. Exportiert nach Mermaid (in GitHub nativ darstellbar) + PlantUML (als Backup-Format)
  4. Committet die Diagramme nach `docs/architektur/diagramme/` zurück (nur auf main)
- Lokale Vorschau via **Structurizr Lite** Docker-Image (Live-Reload-Editor).
- Die Komponenten-Namen im DSL **spiegeln 1:1 die Java-Paket-Namen** —
  `organisation`, `projekt`, `anfrage`, … — der Bezug zwischen Code und
  Diagramm ist damit explizit.

Wir modellieren bewusst nur drei C4-Ebenen:

- **C1 — SystemContext:** Personas + externe Systeme + Sponsorplatz
- **C2 — Container:** Web-App + PostgreSQL + Anbindungen
- **C3 — Komponenten Web:** Feature-Folder mit ihren Beziehungen

C4-Code (UML-Klassen, Sequenzen) wird **nicht** modelliert — das wäre
Doppelung mit dem Code. Deployment-Diagramme entstehen separat in der
OCI-Cloud-Config, nicht hier.

## Konsequenzen

**Positiv:**

- **Eine Quelle für alle Diagramme** — kein Auseinanderlaufen mehr.
- **Git-Diffs auf Architektur-Änderungen** sichtbar im PR-Review.
- **GitHub rendert Mermaid nativ** — Diagramme sind ohne externe Tools sichtbar.
- **ArchUnit-Regeln und Diagramm hängen zusammen** — wenn ARCH-06 prüft, dass Feature-Folder zyklenfrei sind, dann visualisiert C3 dieselbe Wahrheit.
- **Stakeholder-Reviews kriegen aktuelle Diagramme** automatisch — keine Folien-Sünde mehr.
- **Niedriger laufender Aufwand** — ein DSL-Update pro architektonischer Änderung, dann Auto-Render.

**Negativ:**

- **DSL-Lernkurve** — Structurizr-DSL ist nicht ganz trivial, vor allem Styling und Auto-Layout-Konfiguration.
- **Auto-Layout ist nicht immer schön** — bei vielen Komponenten kann der C3-View überladen wirken. Workaround: `group`-Konstrukte für visuelle Cluster.
- **Workflow committet auf main zurück** — das erzeugt einen automatischen Commit nach jedem `workspace.dsl`-Push. Akzeptiert, weil der Commit klar als CI-Output markiert ist (`Sponsorplatz CI`-Author).
- **Lock-In auf Structurizr-DSL** — würden wir den Renderer wechseln, wäre die DSL anzupassen. Realität: Structurizr ist seit ~2018 stabil, das Risiko ist gering. Backup: die DSL ist gut lesbar, manuelle Migration nach z. B. C4-PlantUML wäre in einem Tag machbar.

## Alternativen

- **PlantUML mit C4-Macros** verworfen — kein Workspace-Konzept, Style-Konsistenz über mehrere Diagramme aufwendig.
- **Hand-gepflegtes Mermaid** verworfen — mehrere Diagramme würden auseinanderlaufen, kein Single-Source-of-Truth.
- **draw.io / Excalidraw** verworfen — keine Git-Diff-Eignung, Auto-Layout-Verlust beim Re-Editor-Öffnen.
- **Spring Modulith Documentation-Mode** verworfen für initial — würde nur C3-Komponenten generieren, kein C1/C2-Kontext. Lohnt sich später als zusätzlicher Generator-Pfad neben Structurizr.

## Bezug zu anderen Entscheidungen

- [ADR-0001](0001-feature-folder-statt-schichten.md) — die Feature-Folder werden im C3-View als Komponenten gerendert.
- [ADR-0007](0007-archunit-fuer-statische-verifikation.md) — ArchUnit prüft die Regeln, Structurizr-Diagramme visualisieren die gewollte Struktur. Beide hängen am selben Architektur-Bild.

## Referenzen

- [`docs/architektur/workspace.dsl`](../architektur/workspace.dsl) — Workspace-Definition
- [`docs/architektur/README.md`](../architektur/README.md) — lokaler Workflow
- [`.github/workflows/architektur-diagramme.yml`](../../.github/workflows/architektur-diagramme.yml) — CI-Pipeline
- [Structurizr DSL Documentation](https://docs.structurizr.com/dsl)
- [C4 Model](https://c4model.com/)
- [Structurizr CLI Releases](https://github.com/structurizr/cli/releases)
