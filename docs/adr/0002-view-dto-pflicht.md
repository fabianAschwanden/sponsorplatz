# ADR-0002: View-DTO-Pflicht — Entities verlassen den Service-Layer nicht

## Status
Akzeptiert

## Datum
2026-02-22 

## Kontext

In der frühen Skelett-Phase wurden JPA-Entities direkt an Thymeleaf-Templates
gegeben (`model.addAttribute("org", organisation)`). Konsequenzen:

- **Lazy-Loading-Exceptions** in der View — `org.mitglieder.size()` fängt das Template ab, weil die Session bereits geschlossen ist.
- **Sicherheits-Leak:** Felder wie `passwortHash` und `verifikationsToken` waren technisch im Template zugreifbar.
- **Mass-Assignment-Lücke:** PUT-Endpoints akzeptierten ganze Entities und übernahmen User-kontrollierte Werte für `id` und `status`.
- **Refactoring-Hürde:** Jede Entity-Änderung brach Templates an unbekannten Stellen.

Diese Probleme tauchten beim ersten echten Feature („Mitgliederliste") sofort
auf. Eine sofortige Layer-Disziplin war nötig.

## Entscheidung

**Entities verlassen den Service-Layer niemals.** Im Controller darf
`model.addAttribute(...)` ausschliesslich:

- Java-`record`s aus `<feature>.<Name>View` bekommen (für Lese-Pfade)
- `<feature>.<Name>FormDto`-Klassen für Schreibe-Forms

Konkret:

```java
// FALSCH — Entity ins Model:
model.addAttribute("anfragen", anfrageService.findeEingehende(orgId));

// RICHTIG — View vor model.addAttribute:
List<SponsoringAnfrage> anfragen = anfrageService.findeEingehende(orgId);
model.addAttribute("anfragen", AnfrageView.von(anfragen));
```

Pro Entity gibt es einen `<Name>View`-Record mit:

1. Java-`record` in `src/main/java/ch/sponsorplatz/<feature>/<Name>View.java`
2. Statische Factory `von(entity)` und `von(List<entity>)`
3. **Mapping-Test** `<Entity>ViewTest` mit Test-ID `VIEW-NN` in `specs/TESTSTRATEGIE.md`
4. Nur Felder, die tatsächlich gerendert werden (Defense in depth — kein
   `passwortHash`, kein `verifikationsToken`)

Templates greifen ausschliesslich auf View-Properties zu, niemals auf
JPA-Relationen wie `${m.user.email}`. Bei nested Daten flachet der View ein
(`${m.userEmail}`) oder hält ein nested-Record (`${e.projekt.name}`).

Update-Pfad identifiziert die Org via Slug aus URL, niemals via Body-`id`
(K3-Fix gegen Mass-Assignment).

## Konsequenzen

**Positiv:**

- Keine Lazy-Exceptions in Templates — Views sind transactional-konsistent.
- Sicherheits-sensitive Felder können niemals leaken.
- Mass-Assignment-Defense durch DTOs ohne `id`-Feld.
- Schema-Änderungen brechen Tests, nicht Templates — frühes Feedback.
- View-Mapping-Tests sind günstige Spec-Anker (`VIEW-NN`-IDs).

**Negativ:**

- Boilerplate: für jede Entity ein Record + Test.
- Cognitive Load: drei Klassen pro Feature (Entity, View, FormDto) statt einer.
- Mapping-Logik in der Factory `von(entity)` kann komplex werden bei nested Strukturen.

## Alternativen

- **MapStruct generieren** verworfen für initial — adds compile-time complexity, lohnt sich erst ab ~30 Mappings. Records mit Hand-Factory sind klein genug.
- **Projection-Queries auf Repository-Ebene (`@Query` mit Constructor-Expression)** verworfen — verteilt Mapping-Logik auf Repository statt View, schwer testbar.
- **Direkt JPA-Entities** verworfen — siehe Kontext.

## Referenzen

- [`CLAUDE.md`](../../CLAUDE.md) §View-DTO-Pflicht
- ArchUnit-Regel ARCH-02 — Controller dürfen keine `@Entity`-Klassen referenzieren
- ArchUnit-Regel ARCH-03 — `*View`-Klassen sind Records
- VIEW-NN-Test-IDs in [`specs/TESTSTRATEGIE.md`](../../specs/TESTSTRATEGIE.md)
- K3-Fix: Mass-Assignment-Defense
