# ADR-0001: Feature-Folder statt Schichten-Paket

## Status
Akzeptiert

## Datum
2026-02-15

## Kontext

Bei der initialen Skelett-Erstellung von Sponsorplatz stand die Wahl zwischen
zwei Paket-Strukturen:

- **A) Schichten-Pakete:** `controller/`, `service/`, `repository/`, `model/`, `dto/` — flach unter `ch.sponsorplatz`.
- **B) Feature-Folder:** `organisation/`, `projekt/`, `anfrage/`, `benutzer/`, … — jeweils mit allen Layer-Klassen drin, plus ein gemeinsames `shared/` für Querschnitte.

Variante A ist Spring-Boot-Konvention der frühen 2010er und kommt häufig in
Tutorials vor. Variante B ist Domain-Driven-Design-näher, hat sich aber bei
Codebases ab 20+ Features als wartbarer erwiesen.

Erwartetes Plattform-Wachstum: 10–15 Features (Organisation, Projekt,
Anfrage, Vertrag, Rechnung, Watchlist, Einladung, Audit, …). Bei dieser
Grösse ist die Suche nach allen Datei-en eines Features in einem flachen
Schichten-Paket deutlich aufwendiger.

## Entscheidung

Wir verwenden **Feature-Folder** (Variante B).

Struktur:

```
ch.sponsorplatz/
├── organisation/        # Organisation + Mitgliedschaft + AccessControl
├── projekt/             # Projekt + SponsoringPaket + Marktplatz
├── anfrage/             # SponsoringAnfrage + Vertrag + Rechnung + Payment
├── benutzer/            # AppUser + Auth + Profile
├── einladung/           # Einladung-Lifecycle
├── audit/               # AuditLog + DatenExport
├── benachrichtigung/    # In-App + E-Mail
├── admin/               # Admin-Workflows
├── home/                # Public-Pages + Statistik
├── aufgabe/             # Aufgaben/Tasks
├── backup/              # Backup-Service
├── ops/                 # Ops-Dashboards
└── shared/              # Querschnitt
    ├── config/          # SecurityConfig, ModelAttributeNames
    ├── exception/       # NotFoundException, GlobalExceptionHandler
    ├── mail/            # MailService-Abstraktion
    ├── pdf/             # OpenHTMLtoPDF-Wrapper
    ├── storage/         # MedienAsset-Storage
    ├── einstellungen/   # Plattform-weite Einstellungen
    └── util/            # SlugGenerator, TokenGenerator
```

Innerhalb eines Feature-Folders alle Layer (`*Controller`, `*Service`,
`*Repository`, `*View`, `*FormDto`, Entity, Enums).

## Konsequenzen

**Positiv:**

- Feature-bezogenes Refactoring lokal — alles für „Organisation" liegt in `organisation/`.
- Mental Map für neue Engineers einfacher: ein Feature, ein Folder.
- Naturalisiert Domain-Driven-Design-Sprache: das Paket ist die Bounded-Context-Grenze.
- Tests spiegeln die Struktur exakt — `src/test/java/ch/sponsorplatz/<feature>/<Klasse>Test.java`.
- Vorbereitung für Spring Modulith (siehe ADR-zukünftig): jedes Feature-Folder kann formal zum Application Module werden.

**Negativ:**

- Layer-Disziplin pro Folder muss aktiv durchgesetzt werden — nicht implizit durch Paket-Struktur. → Lösung: ArchUnit (siehe ADR-0007).
- Wer in Schichten denkt, muss umstellen — Onboarding-Kosten initial.
- Cross-Cutting wie „alle Controller anschauen" verteilt sich über Folder. → Lösung: IDE-Suche nach Annotation `@Controller`, plus die zentrale Liste in ARCH-12.

## Alternativen

- **Schichten-Pakete (A)** verworfen — bei erwarteten 10+ Features schlechte Skalierbarkeit, redundante Suchpfade.
- **Hexagonal/Onion mit `application/`, `domain/`, `infrastructure/`** verworfen — overkill für eine Spring-Boot-Monolith-App in Pilot-Phase. Domain-Klassen sind hier JPA-Entities (mit Hibernate-Annotations), eine strikte Domain-Trennung würde nur Mapping-Code erzeugen ohne klaren Nutzen.

## Referenzen

- [`CLAUDE.md`](../../CLAUDE.md) — Verzeichnisstruktur
- [`specs/TECHNISCHE_SPEZIFIKATION.md`](../../specs/TECHNISCHE_SPEZIFIKATION.md) — Paket-Mapping pro Feature
- ArchUnit-Regel ARCH-06 (Slices, keine Zyklen zwischen Feature-Foldern)
- ArchUnit-Regel ARCH-07 (`shared/` kennt keine Features)
