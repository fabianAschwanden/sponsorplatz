# Mitarbeit

## Schnell-Einstieg

1. **Repo öffnen:** `code ~/git/sponsorplatz`
2. **Extensions installieren** — VS Code schlägt sie automatisch vor (`Install All`)
3. **Java SDK 21** sicherstellen — `Cmd+Shift+P` → *„Java: Configure Java Runtime"*
4. **App starten:** `F5` → *„Sponsorplatz (dev)"*

## TDD-Pflicht

Jede Code-Änderung folgt:

```
1. SPEC   → in specs/ beschreiben
2. TEST   → erst schreiben, muss rot sein
3. IMPL   → minimaler Code für grün
```

Details: [`.instructions.md`](../.instructions.md).

## Commit-Konvention

```
feat(bereich): kurze beschreibung
fix(bereich): ...
docs(bereich): ...
test(bereich): ...
refactor(bereich): ...
chore(bereich): ...
```

Bereiche: `setup`, `organisation`, `iam`, `crm`, `marktplatz`, `anfragen`, `medien`, `security`, `ui`, `ci`, `infra`, `docs`.

## Branches

- `main` — immer deploybar
- `feature/<thema>` — kurzlebig (max 5 Tage)
- `fix/<thema>`, `chore/<thema>`

## Pull-Request-Checkliste

- [ ] Spec aktualisiert
- [ ] Tests grün (`mvn test`)
- [ ] Build grün (`mvn verify`)
- [ ] Nichts in `target/` oder `data/` committed
- [ ] Commit-Konvention eingehalten

## Mit Claude / KI-Assistenten

Diese Repo enthält [`CLAUDE.md`](../CLAUDE.md) als zentralen Kontext-Anker für KI-Tools.
Vor jeder größeren Aktion: Spec/Test/Impl-Reihenfolge einhalten und Stand in `specs/` prüfen.
