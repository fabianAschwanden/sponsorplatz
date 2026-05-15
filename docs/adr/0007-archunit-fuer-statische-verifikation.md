# ADR-0007: ArchUnit für statische Architektur-Verifikation

## Status
Akzeptiert

## Datum
2026-05-08

## Kontext

Sponsorplatz hat in der Frühphase eine Reihe expliziter Architektur-Regeln
formuliert — View-DTO-Pflicht (ADR-0002), Feature-Folder (ADR-0001), Layer-
Disziplin Controller→Service→Repository, Custom-Exception-Mapping,
AccessControl-Pflicht für Admin-Routen. Diese Regeln stehen heute in:

- `CLAUDE.md` (als Konvention)
- `.instructions.md` (als TDD-Pflicht)
- `specs/TESTSTRATEGIE.md` (als Test-Erwartung)

Aber: **Dokumentation alleine durchsetzt keine Regel.** Jeder neue Feature-PR
kann unbeabsichtigt einen `model.addAttribute(entity)` einführen, einen
`@Service` ausserhalb der Feature-Folder anlegen, ein `*Repository` als Klasse
schreiben statt als Interface. Das ist klassischer Architektur-Zerfall, der
Quartal für Quartal langsam stattfindet, bis die Codebasis nicht mehr dem
ursprünglichen Bild entspricht.

Wir brauchen einen **automatisierten Wächter**, der jede Verletzung sofort
sichtbar macht — im rotem Build, nicht erst im Code-Review.

Optionen evaluiert:

| Werkzeug | Stärke | Schwäche |
|---|---|---|
| **ArchUnit** | JUnit-native, ausdrucksstarke DSL, gut maintained | Java-only |
| **Spring Modulith** | Module-Boundaries explizit, Boot-Time-Verify | Lernkurve, jünger |
| **Sonarqube Custom Rules** | UI-getrieben, Quality-Gate-Integration | Server-Setup nötig, schwächere Architektur-DSL |
| **Custom Maven-Enforcer-Rules** | Sehr explizit | Boilerplate, kein Pattern-Match |

## Entscheidung

Wir nutzen **ArchUnit** als Schicht-1-Verifikation.

Konkret:

- Dependency `com.tngtech.archunit:archunit-junit5:1.3.0` in pom.xml (test-scope)
- Zentrale Klasse `ch.sponsorplatz.architektur.ArchitekturRegelnTest` mit
  derzeit **13 Regeln** (`ARCH-01..13`), gespiegelt in `TESTSTRATEGIE.md`
- Architektur-Regeln laufen automatisch in `mvn test`, also in CI bei jedem PR
- Spec-Update-Pflicht: neue Regel → erst Test-ID in TESTSTRATEGIE pflegen,
  dann Regel im Test ergänzen
- Eine Regel wird nicht gelöscht, nur kommentiert mit Begründung und Link auf
  einen neuen ADR (klassischer Audit-Trail-Pfad)

Spring Modulith wird **später** als Schicht 2 aufgesetzt, sobald die
Plattform-Grösse oder ein zweiter Engineer den Mehr-Aufwand rechtfertigt.

## Konsequenzen

**Positiv:**

- Layer-Verletzungen werden im Build sofort rot — kein Code-Review-Loch mehr.
- Neue Mitarbeitende lernen die Regeln durch Test-Fehler — die Erwartung wird beim Erstkontakt explizit.
- Refactorings können nicht versehentlich Modul-Boundaries brechen.
- Niedriger Lern-Aufwand — ArchUnit-DSL ist lesbar wie eine Spec.
- Geringer Code-Aufwand — ~250 Zeilen Test-Code für 13 Regeln.

**Negativ:**

- Verlangt Disziplin, neue Regeln zu pflegen — wenn eine Regel nicht in
  ArchUnit landet, bleibt sie nur Dokumentation. Mitigation: PR-Template
  enthält Pflicht-Punkt "ArchUnit-Regel ergänzt, falls neue Disziplin".
- Falsch-Positive sind möglich, wenn die Regel zu strikt formuliert ist.
  Mitigation: Bewusste Ausnahmen via `.as("…")`-Begründung dokumentieren.
- Bytecode-Scan kostet Test-Zeit — heute marginal bei ~150 Test-Klassen, bei
  1000+ messbar. Akzeptabel.

## Alternativen

- **Reine Dokumentation in CLAUDE.md belassen** verworfen — siehe Kontext, Architektur-Zerfall ist real.
- **Spring Modulith direkt** verworfen für *initial* — höherer Refactoring-Aufwand für die Feature-Folder-Struktur (`internal/`-Subpakete pro Feature). Wird als Schicht-2-Folge-Investition geführt.
- **Sonarqube** verworfen — Server-Setup, schwächere Architektur-DSL als ArchUnit, lohnt sich nicht für Solo-/Klein-Team-Setup.
- **Maven-Enforcer-Plugin** verworfen — keine Pattern-Matching-DSL.

## Konsequenzen für die Engineering-Praxis

ArchUnit verändert das Review-Verhalten:

- Code-Reviews fokussieren auf Geschäftslogik und Naming, nicht mehr auf "ist das im richtigen Layer?".
- Architektur-Diskussionen finden im ADR statt — wenn eine Regel umstritten ist, wird ein neuer ADR mit Status-Wechsel angelegt, nicht stillschweigend ein Test gelöscht.
- Neue Engineers werden durch rote Tests erzogen — Onboarding-Kosten sinken.

## Referenzen

- [`specs/TESTSTRATEGIE.md`](../../specs/TESTSTRATEGIE.md) §Architektur-Verifikation (ARCH-01..13)
- `src/test/java/ch/sponsorplatz/architektur/ArchitekturRegelnTest.java`
- [ArchUnit User Guide](https://www.archunit.org/userguide/html/000_Index.html)
- [Spring Modulith](https://docs.spring.io/spring-modulith/reference/) (Schicht 2, später)
- `pom.xml` — Dependency `archunit-junit5:1.3.0`
