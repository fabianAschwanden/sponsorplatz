<!--
Sponsorplatz — Pull-Request-Template

Bitte alle relevanten Punkte beantworten oder als nicht zutreffend markieren.
Architektur-Disziplin wird sowohl durch ArchUnit (ARCH-01..13) als auch durch
dieses Review-Template durchgesetzt.
-->

## Zusammenfassung

<!-- Eine bis drei Sätze: Was tut diese PR und warum? -->

## Bezug

- **Test-IDs:** <!-- z. B. ORG-22, MKT-08, RECH-07 -->
- **Spec-Update:** <!-- z. B. specs/TESTSTRATEGIE.md, ROADMAP-Eintrag -->
- **ADR neu/aktualisiert:** <!-- z. B. ADR-0007 oder „keine ADR-Auswirkung" -->
- **Issue / Backlog-Item:** <!-- falls zutreffend -->

---

## Architektur-Check (verbindlich)

### TDD-Disziplin

- [ ] Spec in `specs/` aktualisiert **vor** dem Test
- [ ] Test geschrieben, war **initial rot**
- [ ] Implementation macht den Test **grün**, ohne andere Tests rot zu machen
- [ ] Test-ID nach `<Bereich>-<Nummer>` in TESTSTRATEGIE.md ergänzt

### View-DTO-Pflicht

- [ ] Keine JPA-`@Entity`-Klasse landet via `model.addAttribute(...)` im Template
- [ ] Bei neuer Entity: zugehöriger `*View`-Record im selben Feature-Folder, inkl. `<Name>ViewTest`
- [ ] Templates nutzen ausschliesslich View-Properties, keine `${entity.lazyRelation}`

### Layer-Disziplin

- [ ] Controller ruft Services auf, nicht Repositories direkt
- [ ] `@Service`-Klassen leben im Feature-Folder oder `shared/`
- [ ] `shared/` importiert nichts aus Feature-Foldern (Querschnitts-Disziplin)
- [ ] `mvn test -Dtest=ArchitekturRegelnTest` lokal grün

### Security & AccessControl

- [ ] Jede schreibende Route prüft `AccessControl.kannOrg*` (oder `@PreAuthorize`)
- [ ] Admin-Routen tragen `@PreAuthorize("hasRole('PLATFORM_ADMIN')")`
- [ ] Keine Mass-Assignment-Lücke: Update-Pfade identifizieren via URL-Slug, nicht via Body-`id`

### Datenmodell & Migrationen

- [ ] Migration ist **additiv** — keine `DROP COLUMN` auf produktiven Tabellen
- [ ] SQL ist kompatibel zu H2 und PostgreSQL (`MODE=PostgreSQL` getestet)
- [ ] Bei Schema-Change: `DATENMODELL.md` aktualisiert
- [ ] `ddl-auto=validate` bleibt grün (keine Drift Entity vs. Schema)

### Zahlungs-Compliance *(falls Vertrag, Rechnung, PaymentProvider betroffen)*

- [ ] Lifecycle gemäss `SPONSORING_ZAHLUNGSFLUSS.md` §3 eingehalten
- [ ] Audit-Log-Verdrahtung bei Status-Übergängen (siehe §10)
- [ ] DSG-Permission-Matrix (§9) durchgesetzt — Tests RECH-13/14
- [ ] Buchhaltungs-Integrität: keine `DELETE` auf bezahlten Rechnungen

### Dokumentation

- [ ] `CLAUDE.md` / `specs/PROJEKT_INFO.md` aktualisiert, falls Vision/Architektur betroffen
- [ ] Neue Architektur-Entscheidung → eigener ADR unter `docs/adr/`
- [ ] Code-Kommentare in deutscher Domain-Sprache (`speichere`, `findeNachSlug`)

---

## Smoke-Test (manuell)

- [ ] `mvn -B clean verify` lokal grün
- [ ] App startet via `mvn spring-boot:run`
- [ ] Affected Routes manuell durchgeklickt
- [ ] *(falls UI)* In Chrome DevTools Mobile-Ansicht geprüft

---

## Sicherheitsrelevant?

<!--
Wenn ja: kurz beschreiben, was geprüft wurde.
Falls externe Abhängigkeit hinzugefügt: Lizenz + Vulnerability-Stand prüfen.
-->

## Screenshots / Output

<!-- Bei UI-Änderungen Screenshot Before/After, bei API-Änderungen curl-Beispiel. -->
