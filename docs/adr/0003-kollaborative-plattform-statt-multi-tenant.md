# ADR-0003: Kollaborative Plattform statt Multi-Tenant

## Status
Akzeptiert

## Datum
2026-03-08 

## Kontext

Bei der Konzeption von Sponsorplatz stand die fundamentale Tenant-Frage:

- **A) Multi-Tenant-SaaS:** Jeder Verein hat seine eigene isolierte Datenbasis.
  Sponsorplatz wird wie Fairgate ein Werkzeug, das man pro Verein abonniert.
- **B) Kollaborative Plattform:** Mehrere Vereine teilen eine offene Datenbasis.
  Lese-Zugriff ist offen, Edit-Rechte werden via `Mitgliedschaft` beschränkt.

Der Markt war bereits voll von Tools-only-SaaS (Fairgate, ClubDesk). Die echte
Lücke: kein Anbieter kombiniert CRM mit einem öffentlichen, Marken-zugänglichen
Marktplatz. Das geht nur, wenn die Datenbasis **gemeinsam** ist.

Zusätzliche Treiber:

- Sponsoren wollen branchen- und regionsübergreifend nach Vereinen suchen — funktioniert nicht in isolierten Mandanten.
- Doppelt erfasste Sponsor-Stammdaten (z. B. „Migros" in fünf verschiedenen Verein-Excels) sind ein Datenqualitäts-Albtraum, der durch geteilte Stammdaten gelöst wird.
- DSG ist primär ein Schutz-Argument für **Personen**-Daten, nicht für Verein-Daten — die meisten Verein-Infos sind sowieso öffentlich (Vereinsregister, Webseite).

## Entscheidung

Wir bauen eine **kollaborative Plattform**.

Modell:

- **Eine geteilte Datenbasis** für alle Vereine.
- **Lese-Sichtbarkeit ist offen** für alle eingeloggten User (und teilweise für anonyme Besucher — Marktplatz, Vereins-Profile).
- **Edit-Rechte** werden über `Mitgliedschaft(user, org, rolle)` beschränkt. Ein
  Vereinsmitglied kann nur die Daten seiner eigenen Vereins-Org ändern.
- **Sponsor-Stammdaten geteilt** (Wikipedia-Modell): „Migros" existiert einmal, alle Vereine sehen denselben Datensatz, jeder Verein hängt seine eigenen Notizen und Beteiligungen dran.
- **Sensible Daten** (Notizen, Verträge, Beteiligungen) sind pro Vereins-Org private — nicht geteilt.

Es gibt **keine Datentrennung** auf DB-Ebene. Kein `tenant_id`-Filter in
Queries. Stattdessen: `AccessControl`-Bean prüft pro Schreib-Aktion die
Mitgliedschaft.

## Konsequenzen

**Positiv:**

- Marktplatz funktioniert natürlich — alle Projekte sind in einer Datenbasis durchsuchbar.
- Sponsor-Stammdaten-Konsolidierung — eine einzige „Migros"-Org statt 200 Excel-Einträge.
- Spar-Modus: keine Schema-Replikation pro Mandant, keine `tenant_id`-Spalten überall.
- Kein Tenant-Routing-Layer, keine DataSource-Multiplexer.
- Spring Security mit Standardpattern (`@PreAuthorize` + AccessControl-Bean) reicht aus.

**Negativ:**

- **Strenge `AccessControl`-Disziplin** nötig — jede schreibende Route MUSS prüfen, ob der User Mitglied der betroffenen Org ist. Wird durch ArchUnit (ARCH-09 für Admin) und Test-Pflicht (AC-NN-IDs) abgesichert.
- Pilot-Vereine mit Vertraulichkeits-Wünschen müssen verstehen, dass Vereins-Profile öffentlich sind. Wird durch klare Kommunikation auf der Anmelde-Page adressiert.
- Skalierung: bei sehr grosser Datenbasis muss Index-Strategie sauber sein. Heute völlig OK, in 3 Jahren ggf. Re-Evaluation.
- Falls später doch ein Mandant geschlossen werden soll, ist das ein grosser Refactoring-Schritt — wird heute akzeptiert.

## Alternativen

- **A) Multi-Tenant** verworfen — siehe Kontext. Hauptgrund: keine Marktplatz-Logik möglich, kein USP gegenüber Fairgate.
- **Hybrid (kollaborativ für Marktplatz, Multi-Tenant für CRM)** verworfen — verdoppelt die DB-Komplexität, ohne klaren Nutzen. Pilot-Vereine vertrauen der Plattform mit ihrer Datenqualität nur, wenn die Architektur transparent kollaborativ ist.

## Referenzen

- [`specs/PROJEKT_INFO.md`](../../specs/PROJEKT_INFO.md) §Modell
- [`specs/ROLLENKONZEPT.md`](../../specs/ROLLENKONZEPT.md) — vollständige Permission-Matrix
- `00_Konzept_v3_Kollaborative-Plattform.md` im Workspace
- Test-IDs AC-01..08 (`AccessControlTest`) — durchsetzungsseitige Verifikation
- Vergleichs-Tabelle Fairgate / fundoo / Sponsoo in [`specs/PROJEKT_INFO.md`](../../specs/PROJEKT_INFO.md)
